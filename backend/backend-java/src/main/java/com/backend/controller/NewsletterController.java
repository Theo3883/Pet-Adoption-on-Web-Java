package com.backend.controller;

import com.backend.service.NewsletterService;
import com.backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NewsletterController {
    
    private final NewsletterService newsletterService;
    private final JwtService jwtService;
    
    @GetMapping("/newsletter/subscriptions")
    public ResponseEntity<?> getSubscriptions(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            List<Map<String, Object>> subscriptions = newsletterService.getSubscriptions(userId);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/newsletter/update")
    public ResponseEntity<?> updateSubscriptions(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            @SuppressWarnings("unchecked")
            List<String> species = (List<String>) request.get("species");
            
            newsletterService.updateSubscriptions(userId, species);
            
            Map<String, Boolean> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private Long extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtService.extractUserId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
} 