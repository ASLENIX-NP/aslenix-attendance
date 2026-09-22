package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.PerformanceTaskBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceTaskBreakdownRepository
        extends JpaRepository<PerformanceTaskBreakdown, Long> {

    List<PerformanceTaskBreakdown> findByScoreIdOrderByTotalPointsDesc(Long scoreId);
}
