package com.example.demo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer slaDays;

    @Positive(message = "slaDaysLow must be greater than 0")
    private Integer slaDaysLow;

    @Positive(message = "slaDaysMedium must be greater than 0")
    private Integer slaDaysMedium;

    @Positive(message = "slaDaysHigh must be greater than 0")
    private Integer slaDaysHigh;

    @Positive(message = "slaDaysCritical must be greater than 0")
    private Integer slaDaysCritical;

    // Client sends: "department": { "id": 1 }
    @NotNull(message = "department is required (send {\"id\": <departmentId>})")
    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<Grievance> grievances = new ArrayList<>();

    public Category() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSlaDays() { return slaDaysLow != null ? slaDaysLow : slaDays; }
    public void setSlaDays(Integer slaDays) { this.slaDays = slaDays; }
    public Integer getSlaDaysLow() { return slaDaysLow; }
    public void setSlaDaysLow(Integer slaDaysLow) { this.slaDaysLow = slaDaysLow; }
    public Integer getSlaDaysMedium() { return slaDaysMedium; }
    public void setSlaDaysMedium(Integer slaDaysMedium) { this.slaDaysMedium = slaDaysMedium; }
    public Integer getSlaDaysHigh() { return slaDaysHigh; }
    public void setSlaDaysHigh(Integer slaDaysHigh) { this.slaDaysHigh = slaDaysHigh; }
    public Integer getSlaDaysCritical() { return slaDaysCritical; }
    public void setSlaDaysCritical(Integer slaDaysCritical) { this.slaDaysCritical = slaDaysCritical; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public List<Grievance> getGrievances() { return grievances; }

    public int getSlaDaysForSeverity(GrievanceSeverity severity) {
        Integer days = switch (severity == null ? GrievanceSeverity.MEDIUM : severity) {
            case LOW -> slaDaysLow;
            case MEDIUM -> slaDaysMedium;
            case HIGH -> slaDaysHigh;
            case CRITICAL -> slaDaysCritical;
        };
        return days != null ? days : slaDays != null ? slaDays : 7;
    }
}
