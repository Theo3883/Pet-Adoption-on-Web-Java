package com.backend.controller;

import com.backend.exception.ValidationException;
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
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        if (email == null || password == null) {
            throw ValidationException.missingRequiredField("email or password");
        }
        
        String token = adminService.authenticateAdmin(email, password);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin authentication successful");
        response.put("token", token);
        
        return ResponseEntity.ok(response);
    }
} 