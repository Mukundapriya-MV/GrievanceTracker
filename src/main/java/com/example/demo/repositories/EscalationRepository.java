package com.example.demo.repositories;

import com.example.demo.models.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    boolean existsByGrievanceId(Long grievanceId);
}
