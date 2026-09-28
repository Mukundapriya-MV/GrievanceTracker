package com.example.demo.controllers;

import com.example.demo.models.Escalation;
import com.example.demo.services.EscalationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @GetMapping
    public ResponseEntity<List<Escalation>> getAll() {
        return ResponseEntity.ok(escalationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Escalation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(escalationService.getById(id));
    }
}
