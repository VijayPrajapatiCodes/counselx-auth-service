package com.counselx.auth.repository;

import com.counselx.auth.entity.otp_verifications;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<otp_verifications, Long> {

    List<otp_verifications> findAllByUser_IdAndUsedFalse(Long userId);

    Optional<otp_verifications> findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
