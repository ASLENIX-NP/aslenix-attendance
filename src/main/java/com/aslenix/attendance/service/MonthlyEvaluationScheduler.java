package com.aslenix.attendance.service;

import com.aslenix.attendance.dto.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Automatically evaluates the previous month at 00:05 on the 1st day of each
 * month and stores the Employee of the Month permanently.
 */
@Component
public class MonthlyEvaluationScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyEvaluationScheduler.class);

    private final PerformanceScoringService scoringService;

    public MonthlyEvaluationScheduler(PerformanceScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @Scheduled(cron = "0 5 0 1 * *")
    public void evaluatePreviousMonth() {
        YearMonth previous = YearMonth.from(LocalDate.now()).minusMonths(1);
        try {
            EvaluationResult result = scoringService.evaluateMonth(
                    previous.getYear(), previous.getMonthValue());
            log.info("Monthly evaluation completed for {}: {} employees, winner={}",
                    previous, result.employeesEvaluated(), result.winnerName());
        } catch (Exception e) {
            log.error("Monthly evaluation failed for {}: {}", previous, e.getMessage(), e);
        }
    }
}
