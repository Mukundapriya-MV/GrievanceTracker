package com.example.demo.services;

import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceStatus;
import com.example.demo.repositories.GrievanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything about SLA time lives here:
 *  1. calculateDeadline()  - the ONLY place that reads app.demo-mode
 *  2. checkAndEscalate()   - scheduled job, runs every 30 seconds
 */
@Service
public class SLAService {

    // The single place where the demo-mode property is read.
    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    private final GrievanceRepository grievanceRepository;
    private final EscalationService escalationService;

    public SLAService(GrievanceRepository grievanceRepository, EscalationService escalationService) {
        this.grievanceRepository = grievanceRepository;
        this.escalationService = escalationService;
    }

    /** demo-mode false: 1 SLA day = 1 day.  demo-mode true: 1 SLA day = 1 minute. */
    public LocalDateTime calculateDeadline(LocalDateTime createdAt, int slaDays) {
        return demoMode ? createdAt.plusMinutes(slaDays) : createdAt.plusDays(slaDays);
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkAndEscalate() {
        List<GrievanceStatus> openStatuses = List.of(GrievanceStatus.PENDING, GrievanceStatus.IN_PROGRESS);
        List<Grievance> overdue = grievanceRepository.findByStatusInAndSlaDeadlineBefore(openStatuses, LocalDateTime.now());
        for (Grievance grievance : overdue) {
            escalationService.escalate(grievance, "SLA exceeded");
        }
    }
}
