package com.ardhana.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.SchoolPayment;

@Repository
public interface SchoolPaymentRepository extends JpaRepository<SchoolPayment, String> {
    List<SchoolPayment> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    List<SchoolPayment> findAllByOrderByCreatedAtDesc();
    Optional<SchoolPayment> findByRazorpayOrderId(String razorpayOrderId);
}
