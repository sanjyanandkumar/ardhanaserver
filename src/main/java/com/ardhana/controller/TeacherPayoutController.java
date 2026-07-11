package com.ardhana.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.PendingTeacherPaymentResponse;
import com.ardhana.dto.TeacherEarningsResponse;
import com.ardhana.dto.TeacherPayoutResponse;
import com.ardhana.dto.UpdateBankDetailsRequest;
import com.ardhana.dto.UpdatePreferredPayoutMethodRequest;
import com.ardhana.dto.UpdateTeacherRateRequest;
import com.ardhana.dto.UpdateUpiDetailsRequest;
import com.ardhana.model.AppRole;
import com.ardhana.service.TeacherPayoutService;
import com.ardhana.service.UserRoleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin manages teacher rates and pays out what's owed; teachers view their
 * own earnings and manage their OWN bank details. Admin checks are done here
 * at the controller level (matching ClassScheduleController) since
 * SecurityConfig only enforces "authenticated", not role. Bank account
 * numbers/IFSC are never accepted or returned on the admin-facing endpoints —
 * only a teacher acting on their own id can read/write those (enforced via
 * principal.getName(), not a path variable).
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher-payouts")
public class TeacherPayoutController {

    private final TeacherPayoutService teacherPayoutService;
    private final UserRoleService userRoleService;

    public TeacherPayoutController(TeacherPayoutService teacherPayoutService,
                                    UserRoleService userRoleService) {
        this.teacherPayoutService = teacherPayoutService;
        this.userRoleService = userRoleService;
    }

    /** Admin: every teacher with their rate, pending amount owed, and bank-details status. */
    @GetMapping("/overview")
    public ResponseEntity<List<PendingTeacherPaymentResponse>> overview(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(teacherPayoutService.getAdminOverview());
    }

    /** Admin: full payout history across all teachers. */
    @GetMapping
    public ResponseEntity<List<TeacherPayoutResponse>> history(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(teacherPayoutService.getAllPayoutHistory());
    }

    /** Admin: pays a teacher everything currently owed to them, via a real RazorpayX payout. */
    @PostMapping("/pay/{teacherId}")
    public ResponseEntity<TeacherPayoutResponse> pay(Principal principal, @PathVariable String teacherId) {
        requireAdmin(principal);
        log.info("Admin {} initiating payout for teacher {}", principal.getName(), teacherId);
        return ResponseEntity.ok(teacherPayoutService.pay(teacherId));
    }

    /** Admin: sets/updates the agreed per-session rate for a teacher. */
    @PutMapping("/rate/{teacherId}")
    public ResponseEntity<Void> updateRate(Principal principal, @PathVariable String teacherId,
                                            @Valid @RequestBody UpdateTeacherRateRequest request) {
        requireAdmin(principal);
        teacherPayoutService.updateRate(teacherId, request);
        return ResponseEntity.noContent().build();
    }

    /** The signed-in teacher's own earnings: rate, pending amount, total paid, payout history, bank details (masked). */
    @GetMapping("/my-earnings")
    public ResponseEntity<TeacherEarningsResponse> myEarnings(Principal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(teacherPayoutService.getEarnings(principal.getName()));
    }

    /** The signed-in teacher sets/updates their OWN bank account details. Nobody else can call this on their behalf. */
    @PutMapping("/my-bank-details")
    public ResponseEntity<Void> updateMyBankDetails(Principal principal,
                                                      @Valid @RequestBody UpdateBankDetailsRequest request) {
        requireAuthenticated(principal);
        teacherPayoutService.updateBankDetails(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    /** The signed-in teacher sets/updates their OWN UPI VPA. Nobody else can call this on their behalf. */
    @PutMapping("/my-upi-details")
    public ResponseEntity<Void> updateMyUpiDetails(Principal principal,
                                                     @Valid @RequestBody UpdateUpiDetailsRequest request) {
        requireAuthenticated(principal);
        teacherPayoutService.updateUpiDetails(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    /** The signed-in teacher picks which method (bank account or UPI) to be paid via, when both are on file. */
    @PutMapping("/my-preferred-payout-method")
    public ResponseEntity<Void> updateMyPreferredPayoutMethod(Principal principal,
                                                                @Valid @RequestBody UpdatePreferredPayoutMethodRequest request) {
        requireAuthenticated(principal);
        teacherPayoutService.updatePreferredPayoutMethod(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    private void requireAuthenticated(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private void requireAdmin(Principal principal) {
        requireAuthenticated(principal);
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to access teacher payouts", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
