package com.ardhana.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.SchoolFeeOverviewResponse;
import com.ardhana.dto.SchoolFeeStatusResponse;
import com.ardhana.dto.SchoolPaymentCheckoutResponse;
import com.ardhana.dto.SchoolPaymentResponse;
import com.ardhana.dto.SchoolPaymentVerifyRequest;
import com.ardhana.dto.UpdateSchoolFeeRequest;
import com.ardhana.model.School;
import com.ardhana.model.SchoolPayment;
import com.ardhana.model.SchoolPaymentStatus;
import com.ardhana.model.UserProfile;
import com.ardhana.repository.SchoolPaymentRepository;
import com.ardhana.repository.SchoolRepository;
import com.ardhana.repository.UserProfileRepository;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;

/**
 * Lets a school pay their platform fee via the existing Razorpay Checkout
 * integration (RazorpayService) — mirrors OrderService's checkout/verify
 * pattern, just without cart items/enrollments: one school, one owed amount,
 * one payment.
 */
@Slf4j
@Service
public class SchoolPaymentService {

    private final SchoolRepository schoolRepository;
    private final SchoolPaymentRepository schoolPaymentRepository;
    private final UserProfileRepository userProfileRepository;
    private final RazorpayService razorpayService;

    // Same escape hatch as OrderService — reused rather than duplicated,
    // since it's the same underlying "skip real Razorpay" concern.
    @Value("${app.payments.mock-enabled:false}")
    private boolean mockPaymentsEnabled;

    public SchoolPaymentService(SchoolRepository schoolRepository,
                                 SchoolPaymentRepository schoolPaymentRepository,
                                 UserProfileRepository userProfileRepository,
                                 RazorpayService razorpayService) {
        this.schoolRepository = schoolRepository;
        this.schoolPaymentRepository = schoolPaymentRepository;
        this.userProfileRepository = userProfileRepository;
        this.razorpayService = razorpayService;
    }

    /** The signed-in school's own fee status + payment history. */
    @Transactional(readOnly = true)
    public SchoolFeeStatusResponse getMyFeeStatus(String callerUserId) {
        School school = schoolForCaller(callerUserId);
        List<SchoolPaymentResponse> history = schoolPaymentRepository
            .findBySchoolIdOrderByCreatedAtDesc(school.getId()).stream()
            .map(p -> toResponse(p, school.getName()))
            .toList();
        return new SchoolFeeStatusResponse(school.getId(), school.getName(),
            school.getFeeAmount(), school.getFeeCurrency(), history);
    }

    /** Step 1 of checkout: persist a PENDING school payment and open a matching Razorpay order. */
    @Transactional
    public SchoolPaymentCheckoutResponse initiateCheckout(String callerUserId) {
        School school = schoolForCaller(callerUserId);
        if (school.getFeeAmount() == null || school.getFeeAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fee is currently due for your school.");
        }

        String currency = (school.getFeeCurrency() == null || school.getFeeCurrency().isBlank())
            ? "INR" : school.getFeeCurrency();

        SchoolPayment payment = new SchoolPayment();
        payment.setSchoolId(school.getId());
        payment.setAmount(school.getFeeAmount());
        payment.setCurrency(currency);
        payment.setStatus(SchoolPaymentStatus.PENDING);
        payment = schoolPaymentRepository.save(payment);

        log.info("School checkout initiated: schoolId={}, schoolPaymentId={}, amount={}, currency={}",
            school.getId(), payment.getId(), payment.getAmount(), currency);

        if (mockPaymentsEnabled) {
            String mockRazorpayOrderId = "mock_order_" + payment.getId();
            log.warn("PAYMENTS_MOCK_ENABLED=true — skipping real Razorpay order creation for schoolPaymentId={}",
                payment.getId());
            payment.setRazorpayOrderId(mockRazorpayOrderId);
            schoolPaymentRepository.save(payment);
            return new SchoolPaymentCheckoutResponse(
                payment.getId(), mockRazorpayOrderId, payment.getAmount(), currency, razorpayService.getKeyId(), true);
        }

        String razorpayOrderId;
        try {
            razorpayOrderId = razorpayService.createOrder(payment.getAmount(), currency, payment.getId());
        } catch (RazorpayException e) {
            log.error("Razorpay order creation FAILED for schoolPaymentId={}: {}", payment.getId(), e.getMessage(), e);
            payment.setStatus(SchoolPaymentStatus.FAILED);
            schoolPaymentRepository.save(payment);
            throw new IllegalStateException("Unable to create Razorpay order: " + e.getMessage(), e);
        }

        payment.setRazorpayOrderId(razorpayOrderId);
        schoolPaymentRepository.save(payment);

        return new SchoolPaymentCheckoutResponse(
            payment.getId(), razorpayOrderId, payment.getAmount(), currency, razorpayService.getKeyId(), false);
    }

    /** Step 2 of checkout: verify the Razorpay signature and mark the payment (and the school's fee) settled. */
    @Transactional
    public SchoolPaymentResponse confirmPayment(String callerUserId, SchoolPaymentVerifyRequest request) {
        School callerSchool = schoolForCaller(callerUserId);

        SchoolPayment payment = schoolPaymentRepository.findById(request.getSchoolPaymentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!payment.getSchoolId().equals(callerSchool.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This payment does not belong to your school.");
        }

        if (payment.getStatus() == SchoolPaymentStatus.CONFIRMED) {
            return toResponse(payment, callerSchool.getName()); // idempotent no-op
        }

        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order id does not match this payment");
        }

        boolean valid = mockPaymentsEnabled
            ? true
            : razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!valid) {
            log.warn("School payment signature verification FAILED: schoolPaymentId={}", payment.getId());
            payment.setStatus(SchoolPaymentStatus.FAILED);
            schoolPaymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature verification failed");
        }

        payment.setStatus(SchoolPaymentStatus.CONFIRMED);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setConfirmedAt(Instant.now());
        schoolPaymentRepository.save(payment);

        // Only clear the school's outstanding fee if it still matches what
        // this payment covered — if the admin has since set a NEW (different)
        // fee, don't wipe that out from under them.
        if (callerSchool.getFeeAmount() != null && callerSchool.getFeeAmount().compareTo(payment.getAmount()) == 0) {
            callerSchool.setFeeAmount(null);
            schoolRepository.save(callerSchool);
        }

        log.info("School payment CONFIRMED: schoolId={}, schoolPaymentId={}, amount={}",
            callerSchool.getId(), payment.getId(), payment.getAmount());

        return toResponse(payment, callerSchool.getName());
    }

    /** Admin: every school, with their current fee due and whether they have a login yet. */
    @Transactional(readOnly = true)
    public List<SchoolFeeOverviewResponse> getAdminOverview() {
        return schoolRepository.findAllByOrderByNameAsc().stream()
            .map(s -> new SchoolFeeOverviewResponse(
                s.getId(), s.getName(), s.getFeeAmount(), s.getFeeCurrency(), s.getLoginUserId() != null))
            .toList();
    }

    /** Admin: full school-payment history across all schools. */
    @Transactional(readOnly = true)
    public List<SchoolPaymentResponse> getAllPaymentHistory() {
        return schoolPaymentRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(p -> toResponse(p, resolveSchoolName(p.getSchoolId())))
            .toList();
    }

    /** Admin sets (or updates) the amount currently owed by a school. */
    @Transactional
    public void updateFee(String schoolId, UpdateSchoolFeeRequest request) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
        school.setFeeAmount(request.getFeeAmount());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            school.setFeeCurrency(request.getCurrency().toUpperCase());
        } else if (school.getFeeCurrency() == null) {
            school.setFeeCurrency("INR");
        }
        schoolRepository.save(school);
        log.info("School fee updated: schoolId={}, feeAmount={}, currency={}",
            schoolId, request.getFeeAmount(), school.getFeeCurrency());
    }

    private School schoolForCaller(String callerUserId) {
        UserProfile profile = userProfileRepository.findById(callerUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (profile.getSchoolId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your account isn't associated with a school.");
        }
        return schoolRepository.findById(profile.getSchoolId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private String resolveSchoolName(String schoolId) {
        return schoolRepository.findById(schoolId).map(School::getName).orElse("Unknown");
    }

    private SchoolPaymentResponse toResponse(SchoolPayment p, String schoolName) {
        return new SchoolPaymentResponse(
            p.getId(), p.getSchoolId(), schoolName, p.getAmount(), p.getCurrency(),
            p.getStatus().name(), p.getCreatedAt(), p.getConfirmedAt());
    }
}
