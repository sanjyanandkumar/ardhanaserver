package com.ardhana.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.PendingTeacherPaymentResponse;
import com.ardhana.dto.TeacherEarningsResponse;
import com.ardhana.dto.TeacherPayoutResponse;
import com.ardhana.dto.UpdateBankDetailsRequest;
import com.ardhana.dto.UpdatePreferredPayoutMethodRequest;
import com.ardhana.dto.UpdateTeacherRateRequest;
import com.ardhana.dto.UpdateUpiDetailsRequest;
import com.ardhana.model.AppRole;
import com.ardhana.model.ClassSchedule;
import com.ardhana.model.ClassSessionStatus;
import com.ardhana.model.PayoutMethod;
import com.ardhana.model.TeacherPayout;
import com.ardhana.model.TeacherPayoutProfile;
import com.ardhana.model.TeacherPayoutStatus;
import com.ardhana.model.UserProfile;
import com.ardhana.model.UserRole;
import com.ardhana.repository.ClassScheduleRepository;
import com.ardhana.repository.TeacherPayoutProfileRepository;
import com.ardhana.repository.TeacherPayoutRepository;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Core business logic for teacher payments: computing what's owed, executing
 * a real RazorpayX payout for it, teacher earnings/history, and the rate +
 * bank-detail records that drive it all.
 *
 * Least-privilege design: bank account details are only ever written by the
 * teacher who owns them (enforced at the controller, via principal == teacherId
 * for the /my-bank-details endpoint) — the admin-facing methods here never
 * accept or return raw account numbers/IFSC, only a hasBankDetails flag.
 */
@Slf4j
@Service
public class TeacherPayoutService {

    private final ClassScheduleRepository classScheduleRepository;
    private final TeacherPayoutRepository teacherPayoutRepository;
    private final TeacherPayoutProfileRepository teacherPayoutProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RazorpayPayoutService razorpayPayoutService;

    /**
     * TESTING ONLY. When true, pay() never calls RazorpayX — the payout is
     * created and immediately marked PROCESSED with a fake reference. Mirrors
     * OrderService's app.payments.mock-enabled. Never set in production.
     */
    @Value("${app.payouts.mock-enabled:false}")
    private boolean mockPayoutsEnabled;

    public TeacherPayoutService(ClassScheduleRepository classScheduleRepository,
                                 TeacherPayoutRepository teacherPayoutRepository,
                                 TeacherPayoutProfileRepository teacherPayoutProfileRepository,
                                 UserProfileRepository userProfileRepository,
                                 UserRoleRepository userRoleRepository,
                                 RazorpayPayoutService razorpayPayoutService) {
        this.classScheduleRepository = classScheduleRepository;
        this.teacherPayoutRepository = teacherPayoutRepository;
        this.teacherPayoutProfileRepository = teacherPayoutProfileRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.razorpayPayoutService = razorpayPayoutService;
    }

    /**
     * Admin overview: every teacher, with their agreed rate, current pending
     * (unpaid completed sessions) amount, and whether they've added bank
     * details yet. This is the single table the admin's Teacher Payments
     * page is built from — it doubles as where rates get set for teachers
     * who don't have any pending dues yet.
     */
    @Transactional(readOnly = true)
    public List<PendingTeacherPaymentResponse> getAdminOverview() {
        List<UserRole> teacherRoles = userRoleRepository.findAllByRole(AppRole.teacher);

        return teacherRoles.stream()
            .map(ur -> ur.getUserId().toString())
            .map(this::toPendingPaymentResponse)
            .filter(r -> r != null)
            .sorted(Comparator.comparing(PendingTeacherPaymentResponse::getTeacherName,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    private PendingTeacherPaymentResponse toPendingPaymentResponse(String teacherId) {
        UserProfile teacherProfile = userProfileRepository.findById(teacherId).orElse(null);
        if (teacherProfile == null) return null;

        List<ClassSchedule> pending = classScheduleRepository
            .findByTeacherIdAndStatusAndTeacherPayoutIdIsNull(teacherId, ClassSessionStatus.COMPLETED);
        int pendingCount = pending.size();

        TeacherPayoutProfile payoutProfile = teacherPayoutProfileRepository.findById(teacherId).orElse(null);
        BigDecimal rate = payoutProfile != null ? payoutProfile.getRatePerSession() : null;
        BigDecimal pendingAmount = rate != null ? rate.multiply(BigDecimal.valueOf(pendingCount)) : null;
        boolean hasBankDetails = payoutProfile != null && payoutProfile.hasBankDetails();
        boolean hasUpiDetails = payoutProfile != null && payoutProfile.hasUpiDetails();
        PayoutMethod resolvedMethod = payoutProfile != null ? payoutProfile.resolvePayoutMethod() : null;

        return new PendingTeacherPaymentResponse(
            teacherId, teacherProfile.getFullName(), rate, pendingCount, pendingAmount,
            hasBankDetails, hasUpiDetails, resolvedMethod != null ? resolvedMethod.name() : null);
    }

    /**
     * Pays a teacher the FULL amount currently owed to them (rate × every
     * completed-and-unpaid session), via a single RazorpayX payout.
     *
     * Throws ResponseStatusException(BAD_REQUEST) for precondition failures
     * (nothing pending, no rate set, no bank details) — these are caught by
     * the controller and returned as a normal error toast, not a 500.
     * A RazorpayX-side failure (declined, network error, etc.) does NOT
     * throw — it's recorded as a FAILED TeacherPayout and returned normally,
     * so the admin sees exactly why in the UI.
     */
    @Transactional
    public TeacherPayoutResponse pay(String teacherId) {
        List<ClassSchedule> pendingSessions = classScheduleRepository
            .findByTeacherIdAndStatusAndTeacherPayoutIdIsNull(teacherId, ClassSessionStatus.COMPLETED);
        if (pendingSessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This teacher has no pending completed sessions to pay.");
        }

        TeacherPayoutProfile payoutProfile = teacherPayoutProfileRepository.findById(teacherId).orElse(null);
        if (payoutProfile == null || payoutProfile.getRatePerSession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No agreed rate has been set for this teacher yet.");
        }
        PayoutMethod payoutMethod = payoutProfile.resolvePayoutMethod();
        if (payoutMethod == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This teacher hasn't added bank account or UPI details yet.");
        }

        UserProfile teacherProfile = userProfileRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        BigDecimal amount = payoutProfile.getRatePerSession().multiply(BigDecimal.valueOf(pendingSessions.size()));

        TeacherPayout payout = new TeacherPayout();
        payout.setTeacherId(teacherId);
        payout.setAmount(amount);
        payout.setSessionCount(pendingSessions.size());
        payout.setStatus(TeacherPayoutStatus.PENDING);
        payout = teacherPayoutRepository.save(payout);

        log.info("Initiating teacher payout: teacherId={}, payoutId={}, amount={}, sessionCount={}, method={}, mock={}",
            teacherId, payout.getId(), amount, pendingSessions.size(), payoutMethod, mockPayoutsEnabled);

        if (mockPayoutsEnabled) {
            payout.setStatus(TeacherPayoutStatus.PROCESSED);
            payout.setRazorpayPayoutId("mock_" + payout.getId());
            payout.setProcessedAt(Instant.now());
        } else {
            RazorpayPayoutService.PayoutResult result = payoutMethod == PayoutMethod.UPI
                ? razorpayPayoutService.payToUpi(
                    payout.getId(),
                    teacherProfile.getFullName(),
                    teacherProfile.getEmail(),
                    teacherProfile.getMobile(),
                    payoutProfile.getUpiVpa(),
                    amount,
                    payout.getId(),
                    "Ardhana teacher payout")
                : razorpayPayoutService.payToBankAccount(
                    payout.getId(),
                    teacherProfile.getFullName(),
                    teacherProfile.getEmail(),
                    teacherProfile.getMobile(),
                    payoutProfile.getAccountHolderName(),
                    payoutProfile.getAccountNumber(),
                    payoutProfile.getIfsc(),
                    amount,
                    payout.getId(),
                    "Ardhana teacher payout");
            payout.setRazorpayPayoutId(result.razorpayPayoutId);
            payout.setRazorpayFundAccountId(result.razorpayFundAccountId);
            payout.setRazorpayContactId(result.razorpayContactId);
            payout.setStatus(result.status != null ? result.status : TeacherPayoutStatus.FAILED);
            payout.setFailureReason(result.failureReason);
            if (result.success) {
                payout.setProcessedAt(Instant.now());
                payoutProfile.setRazorpayContactId(result.razorpayContactId);
                payoutProfile.setRazorpayFundAccountId(result.razorpayFundAccountId);
                teacherPayoutProfileRepository.save(payoutProfile);
            }
        }
        payout = teacherPayoutRepository.save(payout);

        // Only skip linking sessions if RazorpayX outright rejected the payout
        // or it locally failed — every other status (queued/processing/processed)
        // means the transfer is underway or done, so mark these sessions paid.
        boolean rejected = payout.getStatus() == TeacherPayoutStatus.FAILED
            || payout.getStatus() == TeacherPayoutStatus.REJECTED;
        if (!rejected) {
            for (ClassSchedule session : pendingSessions) {
                session.setTeacherPayoutId(payout.getId());
            }
            classScheduleRepository.saveAll(pendingSessions);
            log.info("Teacher payout linked to {} session(s): payoutId={}, status={}",
                pendingSessions.size(), payout.getId(), payout.getStatus());
        } else {
            log.error("Teacher payout REJECTED/FAILED, sessions left unpaid: payoutId={}, reason={}",
                payout.getId(), payout.getFailureReason());
        }

        return toPayoutResponse(payout, teacherProfile.getFullName());
    }

    /** Admin: full payout history across all teachers, most recent first. */
    @Transactional(readOnly = true)
    public List<TeacherPayoutResponse> getAllPayoutHistory() {
        return teacherPayoutRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(p -> toPayoutResponse(p, resolveTeacherName(p.getTeacherId())))
            .toList();
    }

    /** The signed-in teacher's own earnings summary. */
    @Transactional(readOnly = true)
    public TeacherEarningsResponse getEarnings(String teacherId) {
        TeacherPayoutProfile payoutProfile = teacherPayoutProfileRepository.findById(teacherId).orElse(null);

        List<ClassSchedule> pending = classScheduleRepository
            .findByTeacherIdAndStatusAndTeacherPayoutIdIsNull(teacherId, ClassSessionStatus.COMPLETED);
        int pendingCount = pending.size();
        BigDecimal rate = payoutProfile != null ? payoutProfile.getRatePerSession() : null;
        BigDecimal pendingAmount = rate != null ? rate.multiply(BigDecimal.valueOf(pendingCount)) : null;

        List<TeacherPayout> payouts = teacherPayoutRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        BigDecimal totalPaid = payouts.stream()
            .filter(p -> p.getStatus() == TeacherPayoutStatus.PROCESSED)
            .map(TeacherPayout::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String teacherName = resolveTeacherName(teacherId);
        List<TeacherPayoutResponse> history = payouts.stream()
            .map(p -> toPayoutResponse(p, teacherName))
            .toList();

        boolean hasBankDetails = payoutProfile != null && payoutProfile.hasBankDetails();
        String accountHolderName = payoutProfile != null ? payoutProfile.getAccountHolderName() : null;
        String maskedAccountNumber = payoutProfile != null ? maskAccountNumber(payoutProfile.getAccountNumber()) : null;
        String maskedIfsc = payoutProfile != null ? maskIfsc(payoutProfile.getIfsc()) : null;
        boolean hasUpiDetails = payoutProfile != null && payoutProfile.hasUpiDetails();
        String upiVpa = payoutProfile != null ? payoutProfile.getUpiVpa() : null;
        PayoutMethod effectiveMethod = payoutProfile != null ? payoutProfile.resolvePayoutMethod() : null;
        PayoutMethod preferredMethod = payoutProfile != null ? payoutProfile.getPreferredPayoutMethod() : null;

        return new TeacherEarningsResponse(rate, pendingCount, pendingAmount, totalPaid,
            hasBankDetails, accountHolderName, maskedAccountNumber, maskedIfsc,
            hasUpiDetails, upiVpa,
            effectiveMethod != null ? effectiveMethod.name() : null,
            preferredMethod != null ? preferredMethod.name() : null,
            history);
    }

    /** Admin sets/updates the agreed per-session rate for a teacher. */
    @Transactional
    public void updateRate(String teacherId, UpdateTeacherRateRequest request) {
        TeacherPayoutProfile profile = teacherPayoutProfileRepository.findById(teacherId)
            .orElseGet(() -> newProfile(teacherId));
        profile.setRatePerSession(request.getRatePerSession());
        teacherPayoutProfileRepository.save(profile);
        log.info("Teacher rate updated: teacherId={}, ratePerSession={}", teacherId, request.getRatePerSession());
    }

    /** Teacher sets/updates their OWN bank account details. */
    @Transactional
    public void updateBankDetails(String teacherId, UpdateBankDetailsRequest request) {
        TeacherPayoutProfile profile = teacherPayoutProfileRepository.findById(teacherId)
            .orElseGet(() -> newProfile(teacherId));
        profile.setAccountHolderName(request.getAccountHolderName().trim());
        profile.setAccountNumber(request.getAccountNumber().trim());
        profile.setIfsc(request.getIfsc().trim().toUpperCase());
        // Bank details changed — any previously cached Razorpay fund account
        // is now stale; a fresh one gets created on the next payout.
        profile.setRazorpayContactId(null);
        profile.setRazorpayFundAccountId(null);
        teacherPayoutProfileRepository.save(profile);
        log.info("Teacher bank details updated: teacherId={}", teacherId);
    }

    /** Teacher sets/updates their OWN UPI VPA. */
    @Transactional
    public void updateUpiDetails(String teacherId, UpdateUpiDetailsRequest request) {
        TeacherPayoutProfile profile = teacherPayoutProfileRepository.findById(teacherId)
            .orElseGet(() -> newProfile(teacherId));
        profile.setUpiVpa(request.getVpa().trim());
        teacherPayoutProfileRepository.save(profile);
        log.info("Teacher UPI details updated: teacherId={}", teacherId);
    }

    /**
     * Teacher chooses which method (bank account or UPI) to be paid via, when
     * both are on file. Rejected if the chosen method has no details yet.
     */
    @Transactional
    public void updatePreferredPayoutMethod(String teacherId, UpdatePreferredPayoutMethodRequest request) {
        TeacherPayoutProfile profile = teacherPayoutProfileRepository.findById(teacherId).orElse(null);
        boolean methodIsReady = profile != null && (
            (request.getMethod() == PayoutMethod.BANK_ACCOUNT && profile.hasBankDetails())
            || (request.getMethod() == PayoutMethod.UPI && profile.hasUpiDetails()));
        if (!methodIsReady) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Add " + (request.getMethod() == PayoutMethod.UPI ? "UPI" : "bank account") + " details first.");
        }
        profile.setPreferredPayoutMethod(request.getMethod());
        teacherPayoutProfileRepository.save(profile);
        log.info("Teacher preferred payout method updated: teacherId={}, method={}", teacherId, request.getMethod());
    }

    private TeacherPayoutProfile newProfile(String teacherId) {
        TeacherPayoutProfile profile = new TeacherPayoutProfile();
        profile.setUserId(teacherId);
        return profile;
    }

    private String resolveTeacherName(String teacherId) {
        return userProfileRepository.findById(teacherId).map(UserProfile::getFullName).orElse("Unknown");
    }

    private TeacherPayoutResponse toPayoutResponse(TeacherPayout p, String teacherName) {
        return new TeacherPayoutResponse(
            p.getId(), p.getTeacherId(), teacherName, p.getAmount(), p.getCurrency(),
            p.getStatus().name(), p.getSessionCount(), p.getRazorpayPayoutId(),
            p.getFailureReason(), p.getCreatedAt(), p.getProcessedAt());
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return null;
        return "•••• " + accountNumber.substring(accountNumber.length() - 4);
    }

    private static String maskIfsc(String ifsc) {
        if (ifsc == null || ifsc.length() < 6) return null;
        return ifsc.substring(0, 4) + "•••" + ifsc.substring(ifsc.length() - 2);
    }
}
