package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.PerformanceSettings;
import com.aslenix.attendance.repository.PerformanceSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the singleton {@link PerformanceSettings} row that drives the
 * Employee of the Month scoring engine. All scoring values are configurable
 * here; the engine never hard-codes a number.
 */
@Service
public class PerformanceSettingsService {

    private final PerformanceSettingsRepository repository;

    public PerformanceSettingsService(PerformanceSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the existing settings row, creating one with defaults if absent.
     */
    @Transactional
    public PerformanceSettings getSettings() {
        return repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> repository.save(new PerformanceSettings()));
    }

    @Transactional(readOnly = true)
    public boolean exists() {
        return repository.count() > 0;
    }

    /**
     * Persists new settings.
     *
     * @throws IllegalArgumentException if the attendance/task weights do not sum
     *                                  to exactly 100, or any value is negative.
     */
    @Transactional
    public PerformanceSettings saveSettings(PerformanceSettings form) {
        if (form == null) {
            throw new IllegalArgumentException("Settings are required.");
        }

        if (form.getAttendanceWeight() + form.getTaskWeight() != 100) {
            throw new IllegalArgumentException(
                    "Attendance weight and task weight must add up to exactly 100%.");
        }

        if (form.getAttendanceWeight() < 0 || form.getTaskWeight() < 0) {
            throw new IllegalArgumentException("Weights cannot be negative.");
        }

        PerformanceSettings settings = getSettings();

        settings.setAttendanceWeight(form.getAttendanceWeight());
        settings.setTaskWeight(form.getTaskWeight());

        settings.setLateDeductionPoints(nonNegative(form.getLateDeductionPoints()));
        settings.setEarlyCheckInBonusPoints(nonNegative(form.getEarlyCheckInBonusPoints()));
        settings.setOvertimeBonusPerHour(nonNegative(form.getOvertimeBonusPerHour()));
        settings.setMaxOvertimeBonusHours(Math.max(0, form.getMaxOvertimeBonusHours()));

        settings.setSmallPoints(nonNegative(form.getSmallPoints()));
        settings.setMediumPoints(nonNegative(form.getMediumPoints()));
        settings.setLargePoints(nonNegative(form.getLargePoints()));
        settings.setEpicPoints(nonNegative(form.getEpicPoints()));

        settings.setEarlyCompletionBonusPoints(nonNegative(form.getEarlyCompletionBonusPoints()));
        settings.setOnTimeCompletionPoints(nonNegative(form.getOnTimeCompletionPoints()));
        settings.setLateCompletionDeductionPoints(nonNegative(form.getLateCompletionDeductionPoints()));

        double perfect = form.getTaskPerfectScorePoints();
        settings.setTaskPerfectScorePoints(perfect <= 0 ? 100.0 : perfect);

        return repository.save(settings);
    }

    private double nonNegative(double value) {
        return Math.max(0, value);
    }
}
