package com.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeMessageService {
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<String>> activeUserSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Void> registerUserSessionAsync(Long userId, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Registering session {} for user {}", sessionId, userId);

                activeUserSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(sessionId);
                sessionUserMap.put(sessionId, userId);

                log.info("Session {} registered successfully for user {}", sessionId, userId);
                notifyUserOnlineStatusAsync(userId, true);

            } catch (Exception e) {
                log.error("Error registering session {} for user {}", sessionId, userId, e);
            }
        });
    }

    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Void> unregisterUserSessionAsync(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Long userId = sessionUserMap.remove(sessionId);
                if (userId != null) {
                    log.info("Unregistering session {} for user {}", sessionId, userId);

                    CopyOnWriteArrayList<String> userSessions = activeUserSessions.get(userId);
                    if (userSessions != null) {
                        userSessions.remove(sessionId);

                        if (userSessions.isEmpty()) {
                            activeUserSessions.remove(userId);
                            notifyUserOnlineStatusAsync(userId, false);
                        }
                    }
                    log.info("Session {} unregistered successfully", sessionId);
                }
            } catch (Exception e) {
                log.error("Error unregistering session {}", sessionId, e);
            }
        });
    }

    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Void> sendRealTimeNotificationAsync(Long userId, Object messageData) {
        return CompletableFuture.runAsync(() -> {
            try {
                CopyOnWriteArrayList<String> userSessions = activeUserSessions.get(userId);
                if (userSessions != null && !userSessions.isEmpty()) {
                    log.info("Sending real-time notification to {} sessions for user {}", userSessions.size(), userId);

                    userSessions.parallelStream().forEach(sessionId -> {
                        try {
                            simulateRealTimeUpdate(sessionId, messageData);
                        } catch (Exception e) {
                            log.error("Error sending real-time update to session {}", sessionId, e);
                        }
                    });

                    log.info("Real-time notifications sent to user {}", userId);
                } else {
                    log.debug("No active sessions found for user {}", userId);
                }
            } catch (Exception e) {
                log.error("Error sending real-time notification to user {}", userId, e);
            }
        });
    }

    @Async("generalThreadPoolTaskExecutor")
    public CompletableFuture<Void> notifyUserOnlineStatusAsync(Long userId, boolean isOnline) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("User {} is now {}", userId, isOnline ? "online" : "offline");
                Thread.sleep(50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("User status notification interrupted for user {}", userId);
            } catch (Exception e) {
                log.error("Error notifying user status change for user {}", userId, e);
            }
        });
    }

    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Void> handleTypingIndicatorAsync(Long senderId, Long receiverId, boolean isTyping) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("User {} {} typing to user {}", senderId, isTyping ? "started" : "stopped", receiverId);
                CopyOnWriteArrayList<String> receiverSessions = activeUserSessions.get(receiverId);
                if (receiverSessions != null && !receiverSessions.isEmpty()) {

                    Map<String, Object> typingData = new HashMap<>();
                    typingData.put("senderId", senderId);
                    typingData.put("isTyping", isTyping);
                    typingData.put("type", "typing_indicator");
                    receiverSessions.parallelStream().forEach(sessionId -> {
                        simulateRealTimeUpdate(sessionId, typingData);
                    });
                }

            } catch (Exception e) {
                log.error("Error handling typing indicator from {} to {}", senderId, receiverId, e);
            }
        });
    }

    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Void> broadcastMessageAsync(java.util.List<Long> userIds, Object messageData) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Broadcasting message to {} users", userIds.size());
                userIds.parallelStream().forEach(userId -> {
                    sendRealTimeNotificationAsync(userId, messageData);
                });
                log.info("Message broadcast completed");
            } catch (Exception e) {
                log.error("Error broadcasting message", e);
            }
        });
    }

    public CompletableFuture<Integer> getOnlineUsersCountAsync() {
        return CompletableFuture.supplyAsync(() -> {
            return activeUserSessions.size();
        });
    }

    public CompletableFuture<Boolean> isUserOnlineAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            CopyOnWriteArrayList<String> userSessions = activeUserSessions.get(userId);
            return userSessions != null && !userSessions.isEmpty();
        });
    }

    private void simulateRealTimeUpdate(String sessionId, Object data) {
        try {
            Thread.sleep(10);
            log.debug("Real-time update sent to session {}: {}", sessionId, data.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Async("generalThreadPoolTaskExecutor")
    public CompletableFuture<Void> cleanupInactiveSessionsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting cleanup of inactive sessions");

                // This would typically check session heartbeats or last activity
                // and remove inactive sessions

                // Simulate cleanup process
                Thread.sleep(100);

                log.info("Session cleanup completed");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Session cleanup interrupted");
            } catch (Exception e) {
                log.error("Error during session cleanup", e);
            }
        });
    }
}
