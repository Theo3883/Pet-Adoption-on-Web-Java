package com.backend.controller;

import com.backend.exception.AuthenticationException;
import com.backend.exception.ValidationException;
import com.backend.exception.ServiceException;
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
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long senderId = extractUserIdFromToken(httpRequest);
        if (senderId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("receiverId") || !request.containsKey("content")) {
            throw ValidationException.missingRequiredField("receiverId or content");
        }

        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        String content = (String) request.get("content");

        if (content == null) {
            throw ValidationException.missingRequiredField("receiverId or content");
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
    }

    @PostMapping("/messages/conversation")
    public ResponseEntity<List<Map<String, Object>>> getConversation(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("otherUserId")) {
            throw ValidationException.missingRequiredField("otherUserId");
        }

        Long otherUserId = Long.valueOf(request.get("otherUserId").toString());

        List<Map<String, Object>> messages = messageService.getConversation(userId, otherUserId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages/conversation/async")
    public ResponseEntity<List<Map<String, Object>>> getConversationAsync(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("otherUserId")) {
            throw ValidationException.missingRequiredField("otherUserId");
        }

        Long otherUserId = Long.valueOf(request.get("otherUserId").toString());

        if (otherUserId == null) {
            throw ValidationException.missingRequiredField("otherUserId");
        }

        try {
            CompletableFuture<List<Map<String, Object>>> messagesFuture = messageService.getConversationAsync(userId,
                    otherUserId);

            messageService.markAsReadAsync(userId, otherUserId);

            List<Map<String, Object>> messages = messagesFuture.get();
            return ResponseEntity.ok(messages);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting conversation async", e);
            throw ServiceException.externalServiceFailure("MessageService",
                    "Failed to retrieve conversation asynchronously");
        }
    }

    @GetMapping("/messages/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        List<Map<String, Object>> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/messages/conversations/async")
    public ResponseEntity<Map<String, Object>> getConversationsAsync(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        try {
            CompletableFuture<List<Map<String, Object>>> conversationsFuture = messageService
                    .getConversationsAsync(userId);

            CompletableFuture<Long> unreadCountFuture = messageService.getUnreadCountAsync(userId);

            CompletableFuture<Void> allOperations = CompletableFuture.allOf(conversationsFuture, unreadCountFuture);
            allOperations.join();

            List<Map<String, Object>> conversations = conversationsFuture.get();
            Long unreadCount = unreadCountFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("conversations", conversations);
            response.put("totalUnreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting conversations async for user", e);
            throw ServiceException.externalServiceFailure("MessageService",
                    "Failed to retrieve conversations asynchronously");
        }
    }

    @PostMapping("/messages/read")
    public ResponseEntity<Map<String, String>> markMessagesAsRead(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("otherUserId")) {
            throw ValidationException.missingRequiredField("otherUserId");
        }

        Long otherUserId = Long.valueOf(request.get("otherUserId").toString());

        messageService.markAsReadAsync(userId, otherUserId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Messages marked as read");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        try {
            CompletableFuture<Long> countFuture = messageService.getUnreadCountAsync(userId);
            Long count = countFuture.get();

            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting unread count", e);
            throw ServiceException.externalServiceFailure("MessageService", "Failed to retrieve unread count");
        }
    }

    @GetMapping("/messages/dashboard")
    public ResponseEntity<Map<String, Object>> getMessagesDashboard(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        try {
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
            throw ServiceException.externalServiceFailure("MessageService", "Failed to retrieve messages dashboard");
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
    public ResponseEntity<Map<String, String>> registerSession(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("sessionId")) {
            throw ValidationException.missingRequiredField("sessionId");
        }

        String sessionId = (String) request.get("sessionId");
        if (sessionId == null) {
            throw ValidationException.missingRequiredField("sessionId");
        }

        realTimeMessageService.registerUserSessionAsync(userId, sessionId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Session registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages/session/unregister")
    public ResponseEntity<Map<String, String>> unregisterSession(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("sessionId")) {
            throw ValidationException.missingRequiredField("sessionId");
        }

        String sessionId = (String) request.get("sessionId");
        if (sessionId == null) {
            throw ValidationException.missingRequiredField("sessionId");
        }

        realTimeMessageService.unregisterUserSessionAsync(sessionId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Session unregistered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages/typing")
    public ResponseEntity<Map<String, String>> handleTyping(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long senderId = extractUserIdFromToken(httpRequest);
        if (senderId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (!request.containsKey("receiverId") || !request.containsKey("isTyping")) {
            throw ValidationException.missingRequiredField("receiverId or isTyping");
        }

        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        Boolean isTyping = (Boolean) request.get("isTyping");

        if (isTyping == null) {
            throw ValidationException.missingRequiredField("receiverId or isTyping");
        }

        realTimeMessageService.handleTypingIndicatorAsync(senderId, receiverId, isTyping);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Typing indicator processed");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages/online-status/{userId}")
    public ResponseEntity<Map<String, Object>> getOnlineStatus(@PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long requesterId = extractUserIdFromToken(httpRequest);
        if (requesterId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        if (userId == null) {
            throw ValidationException.missingRequiredField("userId");
        }

        try {
            realTimeMessageService.cleanupInactiveSessionsAsync().get();

            CompletableFuture<Boolean> onlineFuture = realTimeMessageService.isUserOnlineAsync(userId);
            Boolean isOnline = onlineFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("isOnline", isOnline);
            log.debug("User {} online status requested by {}: {}", userId, requesterId, isOnline);
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting online status", e);
            throw ServiceException.externalServiceFailure("RealTimeMessageService", "Failed to check online status");
        }
    }

    @GetMapping("/messages/online-count")
    public ResponseEntity<Map<String, Object>> getOnlineUsersCount(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        try {
            CompletableFuture<Integer> countFuture = realTimeMessageService.getOnlineUsersCountAsync();
            Integer count = countFuture.get();
            Map<String, Object> response = new HashMap<>();
            response.put("onlineCount", count);
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error getting online users count", e);
            throw ServiceException.externalServiceFailure("RealTimeMessageService", "Failed to get online users count");
        }
    }

    @PostMapping("/messages/force-offline")
    public ResponseEntity<Map<String, Object>> forceUserOffline(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw AuthenticationException.authenticationRequired();
        }

        log.info("Force offline request received for user: {}", userId);

        realTimeMessageService.notifyUserOnlineStatusAsync(userId, false).join();
        realTimeMessageService.cleanupInactiveSessionsAsync().join();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User set to offline");
        return ResponseEntity.ok(response);
    }
}