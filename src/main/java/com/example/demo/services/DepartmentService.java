package com.example.demo.services;

import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.models.Department;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.DepartmentRepository;
import com.example.demo.repositories.GrievanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final GrievanceRepository grievanceRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             CategoryRepository categoryRepository,
                             GrievanceRepository grievanceRepository) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.grievanceRepository = grievanceRepository;
    }

    public Department create(Department request) {
        checkNameFree(request.getName(), null);
        Department department = new Department();
        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        return departmentRepository.save(department);
    }

    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    public Department getById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    public Department update(Long id, Department request) {
        Department existing = getById(id);
        checkNameFree(request.getName(), id);
        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription());
        return departmentRepository.save(existing);
    }

    public void delete(Long id) {
        Department existing = getById(id);
        if (categoryRepository.existsByDepartmentId(id) || grievanceRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException(
                    "Cannot delete department '" + existing.getName() + "': it still has categories or grievances");
        }
        departmentRepository.delete(existing);
    }

    private void checkNameFree(String name, Long ownId) {
        departmentRepository.findByNameIgnoreCase(name.trim()).ifPresent(other -> {
            if (ownId == null || !other.getId().equals(ownId)) {
                throw new IllegalStateException("A department named '" + name.trim() + "' already exists");
            }
        });
    }
}
