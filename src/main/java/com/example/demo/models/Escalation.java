package com.example.demo.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "escalations", uniqueConstraints = @UniqueConstraint(columnNames = "grievance_id"))
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grievance_id", nullable = false)
    private Grievance grievance;

    @Column(nullable = false)
    private String escalatedToOfficer;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime escalatedAt;

    public Escalation() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Grievance getGrievance() { return grievance; }
    public void setGrievance(Grievance grievance) { this.grievance = grievance; }
    public String getEscalatedToOfficer() { return escalatedToOfficer; }
    public void setEscalatedToOfficer(String escalatedToOfficer) { this.escalatedToOfficer = escalatedToOfficer; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
}
