package com.backend.service;

import com.backend.exception.ResourceNotFoundException;
import com.backend.model.Message;
import com.backend.model.User;
import com.backend.repository.MessageRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Transactional
@EnableAsync
@Slf4j
public class MessageService {
      private final MessageRepository messageRepository;
    private final UserRepository userRepository;
      public Long sendMessage(Long senderId, Long receiverId, String content) {
        Optional<User> sender = userRepository.findById(senderId);
        Optional<User> receiver = userRepository.findById(receiverId);
        
        if (sender.isEmpty()) {
            throw ResourceNotFoundException.userNotFound(senderId);
        }
        if (receiver.isEmpty()) {
            throw ResourceNotFoundException.userNotFound(receiverId);
        }
        
        Message message = new Message();
        message.setSender(sender.get());
        message.setReceiver(receiver.get());
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);
        
        Message savedMessage = messageRepository.save(message);
        processMessageAsync(savedMessage.getMessageId(), senderId, receiverId);
        
        return savedMessage.getMessageId();
    }
    @Async("messageThreadPoolTaskExecutor")
    public void processMessageAsync(Long messageId, Long senderId, Long receiverId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Message processing completed for message {}", messageId);

            } catch (Exception e) {
                log.error("Error processing message {} asynchronously", messageId, e);
            }
        });
    }
    
    public CompletableFuture<List<Map<String, Object>>> getConversationAsync(Long userId, Long otherUserId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Message> messages = messageRepository.findConversation(userId, otherUserId);
                
                return messages.parallelStream().map(message -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("MESSAGEID", message.getMessageId());
                    messageMap.put("SENDERID", message.getSender().getUserId());
                    messageMap.put("RECEIVERID", message.getReceiver().getUserId());
                    messageMap.put("CONTENT", message.getContent());
                    messageMap.put("TIMESTAMP", message.getTimestamp());
                    messageMap.put("ISREAD", message.getIsRead() ? 1 : 0);
                    
                    messageMap.put("SENDERFIRSTNAME", message.getSender().getFirstName());
                    messageMap.put("SENDERLASTNAME", message.getSender().getLastName());
                    messageMap.put("RECEIVERFIRSTNAME", message.getReceiver().getFirstName());
                    messageMap.put("RECEIVERLASTNAME", message.getReceiver().getLastName());
                    
                    return messageMap;
                }).toList();
            } catch (Exception e) {
                log.error("Error loading conversation for users {} and {}", userId, otherUserId, e);
                throw new CompletionException(e);
            }
        });
    }
    
    public List<Map<String, Object>> getConversation(Long userId, Long otherUserId) {
        List<Message> messages = messageRepository.findConversation(userId, otherUserId);
        
        return messages.stream().map(message -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("MESSAGEID", message.getMessageId());
            messageMap.put("SENDERID", message.getSender().getUserId());
            messageMap.put("RECEIVERID", message.getReceiver().getUserId());
            messageMap.put("CONTENT", message.getContent());
            messageMap.put("TIMESTAMP", message.getTimestamp());
            messageMap.put("ISREAD", message.getIsRead() ? 1 : 0);
            
            messageMap.put("SENDERFIRSTNAME", message.getSender().getFirstName());
            messageMap.put("SENDERLASTNAME", message.getSender().getLastName());
            messageMap.put("RECEIVERFIRSTNAME", message.getReceiver().getFirstName());
            messageMap.put("RECEIVERLASTNAME", message.getReceiver().getLastName());
            
            return messageMap;
        }).toList();
    }
    
    public CompletableFuture<List<Map<String, Object>>> getConversationsAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CompletableFuture<List<Object[]>> senderFuture = CompletableFuture.supplyAsync(() -> 
                    messageRepository.findConversationsAsSender(userId));
                CompletableFuture<List<Object[]>> receiverFuture = CompletableFuture.supplyAsync(() -> 
                    messageRepository.findConversationsAsReceiver(userId));

                CompletableFuture.allOf(senderFuture, receiverFuture).join();
                
                List<Object[]> senderResults = senderFuture.join();
                List<Object[]> receiverResults = receiverFuture.join();
                
                Map<Long, Map<String, Object>> conversationMap = new HashMap<>();
                
                senderResults.parallelStream().forEach(result -> {
                    User otherUser = (User) result[0];
                    LocalDateTime lastMessageTime = (LocalDateTime) result[1];
                    
                    synchronized (conversationMap) {
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("OTHERUSERID", otherUser.getUserId());
                        conversation.put("OTHERUSERNAME", otherUser.getFirstName() + " " + otherUser.getLastName());
                        conversation.put("FIRSTNAME", otherUser.getFirstName());
                        conversation.put("LASTNAME", otherUser.getLastName());
                        conversation.put("EMAIL", otherUser.getEmail());
                        conversation.put("LASTMESSAGETIME", lastMessageTime);
                        
                        conversationMap.put(otherUser.getUserId(), conversation);
                    }
                });
                
                receiverResults.parallelStream().forEach(result -> {
                    User otherUser = (User) result[0];
                    LocalDateTime lastMessageTime = (LocalDateTime) result[1];
                    
                    synchronized (conversationMap) {
                        if (conversationMap.containsKey(otherUser.getUserId())) {
                            Map<String, Object> existing = conversationMap.get(otherUser.getUserId());
                            LocalDateTime existingTime = (LocalDateTime) existing.get("LASTMESSAGETIME");
                            if (lastMessageTime.isAfter(existingTime)) {
                                existing.put("LASTMESSAGETIME", lastMessageTime);
                            }
                        } else {
                            Map<String, Object> conversation = new HashMap<>();
                            conversation.put("OTHERUSERID", otherUser.getUserId());
                            conversation.put("OTHERUSERNAME", otherUser.getFirstName() + " " + otherUser.getLastName());
                            conversation.put("FIRSTNAME", otherUser.getFirstName());
                            conversation.put("LASTNAME", otherUser.getLastName());
                            conversation.put("EMAIL", otherUser.getEmail());
                            conversation.put("LASTMESSAGETIME", lastMessageTime);
                            
                            conversationMap.put(otherUser.getUserId(), conversation);
                        }
                    }
                });
                
                return conversationMap.values().stream()
                        .sorted((a, b) -> ((LocalDateTime) b.get("LASTMESSAGETIME"))
                                .compareTo((LocalDateTime) a.get("LASTMESSAGETIME")))
                                .toList();
                        
            } catch (Exception e) {
                log.error("Error loading conversations for user {}", userId, e);
                throw new CompletionException(e);
            }
        });
    }
    
    public List<Map<String, Object>> getConversations(Long userId) {
        List<Object[]> senderResults = messageRepository.findConversationsAsSender(userId);
        List<Object[]> receiverResults = messageRepository.findConversationsAsReceiver(userId);
    
        Map<Long, Map<String, Object>> conversationMap = new HashMap<>();
        
        for (Object[] result : senderResults) {
            User otherUser = (User) result[0];
            LocalDateTime lastMessageTime = (LocalDateTime) result[1];
            
            Map<String, Object> conversation = new HashMap<>();
            conversation.put("OTHERUSERID", otherUser.getUserId());
            conversation.put("OTHERUSERNAME", otherUser.getFirstName() + " " + otherUser.getLastName());
            conversation.put("FIRSTNAME", otherUser.getFirstName());
            conversation.put("LASTNAME", otherUser.getLastName());
            conversation.put("EMAIL", otherUser.getEmail());
            conversation.put("LASTMESSAGETIME", lastMessageTime);
            
            conversationMap.put(otherUser.getUserId(), conversation);
        }
        
        for (Object[] result : receiverResults) {
            User otherUser = (User) result[0];
            LocalDateTime lastMessageTime = (LocalDateTime) result[1];
            
            if (conversationMap.containsKey(otherUser.getUserId())) {
                Map<String, Object> existing = conversationMap.get(otherUser.getUserId());
                LocalDateTime existingTime = (LocalDateTime) existing.get("LASTMESSAGETIME");
                if (lastMessageTime.isAfter(existingTime)) {
                    existing.put("LASTMESSAGETIME", lastMessageTime);
                }
            } else {
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("OTHERUSERID", otherUser.getUserId());
                conversation.put("OTHERUSERNAME", otherUser.getFirstName() + " " + otherUser.getLastName());
                conversation.put("FIRSTNAME", otherUser.getFirstName());
                conversation.put("LASTNAME", otherUser.getLastName());
                conversation.put("EMAIL", otherUser.getEmail());
                conversation.put("LASTMESSAGETIME", lastMessageTime);
                
                conversationMap.put(otherUser.getUserId(), conversation);
            }
        }
        
        return conversationMap.values().stream()
                .sorted((a, b) -> ((LocalDateTime) b.get("LASTMESSAGETIME"))
                        .compareTo((LocalDateTime) a.get("LASTMESSAGETIME")))
                .toList();
    }
    
    @Async("messageThreadPoolTaskExecutor")
    public void markAsReadAsync(Long userId, Long otherUserId) {
        CompletableFuture.runAsync(() -> {
            try {
                messageRepository.markAsRead(userId, otherUserId);
                log.info("Messages marked as read for user {} from user {}", userId, otherUserId);
            } catch (Exception e) {
                log.error("Error marking messages as read for user {} from user {}", userId, otherUserId, e);
                throw new CompletionException(e);
            }
        });
    }
    
    public void markAsRead(Long userId, Long otherUserId) {
        messageRepository.markAsRead(userId, otherUserId);
    }
    
    @Async("messageThreadPoolTaskExecutor")
    public CompletableFuture<Long> getUnreadCountAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long count = messageRepository.countUnreadMessages(userId);
                log.debug("Unread count for user {}: {}", userId, count);
                return count;
            } catch (Exception e) {
                log.error("Error getting unread count for user {}", userId, e);
                throw new CompletionException(e);
            }
        });
    }
    
    public Long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }
}