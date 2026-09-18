package com.aslenix.attendance.dto;

public class StreakDto {

    private final int currentStreak;
    private final int longestStreak;
    private final String message;

    public StreakDto(int currentStreak, int longestStreak, String message) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.message = message;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public String getMessage() {
        return message;
    }
}
