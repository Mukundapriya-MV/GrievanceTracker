package com.example.demo.services;

import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.models.Category;
import com.example.demo.models.Department;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.DepartmentRepository;
import com.example.demo.repositories.GrievanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final GrievanceRepository grievanceRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           DepartmentRepository departmentRepository,
                           GrievanceRepository grievanceRepository) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.grievanceRepository = grievanceRepository;
    }

    public Category create(Category request) {
        Department department = resolveDepartment(request);
        checkNameFree(request.getName(), null);
        Category category = new Category();
        category.setName(request.getName().trim());
        copySlaConfiguration(request, category);
        category.setDepartment(department);
        return categoryRepository.save(category);
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }

    public Category update(Long id, Category request) {
        Category existing = getById(id);
        Department department = resolveDepartment(request);
        checkNameFree(request.getName(), id);
        existing.setName(request.getName().trim());
        copySlaConfiguration(request, existing);
        existing.setDepartment(department);
        return categoryRepository.save(existing);
    }

    public void delete(Long id) {
        Category existing = getById(id);
        if (grievanceRepository.existsByCategoryId(id)) {
            throw new IllegalStateException(
                    "Cannot delete category '" + existing.getName() + "': grievances already use it");
        }
        categoryRepository.delete(existing);
    }

    private Department resolveDepartment(Category request) {
        if (request.getDepartment() == null) {
            throw new IllegalArgumentException("department is required");
        }
        Long departmentId = request.getDepartment().getId();
        if (departmentId == null) {
            throw new IllegalArgumentException("Invalid department: department.id is required");
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid department: no department exists with id " + departmentId));
    }

    private void copySlaConfiguration(Category source, Category target) {
        boolean legacyRequest = source.getSlaDaysLow() == null && source.getSlaDaysMedium() == null
                && source.getSlaDaysHigh() == null && source.getSlaDaysCritical() == null;
        int low;
        int medium;
        int high;
        int critical;
        if (legacyRequest) {
            int legacyDays = configuredDays(source.getSlaDays(), "SLA");
            low = legacyDays;
            medium = legacyDays;
            high = legacyDays;
            critical = legacyDays;
        } else {
            low = configuredDays(source.getSlaDaysLow(), "LOW");
            medium = configuredDays(source.getSlaDaysMedium(), "MEDIUM");
            high = configuredDays(source.getSlaDaysHigh(), "HIGH");
            critical = configuredDays(source.getSlaDaysCritical(), "CRITICAL");
        }
        target.setSlaDays(low);
        target.setSlaDaysLow(low);
        target.setSlaDaysMedium(medium);
        target.setSlaDaysHigh(high);
        target.setSlaDaysCritical(critical);
    }

    private int configuredDays(Integer configured, String severity) {
        if (configured == null) {
            throw new IllegalArgumentException("SLA duration is required for " + severity);
        }
        int days = configured;
        if (days <= 0) {
            throw new IllegalArgumentException("A positive SLA duration is required for " + severity);
        }
        return days;
    }

    private void checkNameFree(String name, Long ownId) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(other -> {
            if (ownId == null || !other.getId().equals(ownId)) {
                throw new IllegalStateException("A category named '" + name.trim() + "' already exists");
            }
        });
    }
}
