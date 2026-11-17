package com.rehan.jobtracker.controller;

import com.rehan.jobtracker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * GET /api/admin/summary - Administrator overview endpoint restricted to users with ROLE_ADMIN.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAdminSummary() {
        long totalUsers = userRepository.count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "systemStatus", "OPERATIONAL"
        ));
    }
}
