package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.EmployeeSequence;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.EmployeeSequenceRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for generating sequential, collision-free employee codes
 * and login usernames following the format: DEPARTMENT_ABBREVIATION-NUMBER.
 *
 * Sequence starts at 1001 and increments globally for all new employees.
 */
@Service
public class EmployeeCodeService {

    public static final String SEQUENCE_ID = "GLOBAL_EMPLOYEE";
    public static final long START_NUMBER = 1001L;

    // Pattern matching [PREFIX]-[DIGITS] where digits are 4 or more
    private static final Pattern CODE_PATTERN = Pattern.compile("^([A-Za-z]+)-(\\d+)$");

    private final EmployeeSequenceRepository sequenceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeCodeService(
            EmployeeSequenceRepository sequenceRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.sequenceRepository = sequenceRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Generates and commits the next unique employee code and username.
     * E.g. FE-1001, BE-1002, FE-1003, AD-1004, DM-1005.
     */
    @Transactional
    public synchronized String generateNextEmployeeCode(Department department) {
        String abbrev = resolveAbbreviation(department);
        long number = getAndAdvanceNextNumber(abbrev);
        return abbrev + "-" + number;
    }

    /**
     * Peeks at the next sequential number that will be assigned.
     */
    @Transactional(readOnly = true)
    public synchronized long peekNextEmployeeNumber() {
        long currentMax = findMaxExistingNumber();
        EmployeeSequence seq = sequenceRepository.findById(SEQUENCE_ID).orElse(null);
        if (seq != null && seq.getNextNumber() != null) {
            return Math.max(seq.getNextNumber(), Math.max(currentMax + 1, START_NUMBER));
        }
        return Math.max(currentMax + 1, START_NUMBER);
    }

    /**
     * Previews the next employee code for a department without advancing the sequence.
     */
    @Transactional(readOnly = true)
    public String previewNextEmployeeCode(Department department) {
        String abbrev = resolveAbbreviation(department);
        return abbrev + "-" + peekNextEmployeeNumber();
    }

    private synchronized long getAndAdvanceNextNumber(String abbrev) {
        long currentMax = findMaxExistingNumber();
        EmployeeSequence seq = sequenceRepository.findById(SEQUENCE_ID).orElse(null);

        long candidate;
        if (seq == null) {
            candidate = Math.max(currentMax + 1, START_NUMBER);
            seq = new EmployeeSequence(SEQUENCE_ID, candidate + 1);
        } else {
            candidate = Math.max(seq.getNextNumber(), Math.max(currentMax + 1, START_NUMBER));
        }

        // Advance candidate if code or username is already taken
        while (isCodeOrUsernameTaken(abbrev, candidate)) {
            candidate++;
        }

        seq.setNextNumber(candidate + 1);
        sequenceRepository.save(seq);
        return candidate;
    }

    private boolean isCodeOrUsernameTaken(String abbrev, long number) {
        String candidateCode = abbrev + "-" + number;
        return employeeRepository.existsByEmployeeCode(candidateCode)
                || userRepository.existsByUsername(candidateCode)
                || userRepository.existsByUsernameIgnoreCase(candidateCode);
    }

    /**
     * Scans existing employees and users to find the highest sequential number
     * starting from the 1000 baseline.
     */
    private long findMaxExistingNumber() {
        long max = 1000L;
        Set<String> knownPrefixes = getRecognizedDepartmentAbbreviations();

        List<Employee> allEmployees = employeeRepository.findAll();
        for (Employee emp : allEmployees) {
            if (emp.getEmployeeCode() != null) {
                max = checkAndExtractNumber(emp.getEmployeeCode().trim(), knownPrefixes, max);
            }
        }

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getUsername() != null) {
                max = checkAndExtractNumber(user.getUsername().trim(), knownPrefixes, max);
            }
        }

        return max;
    }

    private long checkAndExtractNumber(String text, Set<String> knownPrefixes, long currentMax) {
        Matcher matcher = CODE_PATTERN.matcher(text);
        if (matcher.matches()) {
            String prefix = matcher.group(1).toUpperCase();
            if (knownPrefixes.contains(prefix)) {
                try {
                    long num = Long.parseLong(matcher.group(2));
                    if (num >= 1000 && num > currentMax) {
                        return num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return currentMax;
    }

    /**
     * Resolves the department abbreviation or provides fallback.
     */
    public String resolveAbbreviation(Department department) {
        if (department != null) {
            return department.getEffectiveAbbreviation();
        }
        return "EMP";
    }

    /**
     * Set of all known department abbreviations to distinguish sequential numbers
     * from legacy random ASL-XXXXX or ADM-001 codes.
     */
    public Set<String> getRecognizedDepartmentAbbreviations() {
        Set<String> set = new HashSet<>();
        set.add("FE");
        set.add("BE");
        set.add("AD");
        set.add("DM");

        try {
            List<Department> departments = departmentRepository.findAll();
            for (Department d : departments) {
                if (d.getEffectiveAbbreviation() != null) {
                    set.add(d.getEffectiveAbbreviation());
                }
            }
        } catch (Exception ignored) {
        }
        return set;
    }
}
