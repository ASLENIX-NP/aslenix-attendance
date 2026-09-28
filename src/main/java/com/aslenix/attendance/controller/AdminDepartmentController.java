package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/admin/departments", "/admin/settings/departments"})
public class AdminDepartmentController {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public AdminDepartmentController(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    // ============================================================
    // REDIRECT TO SETTINGS DEPARTMENTS SECTION
    // ============================================================
    @GetMapping
    public String index() {
        return "redirect:/admin/settings#departmentsSection";
    }

    // ============================================================
    // ADD NEW DEPARTMENT
    // ============================================================
    @PostMapping("/add")
    public String addDepartment(
            @RequestParam("name") String name,
            @RequestParam(value = "abbreviation", required = false) String abbreviation,
            RedirectAttributes redirectAttributes) {

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("deptError", "Department name cannot be empty.");
            return "redirect:/admin/settings#departmentsSection";
        }

        String trimmedName = name.trim();
        String trimmedAbbrev = (abbreviation != null) ? abbreviation.trim().toUpperCase() : "";

        // Check if matching department already exists (case-insensitive)
        Optional<Department> existingOpt = departmentRepository.findByNameIgnoreCase(trimmedName);

        if (existingOpt.isPresent()) {
            Department existing = existingOpt.get();
            if (existing.isActive()) {
                redirectAttributes.addFlashAttribute("deptError",
                        "Department '" + trimmedName + "' already exists.");
                return "redirect:/admin/settings#departmentsSection";
            } else {
                // Reactivate previously deleted department
                existing.setActive(true);
                if (!trimmedAbbrev.isEmpty()) {
                    existing.setAbbreviation(trimmedAbbrev);
                } else if (existing.getAbbreviation() == null || existing.getAbbreviation().isBlank()) {
                    existing.setAbbreviation(existing.getEffectiveAbbreviation());
                }
                departmentRepository.save(existing);
                redirectAttributes.addFlashAttribute("deptSuccess",
                        "Department '" + existing.getName() + "' reactivated successfully.");
                return "redirect:/admin/settings#departmentsSection";
            }
        }

        // Create new department
        Department department = new Department();
        department.setName(trimmedName);
        if (!trimmedAbbrev.isEmpty()) {
            department.setAbbreviation(trimmedAbbrev);
        } else {
            department.setAbbreviation(department.getEffectiveAbbreviation());
        }
        department.setActive(true);
        departmentRepository.save(department);

        redirectAttributes.addFlashAttribute("deptSuccess",
                "Department '" + department.getName() + " (" + department.getEffectiveAbbreviation() + ")' added successfully.");
        return "redirect:/admin/settings#departmentsSection";
    }

    // ============================================================
    // DELETE DEPARTMENT
    // ============================================================
    @PostMapping("/delete/{id}")
    public String deleteDepartment(
            @PathVariable("id") Long id,
            @RequestParam(value = "reassignDepartmentId", required = false) Long reassignDepartmentId,
            RedirectAttributes redirectAttributes) {

        Department department = departmentRepository.findById(id).orElse(null);
        if (department == null) {
            redirectAttributes.addFlashAttribute("deptError", "Department not found.");
            return "redirect:/admin/settings#departmentsSection";
        }

        long employeeCount = employeeRepository.countByDepartmentId(id);

        if (employeeCount > 0) {
            if (reassignDepartmentId != null) {
                if (reassignDepartmentId.equals(id)) {
                    redirectAttributes.addFlashAttribute("deptError",
                            "Cannot reassign employees to the department being deleted.");
                    return "redirect:/admin/settings#departmentsSection";
                }

                Department targetDept = departmentRepository.findById(reassignDepartmentId).orElse(null);
                if (targetDept == null || !targetDept.isActive()) {
                    redirectAttributes.addFlashAttribute("deptError",
                            "Selected reassignment department is invalid or inactive.");
                    return "redirect:/admin/settings#departmentsSection";
                }

                // Transfer all employees to target department
                List<Employee> assignedEmployees = employeeRepository.findByDepartmentId(id);
                for (Employee emp : assignedEmployees) {
                    emp.setDepartment(targetDept);
                    employeeRepository.save(emp);
                }
            } else {
                redirectAttributes.addFlashAttribute("deptError",
                        "Cannot remove department '" + department.getName() + "' because "
                                + employeeCount + " employee(s) are assigned to it. Please select a transfer department.");
                return "redirect:/admin/settings#departmentsSection";
            }
        }

        // Safe removal: attempt hard delete first, fallback to soft deactivate
        try {
            departmentRepository.delete(department);
        } catch (Exception ex) {
            department.setActive(false);
            departmentRepository.save(department);
        }

        redirectAttributes.addFlashAttribute("deptSuccess",
                "Department '" + department.getName() + "' removed successfully.");
        return "redirect:/admin/settings#departmentsSection";
    }

    // ============================================================
    // EDIT DEPARTMENT
    // ============================================================
    @PostMapping("/edit/{id}")
    public String editDepartment(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "abbreviation", required = false) String abbreviation,
            RedirectAttributes redirectAttributes) {

        Department department = departmentRepository.findById(id).orElse(null);
        if (department == null) {
            redirectAttributes.addFlashAttribute("deptError", "Department not found.");
            return "redirect:/admin/settings#departmentsSection";
        }

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("deptError", "Department name cannot be empty.");
            return "redirect:/admin/settings#departmentsSection";
        }

        String trimmedName = name.trim();
        String trimmedAbbrev = (abbreviation != null) ? abbreviation.trim().toUpperCase() : "";

        // Check for name collision with another department
        Optional<Department> collision = departmentRepository.findByNameIgnoreCase(trimmedName);
        if (collision.isPresent() && !collision.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("deptError",
                    "Another department with name '" + trimmedName + "' already exists.");
            return "redirect:/admin/settings#departmentsSection";
        }

        department.setName(trimmedName);
        if (!trimmedAbbrev.isEmpty()) {
            department.setAbbreviation(trimmedAbbrev);
        }
        departmentRepository.save(department);

        redirectAttributes.addFlashAttribute("deptSuccess",
                "Department '" + department.getName() + "' updated successfully.");
        return "redirect:/admin/settings#departmentsSection";
    }
}
