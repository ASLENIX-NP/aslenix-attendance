package com.aslenix.attendance.dto;

/**
 * Summary returned after a monthly evaluation run.
 */
public record EvaluationResult(
        int year,
        int month,
        int employeesEvaluated,
        String winnerName,
        double winnerScore,
        boolean winnerSelected
) {
}
