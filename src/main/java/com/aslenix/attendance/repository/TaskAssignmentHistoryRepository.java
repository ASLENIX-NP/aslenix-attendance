package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentHistoryRepository extends JpaRepository<TaskAssignmentHistory, Long> {

    List<TaskAssignmentHistory> findByAssignmentOrderByCreatedAtDesc(TaskAssignment assignment);

    List<TaskAssignmentHistory> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
}
