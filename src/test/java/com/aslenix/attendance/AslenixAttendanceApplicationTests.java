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
	void testDiscussionTabRemovedFromTemplates() throws Exception {
		String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/tasks.html"));
		String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/tasks.html"));

		assertFalse(adminHtml.contains("data-tab=\"tabDiscussion\""), "Admin template should not contain tabDiscussion button");
		assertFalse(empHtml.contains("data-tab=\"tabDiscussion\""), "Employee template should not contain tabDiscussion button");

		assertFalse(adminHtml.contains("id=\"tabDiscussion\""), "Admin template should not contain tabDiscussion div");
		assertFalse(empHtml.contains("id=\"tabDiscussion\""), "Employee template should not contain tabDiscussion div");
	}

	@Test
	void testEmployeeModalFieldsAreReadOnly() throws Exception {
		String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/tasks.html"));

		// Title is readonly
		assertTrue(empHtml.contains("id=\"modalTitleInput\" class=\"form-input readonly-field\" placeholder=\"Task title\" readonly"),
				"Employee title must be readonly");

		// Description is readonly
		assertTrue(empHtml.contains("id=\"modalDescriptionInput\" class=\"form-textarea readonly-field\" placeholder=\"No description provided\" readonly"),
				"Employee description must be readonly");

		// Complexity is disabled
		assertTrue(empHtml.contains("id=\"modalComplexitySelect\" class=\"form-select readonly-field\" disabled"),
				"Employee complexity select must be disabled");

		// Priority is disabled
		assertTrue(empHtml.contains("id=\"modalPrioritySelect\" class=\"form-select readonly-field\" disabled"),
				"Employee priority select must be disabled");

		// Complexity Guide cards are read-only without onclick
		assertTrue(empHtml.contains("class=\"comp-card read-only\" id=\"cardCompSmall\""),
				"Employee cardCompSmall must have read-only class");
		assertFalse(empHtml.contains("id=\"cardCompSmall\" onclick="),
				"Employee cardCompSmall must not have onclick");
		assertFalse(empHtml.contains("id=\"cardCompMedium\" onclick="),
				"Employee cardCompMedium must not have onclick");
		assertFalse(empHtml.contains("id=\"cardCompLarge\" onclick="),
				"Employee cardCompLarge must not have onclick");
		assertFalse(empHtml.contains("id=\"cardCompEpic\" onclick="),
				"Employee cardCompEpic must not have onclick");

		// Deadline BS is readonly and has no datepicker class
		assertTrue(empHtml.contains("id=\"modalDeadlineBsInput\" class=\"form-input readonly-field\" placeholder=\"No deadline set\" readonly"),
				"Employee deadline BS must be readonly");
		assertFalse(empHtml.contains("modalDeadlineBsInput\" class=\"form-input use-nepali-datepicker\""),
				"Employee deadline BS must not have use-nepali-datepicker class");

		// Deadline Time is readonly and disabled
		assertTrue(empHtml.contains("id=\"modalDeadlineTimeInput\" class=\"form-input readonly-field\" value=\"14:00\" readonly disabled"),
				"Employee deadline time must be readonly and disabled");

		// Tags is readonly
		assertTrue(empHtml.contains("id=\"modalTagsInput\" class=\"form-input readonly-field\" placeholder=\"None\" readonly"),
				"Employee tags must be readonly");
	}

	@Test
	void testAdminModalFieldsRemainEditable() throws Exception {
		String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/tasks.html"));

		// Admin title is editable (not readonly)
		assertTrue(adminHtml.contains("id=\"modalTitleInput\" class=\"form-input\""),
				"Admin title must be editable");

		// Admin complexity guide cards have onclick
		assertTrue(adminHtml.contains("id=\"cardCompSmall\" onclick=\"selectComplexityCard('SMALL')\""),
				"Admin cardCompSmall must be interactive");

		// Admin deadline BS has nepali datepicker
		assertTrue(adminHtml.contains("id=\"modalDeadlineBsInput\" class=\"form-input use-nepali-datepicker\""),
				"Admin deadline BS must have nepali datepicker");
	}

	@Test
	void testProgressBarLiveSyncAndAutoCommit() throws Exception {
		String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/tasks.html"));
		String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/tasks.html"));

		// Sliders have onchange auto-commit handlers
		assertTrue(adminHtml.contains("onchange=\"onProgressSliderCommit(this.value)\""),
				"Admin slider must have onchange commit handler");
		assertTrue(empHtml.contains("onchange=\"onSliderInputCommit(this.value)\""),
				"Employee slider must have onchange commit handler");

		// Auto commit functions exist
		assertTrue(adminHtml.contains("function onProgressSliderCommit"),
				"Admin must have onProgressSliderCommit function");
		assertTrue(empHtml.contains("function onSliderInputCommit"),
				"Employee must have onSliderInputCommit function");

		// Task card fragments guard against undefined in deadlineBs
		assertTrue(adminHtml.contains("!#strings.contains(task.deadlineBs, 'undefined')"),
				"Admin task card fragment must guard against undefined deadlineBs");
		assertTrue(empHtml.contains("!#strings.contains(task.deadlineBs, 'undefined')"),
				"Employee task card fragment must guard against undefined deadlineBs");
	}
}
