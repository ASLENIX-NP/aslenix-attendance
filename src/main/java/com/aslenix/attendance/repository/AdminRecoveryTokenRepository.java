package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.AdminRecoveryToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRecoveryTokenRepository
        extends JpaRepository<AdminRecoveryToken, Long> {

    Optional<AdminRecoveryToken> findByTokenHash(String tokenHash);
}
