package com.tuition.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuition.model.EmailOtp;


@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, String> {

    Optional<EmailOtp> findTopByEmailOrderByExpiryTimeDesc(String email);
}
