package com.backend.controller;

import com.backend.dto.UserLoginRequest;
import com.backend.dto.UserResponse;
import com.backend.dto.UserSignupRequest;
import com.backend.service.JwtService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    @PostMapping("/users/signup")
    public ResponseEntity<?> signUp(@RequestBody UserSignupRequest request) {
        try {
            // Check for missing required fields like in Node.js
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty() ||
                request.getLastName() == null || request.getLastName().trim().isEmpty() ||
                request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty() ||
                request.getPhone() == null || request.getPhone().trim().isEmpty() ||
                request.getAddress() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing required fields");
                return ResponseEntity.status(400).body(error);
            }
            
            UserResponse user = userService.createUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User and address created successfully");
            
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            if (e.getMessage().contains("Email already exists")) {
                error.put("error", "Email already exists");
            } else {
                error.put("error", "Internal Server Error");
            }
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            // Check if email and password are provided
            if (request.getEmail() == null || request.getEmail().trim().isEmpty() || 
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing email or password");
                return ResponseEntity.status(400).body(error);
            }
            
            String token = userService.authenticateUser(request);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Authentication successful");
            response.put("token", token);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            if (e.getMessage().contains("Invalid credentials")) {
                error.put("error", "Email or password wrong");
                return ResponseEntity.status(404).body(error);
            } else {
                error.put("error", "Internal Server Error");
                return ResponseEntity.status(500).body(error);
            }
        }
    }
    
    @GetMapping("/users/all/details")
    public ResponseEntity<?> getAllUsersWithDetails(HttpServletRequest httpRequest) {
        try {
            // Check if user is authenticated and is admin
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            String token = authHeader.substring(7);
            Boolean isAdmin = jwtService.extractIsAdmin(token);
            if (isAdmin == null || !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            List<UserResponse> users = userService.getAllUsersWithDetails();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/users/delete")
    public ResponseEntity<?> deleteUser(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            // Check if user is authenticated and is admin
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            String token = authHeader.substring(7);
            Boolean isAdmin = jwtService.extractIsAdmin(token);
            if (isAdmin == null || !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            if (!request.containsKey("userId")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User ID is required");
                return ResponseEntity.status(400).body(error);
            }
            
            Long userId = Long.valueOf(request.get("userId").toString());
            userService.deleteUser(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User and all related data successfully deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            if (e.getMessage().contains("User not found")) {
                error.put("error", "User not found");
                return ResponseEntity.status(404).body(error);
            } else {
                error.put("error", "Internal Server Error");
                return ResponseEntity.status(500).body(error);
            }
        }
    }
} 