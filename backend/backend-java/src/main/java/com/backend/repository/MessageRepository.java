package com.backend.repository;

import com.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m " +
           "WHERE (m.sender.userId = :userId AND m.receiver.userId = :otherUserId) " +
           "OR (m.sender.userId = :otherUserId AND m.receiver.userId = :userId) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
    
    @Query("SELECT DISTINCT CASE " +
           "WHEN m.sender.userId = :userId THEN m.receiver " +
           "ELSE m.sender END as otherUser, " +
           "MAX(m.timestamp) as lastMessageTime " +
           "FROM Message m " +
           "WHERE m.sender.userId = :userId OR m.receiver.userId = :userId " +
           "GROUP BY CASE WHEN m.sender.userId = :userId THEN m.receiver ELSE m.sender END " +
           "ORDER BY lastMessageTime DESC")
    List<Object[]> findConversationsForUser(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT m.receiver, MAX(m.timestamp) " +
           "FROM Message m " +
           "WHERE m.sender.userId = :userId " +
           "GROUP BY m.receiver " +
           "ORDER BY MAX(m.timestamp) DESC")
    List<Object[]> findConversationsAsSender(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT m.sender, MAX(m.timestamp) " +
           "FROM Message m " +
           "WHERE m.receiver.userId = :userId " +
           "GROUP BY m.sender " +
           "ORDER BY MAX(m.timestamp) DESC")
    List<Object[]> findConversationsAsReceiver(@Param("userId") Long userId);    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true " +
           "WHERE m.receiver.userId = :userId AND m.sender.userId = :otherUserId AND m.isRead = false")
    void markAsRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
    
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.receiver.userId = :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("userId") Long userId);
} 