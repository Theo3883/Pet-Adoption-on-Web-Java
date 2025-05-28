package com.backend.service;

import com.backend.model.Message;
import com.backend.model.User;
import com.backend.repository.MessageRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    public Long sendMessage(Long senderId, Long receiverId, String content) {
        Optional<User> sender = userRepository.findById(senderId);
        Optional<User> receiver = userRepository.findById(receiverId);
        
        if (sender.isEmpty() || receiver.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        Message message = new Message();
        message.setSender(sender.get());
        message.setReceiver(receiver.get());
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);
        
        Message savedMessage = messageRepository.save(message);
        return savedMessage.getMessageId();
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
            
            // Add sender and receiver name details for frontend
            messageMap.put("SENDERFIRSTNAME", message.getSender().getFirstName());
            messageMap.put("SENDERLASTNAME", message.getSender().getLastName());
            messageMap.put("RECEIVERFIRSTNAME", message.getReceiver().getFirstName());
            messageMap.put("RECEIVERLASTNAME", message.getReceiver().getLastName());
            
            return messageMap;
        }).collect(Collectors.toList());
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
        //sort the conversations
        return conversationMap.values().stream()
                .sorted((a, b) -> ((LocalDateTime) b.get("LASTMESSAGETIME"))
                        .compareTo((LocalDateTime) a.get("LASTMESSAGETIME")))
                .collect(Collectors.toList());
    }
    
    public void markAsRead(Long userId, Long otherUserId) {
        messageRepository.markAsRead(userId, otherUserId);
    }
    
    public Long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }
} 