package com.aslenix.attendance.dto;

/**
 * A selectable evaluation month for the admin UI.
 */
public record MonthOption(int year, int month, String label, boolean hasResults) {
}
