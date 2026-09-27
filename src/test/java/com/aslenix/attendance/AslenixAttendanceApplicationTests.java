package com.aslenix.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AslenixAttendanceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testUnnecessaryFieldsRemovedFromTemplates() throws Exception {
		String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/tasks.html"));
		String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/tasks.html"));

		// Deadlines removed
		assertFalse(adminHtml.contains("id=\"modalDeadlineBsInput\""), "Admin template must not contain deadline BS input");
		assertFalse(adminHtml.contains("id=\"modalDeadlineTimeInput\""), "Admin template must not contain deadline time input");
		assertFalse(empHtml.contains("id=\"modalDeadlineBsInput\""), "Employee template must not contain deadline BS input");
		assertFalse(empHtml.contains("id=\"modalDeadlineTimeInput\""), "Employee template must not contain deadline time input");

		// Tags removed
		assertFalse(adminHtml.contains("id=\"modalTagsInput\""), "Admin template must not contain tags input");
		assertFalse(empHtml.contains("id=\"modalTagsInput\""), "Employee template must not contain tags input");

		// Manual main task progress sliders removed
		assertFalse(adminHtml.contains("id=\"modalProgressSlider\""), "Admin template must not contain main task progress slider");
		assertFalse(empHtml.contains("id=\"modalProgressSlider\""), "Employee template must not contain main task progress slider");
	}

	@Test
	void testNewWeeklyWorkflowFieldsPresent() throws Exception {
		String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/tasks.html"));
		String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/tasks.html"));

		// Main task strictly required fields
		assertTrue(adminHtml.contains("id=\"modalTitleInput\""), "Admin must have title input");
		assertTrue(adminHtml.contains("id=\"modalDescriptionInput\""), "Admin must have description input");
		assertTrue(adminHtml.contains("id=\"modalComplexitySelect\""), "Admin must have complexity select");
		assertTrue(adminHtml.contains("id=\"modalPrioritySelect\""), "Admin must have priority select");
		assertTrue(adminHtml.contains("id=\"modalWeeksRequiredInput\""), "Admin must have weeksRequired input");
		assertTrue(adminHtml.contains("id=\"modalProgressBadge\""), "Admin must have automatic total progress badge");

		// Review workflow elements
		assertTrue(adminHtml.contains("btn-approve-subtask"), "Admin must have subtask approve action");
		assertTrue(adminHtml.contains("btn-decline-subtask"), "Admin must have subtask decline action");
		assertTrue(adminHtml.contains("id=\"declineSubtaskModal\""), "Admin must have decline modal with mandatory reason");

		// Employee review workflow elements
		assertTrue(empHtml.contains("btn-submit-review-green"), "Employee must have submit for review action");
		assertTrue(empHtml.contains("id=\"submitReviewModal\""), "Employee must have submit review modal with mandatory note");
		assertTrue(empHtml.contains("subtask-alert-declined"), "Employee must have declined revision alert");
	}
}
