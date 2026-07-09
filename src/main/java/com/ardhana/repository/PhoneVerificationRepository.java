package com.ardhana.repository;

import com.ardhana.model.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, String> {
    Optional<PhoneVerification> findFirstByMobileAndUsedFalseOrderByVerifiedAtDesc(String mobile);
}
