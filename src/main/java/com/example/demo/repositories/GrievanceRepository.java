package com.example.demo.repositories;

import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    // Grievances whose deadline has passed and whose status is one of the given (open) statuses
    List<Grievance> findByStatusInAndSlaDeadlineBefore(List<GrievanceStatus> statuses, LocalDateTime time);

    boolean existsByCategoryId(Long categoryId);
    boolean existsByDepartmentId(Long departmentId);
}
