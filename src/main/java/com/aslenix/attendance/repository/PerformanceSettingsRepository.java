package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.PerformanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerformanceSettingsRepository
        extends JpaRepository<PerformanceSettings, Long> {

    Optional<PerformanceSettings> findFirstByOrderByIdAsc();
}
