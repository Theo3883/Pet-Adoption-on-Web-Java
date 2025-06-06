package com.backend.controller;

import com.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final AdminService adminService;
    
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            
            if (email == null || password == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing email or password");
                return ResponseEntity.badRequest().body(error);
            }
            
            String token = adminService.authenticateAdmin(email, password);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Admin authentication successful");
            response.put("token", token);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }
} 