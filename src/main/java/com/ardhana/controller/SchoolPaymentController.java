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

import com.ardhana.dto.SchoolFeeOverviewResponse;
import com.ardhana.dto.SchoolFeeStatusResponse;
import com.ardhana.dto.SchoolPaymentCheckoutResponse;
import com.ardhana.dto.SchoolPaymentResponse;
import com.ardhana.dto.SchoolPaymentVerifyRequest;
import com.ardhana.dto.UpdateSchoolFeeRequest;
import com.ardhana.model.AppRole;
import com.ardhana.service.SchoolPaymentService;
import com.ardhana.service.UserRoleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin manages what each school owes; a school (signed in via its own
 * role=school login) views and pays its own fee. Admin checks are done here
 * at the controller level, matching the rest of the app's admin-only
 * endpoints (SecurityConfig only enforces "authenticated").
 */
@Slf4j
@RestController
@RequestMapping("/api/school-payments")
public class SchoolPaymentController {

    private final SchoolPaymentService schoolPaymentService;
    private final UserRoleService userRoleService;

    public SchoolPaymentController(SchoolPaymentService schoolPaymentService, UserRoleService userRoleService) {
        this.schoolPaymentService = schoolPaymentService;
        this.userRoleService = userRoleService;
    }

    /** The signed-in school's own fee status + payment history. */
    @GetMapping("/my")
    public ResponseEntity<SchoolFeeStatusResponse> myFeeStatus(Principal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(schoolPaymentService.getMyFeeStatus(principal.getName()));
    }

    /** The signed-in school starts paying their currently-due fee. */
    @PostMapping("/checkout")
    public ResponseEntity<SchoolPaymentCheckoutResponse> checkout(Principal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(schoolPaymentService.initiateCheckout(principal.getName()));
    }

    /** Called once Razorpay Checkout's handler callback fires with a payment id + signature. */
    @PostMapping("/verify-payment")
    public ResponseEntity<SchoolPaymentResponse> verifyPayment(Principal principal,
                                                                 @Valid @RequestBody SchoolPaymentVerifyRequest request) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(schoolPaymentService.confirmPayment(principal.getName(), request));
    }

    /** Admin: every school, with their current fee due and login status. */
    @GetMapping("/overview")
    public ResponseEntity<List<SchoolFeeOverviewResponse>> overview(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(schoolPaymentService.getAdminOverview());
    }

    /** Admin: full school-payment history across all schools. */
    @GetMapping
    public ResponseEntity<List<SchoolPaymentResponse>> history(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(schoolPaymentService.getAllPaymentHistory());
    }

    /** Admin sets/updates the amount currently owed by a school. */
    @PutMapping("/fee/{schoolId}")
    public ResponseEntity<Void> updateFee(Principal principal, @PathVariable String schoolId,
                                           @Valid @RequestBody UpdateSchoolFeeRequest request) {
        requireAdmin(principal);
        schoolPaymentService.updateFee(schoolId, request);
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
            log.warn("Non-admin user {} attempted to access school payments admin endpoints", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
