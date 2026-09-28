package com.example.demo.services;

import com.example.demo.models.Category;
import com.example.demo.models.Department;
import com.example.demo.models.Grievance;
import com.example.demo.models.GrievanceSeverity;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.DepartmentRepository;
import com.example.demo.repositories.GrievanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final GrievanceRepository grievanceRepository;

    public DemoDataInitializer(CategoryRepository categoryRepository, DepartmentRepository departmentRepository,
                               GrievanceRepository grievanceRepository) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.grievanceRepository = grievanceRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            Department publicWorks = department("Public Works", "Roads, drainage, and streetlights");
            Department waterServices = department("Water Services", "Water supply and related issues");
            Department sanitation = department("Sanitation", "Garbage collection and public cleanliness");

            category("Road damage", publicWorks);
            category("Drainage blockage", publicWorks);
            category("Streetlight outage", publicWorks);
            category("Water supply", waterServices);
            category("Garbage collection", sanitation);
        }

        var categories = categoryRepository.findAll();
        for (Category category : categories) {
            int legacyDays = category.getSlaDays() == null ? 7 : category.getSlaDays();
            if (category.getSlaDaysLow() == null) category.setSlaDaysLow(legacyDays);
            if (category.getSlaDaysMedium() == null) category.setSlaDaysMedium(legacyDays);
            if (category.getSlaDaysHigh() == null) category.setSlaDaysHigh(legacyDays);
            if (category.getSlaDaysCritical() == null) category.setSlaDaysCritical(legacyDays);
            category.setSlaDays(category.getSlaDaysLow());
        }
        categoryRepository.saveAll(categories);

        var grievances = grievanceRepository.findAll();
        for (Grievance grievance : grievances) {
            if (grievance.getSeverity() == null) grievance.setSeverity(GrievanceSeverity.MEDIUM);
            if (grievance.getSlaDurationDays() == null) {
                grievance.setSlaDurationDays(grievance.getCategory().getSlaDaysForSeverity(grievance.getSeverity()));
            }
        }
        grievanceRepository.saveAll(grievances);
    }

    private Department department(String name, String description) {
        return departmentRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Department department = new Department();
            department.setName(name);
            department.setDescription(description);
            return departmentRepository.save(department);
        });
    }

    private void category(String name, Department department) {
        Category category = new Category();
        category.setName(name);
        category.setSlaDays(4);
        category.setSlaDaysLow(4);
        category.setSlaDaysMedium(3);
        category.setSlaDaysHigh(2);
        category.setSlaDaysCritical(1);
        category.setDepartment(department);
        categoryRepository.save(category);
    }
}