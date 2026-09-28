package com.example.demo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grievances")
public class Grievance {

    // READ_ONLY = the client can never set these; the server controls them.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Citizen name is required")
    @Size(max = 100, message = "Citizen name must be at most 100 characters")
    @Column(nullable = false, length = 100)
    private String citizenName;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Column(nullable = false, length = 1000)
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must be at most 255 characters")
    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column
    private GrievanceSeverity severity = GrievanceSeverity.MEDIUM;

    @Column(name = "sla_duration_days")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer slaDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private GrievanceStatus status = GrievanceStatus.PENDING;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime slaDeadline;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime resolvedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer rating;

    // Client sends: "category": { "id": 1 }
    @NotNull(message = "category is required (send {\"id\": <categoryId>})")
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Always copied from category.department by the service. Never accepted from the client.
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Department department;

    @JsonIgnore
    @OneToMany(mappedBy = "grievance")
    private List<Escalation> escalations = new ArrayList<>();

    public Grievance() {
    }

    /** SLA shown in DAYS in every API response (independent of demo mode). */
    @JsonProperty(value = "slaDays", access = JsonProperty.Access.READ_ONLY)
    public Integer getSlaDays() {
        return slaDurationDays != null ? slaDurationDays
            : category != null ? category.getSlaDaysForSeverity(severity) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public GrievanceSeverity getSeverity() { return severity; }
    public void setSeverity(GrievanceSeverity severity) { this.severity = severity; }
    public Integer getSlaDurationDays() { return slaDurationDays; }
    public void setSlaDurationDays(Integer slaDurationDays) { this.slaDurationDays = slaDurationDays; }
    public GrievanceStatus getStatus() { return status; }
    public void setStatus(GrievanceStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public List<Escalation> getEscalations() { return escalations; }
}
