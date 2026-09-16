package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByTaskOrderByIdAsc(Task task);
}
