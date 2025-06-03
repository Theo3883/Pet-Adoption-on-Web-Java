package com.backend.controller;

import com.backend.dto.UserLoginRequest;
import com.backend.dto.UserResponse;
import com.backend.dto.UserSignupRequest;
import com.backend.exception.AuthorizationException;
import com.backend.exception.ValidationException;
import com.backend.service.JwtService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Map<String, Object>> signUp(@Valid @RequestBody UserSignupRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("firstName");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("lastName");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("email");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("password");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("phone");
        }
        if (request.getAddress() == null) {
            throw ValidationException.missingRequiredField("address");
        }

        userService.createUser(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User and address created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }    
    
    @PostMapping("/users/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("email");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw ValidationException.missingRequiredField("password");
        }

        String token = userService.authenticateUser(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentication successful");
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/all/details")
    public ResponseEntity<List<UserResponse>> getAllUsersWithDetails(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw AuthorizationException.adminRequired();
        }

        String token = authHeader.substring(7);
        Boolean isAdmin = jwtService.extractIsAdmin(token);
        if (isAdmin == null || !isAdmin) {
            throw AuthorizationException.adminRequired();
        }

        List<UserResponse> users = userService.getAllUsersWithDetails();
        return ResponseEntity.ok(users);
    }    
    
    @DeleteMapping("/users/delete")
    public ResponseEntity<Map<String, String>> deleteUser(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw AuthorizationException.adminRequired();
        }

        String token = authHeader.substring(7);
        Boolean isAdmin = jwtService.extractIsAdmin(token);
        if (isAdmin == null || !isAdmin) {
            throw AuthorizationException.adminRequired();
        }

        if (!request.containsKey("userId")) {
            throw ValidationException.missingRequiredField("userId");
        }

        Long userId = Long.valueOf(request.get("userId").toString());
        userService.deleteUser(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User and all related data successfully deleted");
        return ResponseEntity.ok(response);
    }
}