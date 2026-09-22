package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.EmployeePerformanceScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePerformanceScoreRepository
        extends JpaRepository<EmployeePerformanceScore, Long> {

    List<EmployeePerformanceScore> findByEvalYearAndEvalMonthOrderByRankAsc(int evalYear, int evalMonth);

    List<EmployeePerformanceScore> findByEvalYearAndEvalMonth(int evalYear, int evalMonth);

    Optional<EmployeePerformanceScore> findByEmployeeIdAndEvalYearAndEvalMonth(
            Long employeeId, int evalYear, int evalMonth);

    Optional<EmployeePerformanceScore> findByEvalYearAndEvalMonthAndEmployeeOfTheMonthTrue(
            int evalYear, int evalMonth);

    boolean existsByEvalYearAndEvalMonth(int evalYear, int evalMonth);

    List<EmployeePerformanceScore> findByEmployeeIdOrderByEvalYearDescEvalMonthDesc(Long employeeId);
}
