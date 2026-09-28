package com.example.demo.controllers;

import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceStatus;
import com.example.demo.services.GrievanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grievances")
public class GrievanceController {

    private final GrievanceService grievanceService;

    public GrievanceController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    // Small request bodies for the two PUT endpoints
    public static class StatusUpdateRequest {
        @NotNull(message = "status is required")
        private GrievanceStatus status;

        public GrievanceStatus getStatus() { return status; }
        public void setStatus(GrievanceStatus status) { this.status = status; }
    }

    public static class RatingRequest {
        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        private Integer rating;

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }

    @PostMapping
    public ResponseEntity<Grievance> create(@Valid @RequestBody Grievance grievance) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grievanceService.create(grievance));
    }

    @GetMapping
    public ResponseEntity<List<Grievance>> getAll() {
        return ResponseEntity.ok(grievanceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grievance> getById(@PathVariable Long id) {
        return ResponseEntity.ok(grievanceService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Grievance> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(grievanceService.updateStatus(id, request.getStatus()));
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Grievance> rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(grievanceService.rate(id, request.getRating()));
    }
}
