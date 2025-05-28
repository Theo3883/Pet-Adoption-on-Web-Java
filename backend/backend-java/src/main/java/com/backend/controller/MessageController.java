package com.backend.controller;

import com.backend.service.MessageService;
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
public class MessageController {
    
    private final MessageService messageService;
    private final JwtService jwtService;
    
    @PostMapping("/messages/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long senderId = extractUserIdFromToken(httpRequest);
            if (senderId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            String content = (String) request.get("content");
            
            if (receiverId == null || content == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing required fields");
                return ResponseEntity.badRequest().body(error);
            }
            
            Long messageId = messageService.sendMessage(senderId, receiverId, content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Message sent successfully");
            response.put("messageId", messageId);
            
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/messages/conversation")
    public ResponseEntity<?> getConversation(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            Long otherUserId = Long.valueOf(request.get("otherUserId").toString());
            
            if (otherUserId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing other user ID");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> messages = messageService.getConversation(userId, otherUserId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/messages/conversations")
    public ResponseEntity<?> getConversations(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            List<Map<String, Object>> conversations = messageService.getConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/messages/read")
    public ResponseEntity<?> markMessagesAsRead(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            Long otherUserId = Long.valueOf(request.get("otherUserId").toString());
            
            if (otherUserId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing other user ID");
                return ResponseEntity.badRequest().body(error);
            }
            
            messageService.markAsRead(userId, otherUserId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Messages marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/messages/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            Long count = messageService.getUnreadCount(userId);
            
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
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