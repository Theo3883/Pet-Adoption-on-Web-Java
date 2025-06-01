package com.backend.controller;

import com.backend.service.MessageService;
import com.backend.service.JwtService;
import com.backend.service.RealTimeMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final JwtService jwtService;
    private final RealTimeMessageService realTimeMessageService;

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

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "new_message");
            messageData.put("senderId", senderId);
            messageData.put("messageId", messageId);
            messageData.put("content", content);

            realTimeMessageService.sendRealTimeNotificationAsync(receiverId, messageData);

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
            log.error("Error getting conversation", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/messages/conversation/async")
    public ResponseEntity<?> getConversationAsync(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
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

            CompletableFuture<List<Map<String, Object>>> messagesFuture = messageService.getConversationAsync(userId,
                    otherUserId);

            messageService.markAsReadAsync(userId, otherUserId);

            List<Map<String, Object>> messages = messagesFuture.get();
            return ResponseEntity.ok(messages);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting conversation async", e);
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
            log.error("Error getting conversations for user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/messages/conversations/async")
    public ResponseEntity<?> getConversationsAsync(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            CompletableFuture<List<Map<String, Object>>> conversationsFuture = messageService
                    .getConversationsAsync(userId);

            CompletableFuture<Long> unreadCountFuture = messageService.getUnreadCountAsync(userId);

            CompletableFuture<Void> allOperations = CompletableFuture.allOf(
                    conversationsFuture, unreadCountFuture);

            allOperations.join();

            List<Map<String, Object>> conversations = conversationsFuture.get();
            Long unreadCount = unreadCountFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("conversations", conversations);
            response.put("totalUnreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting conversations async for user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/messages/read")
    public ResponseEntity<?> markMessagesAsRead(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
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

            messageService.markAsReadAsync(userId, otherUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Messages marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
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

            CompletableFuture<Long> countFuture = messageService.getUnreadCountAsync(userId);
            Long count = countFuture.get();

            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting unread count", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/messages/dashboard")
    public ResponseEntity<?> getMessagesDashboard(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            CompletableFuture<List<Map<String, Object>>> conversationsFuture = messageService
                    .getConversationsAsync(userId);

            CompletableFuture<Long> unreadCountFuture = messageService.getUnreadCountAsync(userId);

            CompletableFuture<Void> allOperations = CompletableFuture.allOf(
                    conversationsFuture, unreadCountFuture);

            allOperations.join();

            List<Map<String, Object>> conversations = conversationsFuture.get();
            Long unreadCount = unreadCountFuture.get();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("conversations", conversations);
            dashboard.put("totalUnreadCount", unreadCount);
            dashboard.put("totalConversations", conversations.size());
            dashboard.put("hasUnreadMessages", unreadCount > 0);

            return ResponseEntity.ok(dashboard);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting messages dashboard", e);
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

    @PostMapping("/messages/session/register")
    public ResponseEntity<?> registerSession(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            String sessionId = (String) request.get("sessionId");
            if (sessionId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Session ID is required");
                return ResponseEntity.badRequest().body(error);
            }

            realTimeMessageService.registerUserSessionAsync(userId, sessionId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Session registered successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error registering session", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/messages/typing")
    public ResponseEntity<?> handleTyping(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long senderId = extractUserIdFromToken(httpRequest);
            if (senderId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            Boolean isTyping = (Boolean) request.get("isTyping");

            if (receiverId == null || isTyping == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing required fields");
                return ResponseEntity.badRequest().body(error);
            }

            realTimeMessageService.handleTypingIndicatorAsync(senderId, receiverId, isTyping);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Typing indicator processed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/messages/online-status/{userId}")
    public ResponseEntity<?> getOnlineStatus(@PathVariable Long userId, HttpServletRequest httpRequest) {
        try {
            Long requesterId = extractUserIdFromToken(httpRequest);
            if (requesterId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            CompletableFuture<Boolean> onlineFuture = realTimeMessageService.isUserOnlineAsync(userId);
            Boolean isOnline = onlineFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("isOnline", isOnline);
            return ResponseEntity.ok(response);

        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting online status", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/messages/online-count")
    public ResponseEntity<?> getOnlineUsersCount(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User authentication required");
                return ResponseEntity.status(401).body(error);
            }

            CompletableFuture<Integer> countFuture = realTimeMessageService.getOnlineUsersCountAsync();
            Integer count = countFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("onlineCount", count);
            return ResponseEntity.ok(response);

        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting online users count", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
    }
}