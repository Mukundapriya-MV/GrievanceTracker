package com.example.demo.services;

import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.models.Escalation;
import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceStatus;
import com.example.demo.repositories.EscalationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationRepository escalationRepository;

    public EscalationService(EscalationRepository escalationRepository) {
        this.escalationRepository = escalationRepository;
    }

    public List<Escalation> getAll() {
        return escalationRepository.findAll();
    }

    public Escalation getById(Long id) {
        return escalationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation not found with id " + id));
    }

    /**
     * Creates an escalation for the grievance unless one already exists.
     * @return true if a new escalation was created, false if the grievance was already escalated.
     */
    public boolean escalate(Grievance grievance, String reason) {
        if (grievance.getStatus() == GrievanceStatus.RESOLVED || grievance.getStatus() == GrievanceStatus.CLOSED
                || escalationRepository.existsByGrievanceId(grievance.getId())) {
            return false; // no duplicate escalation
        }
        Escalation escalation = new Escalation();
        escalation.setGrievance(grievance);
        escalation.setEscalatedToOfficer("Senior " + grievance.getDepartment().getName() + " Officer");
        escalation.setReason(reason);
        escalation.setEscalatedAt(LocalDateTime.now());
        escalationRepository.save(escalation);
        log.info("ESCALATION: grievance #{} ({}, {}) escalated to '{}' (reason: {})",
            grievance.getId(), grievance.getCategory().getName(), grievance.getSeverity(),
            escalation.getEscalatedToOfficer(), reason);
        return true;
    }
}
