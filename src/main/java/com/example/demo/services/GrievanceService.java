package com.example.demo.services;

import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.models.Category;
import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceSeverity;
import com.example.demo.models.GrievanceStatus;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.GrievanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GrievanceService {

    private static final Logger log = LoggerFactory.getLogger(GrievanceService.class);

    private final GrievanceRepository grievanceRepository;
    private final CategoryRepository categoryRepository;
    private final SLAService slaService;

    public GrievanceService(GrievanceRepository grievanceRepository,
                            CategoryRepository categoryRepository,
                            SLAService slaService) {
        this.grievanceRepository = grievanceRepository;
        this.categoryRepository = categoryRepository;
        this.slaService = slaService;
    }

    @Transactional
    public Grievance create(Grievance request) {
        Long categoryId = request.getCategory().getId();
        if (categoryId == null) {
            throw new IllegalArgumentException("Invalid category: category.id is required");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid category: no category exists with id " + categoryId));

        // Build a fresh object so the client can never inject status, dates, rating or department.
        Grievance grievance = new Grievance();
        grievance.setCitizenName(request.getCitizenName().trim());
        grievance.setDescription(request.getDescription().trim());
        grievance.setLocation(request.getLocation().trim());
        GrievanceSeverity severity = request.getSeverity() == null
            ? GrievanceSeverity.MEDIUM : request.getSeverity();
        int slaDays = category.getSlaDaysForSeverity(severity);
        grievance.setSeverity(severity);
        grievance.setSlaDurationDays(slaDays);
        grievance.setCategory(category);
        grievance.setDepartment(category.getDepartment());   // auto-routing: always from the category
        grievance.setStatus(GrievanceStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        grievance.setCreatedAt(now);
        grievance.setSlaDeadline(slaService.calculateDeadline(now, slaDays));

        Grievance saved = grievanceRepository.save(grievance);
        log.info("GRIEVANCE {} created, routed to '{}', SLA deadline {}",
                saved.getId(), saved.getDepartment().getName(), saved.getSlaDeadline());
        return saved;
    }

    public List<Grievance> getAll() {
        return grievanceRepository.findAll();
    }

    public Grievance getById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with id " + id));
    }

    @Transactional
    public Grievance updateStatus(Long id, GrievanceStatus newStatus) {
        Grievance grievance = getById(id);
        GrievanceStatus current = grievance.getStatus();

        if (current == GrievanceStatus.CLOSED) {
            throw new IllegalStateException("Grievance " + id + " is already CLOSED and cannot be changed");
        }
        GrievanceStatus allowedNext = GrievanceStatus.values()[current.ordinal() + 1];
        if (newStatus != allowedNext) {
            throw new IllegalStateException("Invalid status change from " + current + " to " + newStatus
                    + ". Allowed next status is " + allowedNext
                    + " (flow: PENDING -> IN_PROGRESS -> RESOLVED -> CLOSED)");
        }

        grievance.setStatus(newStatus);
        if (newStatus == GrievanceStatus.RESOLVED) {
            grievance.setResolvedAt(LocalDateTime.now());
        }
        Grievance saved = grievanceRepository.save(grievance);
        log.info("STATUS CHANGE: grievance {} {} -> {}", id, current, newStatus);
        return saved;
    }

    @Transactional
    public Grievance rate(Long id, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Invalid rating: value must be between 1 and 5");
        }
        Grievance grievance = getById(id);
        if (grievance.getStatus() != GrievanceStatus.CLOSED) {
            throw new IllegalStateException("Rating is only allowed when the grievance is CLOSED (current status: "
                    + grievance.getStatus() + ")");
        }
        if (grievance.getRating() != null) {
            throw new IllegalStateException("Grievance " + id + " has already been rated");
        }
        grievance.setRating(rating);
        return grievanceRepository.save(grievance);
    }
}
