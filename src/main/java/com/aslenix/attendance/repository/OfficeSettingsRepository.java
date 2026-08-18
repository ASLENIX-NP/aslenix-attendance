package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.OfficeSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficeSettingsRepository
        extends JpaRepository<OfficeSettings, Long> {

    Optional<OfficeSettings> findFirstByOrderByIdAsc();
}