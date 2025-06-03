package com.backend.controller;

import com.backend.exception.AuthenticationException;
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
    public ResponseEntity<List<Map<String, Object>>> getSubscriptions(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        List<Map<String, Object>> subscriptions = newsletterService.getSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/newsletter/update")
    public ResponseEntity<Map<String, Boolean>> updateSubscriptions(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        @SuppressWarnings("unchecked")
        List<String> species = (List<String>) request.get("species");

        newsletterService.updateSubscriptions(userId, species);

        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
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