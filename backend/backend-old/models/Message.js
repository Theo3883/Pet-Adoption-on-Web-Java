const { getConnection } = require('../db');
const oracledb = require('oracledb');

class Message {

  static async create(senderId, receiverId, content) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO Messages (senderId, receiverId, content) 
         VALUES (:senderId, :receiverId, :content)
         RETURNING messageId INTO :messageId`,
        { 
          senderId, 
          receiverId: parseInt(receiverId), 
          content,
          messageId: { type: oracledb.NUMBER, dir: oracledb.BIND_OUT }
        },
        { autoCommit: true }
      );
      
      return result.outBinds.messageId[0];
    } finally {
      await connection.close();
    }
  }


  static async getConversation(userId, otherUserId) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT m.*, 
          u1.firstName as senderFirstName, 
          u1.lastName as senderLastName,
          u2.firstName as receiverFirstName,
          u2.lastName as receiverLastName
         FROM Messages m
         JOIN Users u1 ON m.senderId = u1.userId
         JOIN Users u2 ON m.receiverId = u2.userId
         WHERE (m.senderId = :userId AND m.receiverId = :otherUserId)
         OR (m.senderId = :otherUserId AND m.receiverId = :userId)
         ORDER BY m.timestamp ASC`,
        { 
          userId, 
          otherUserId: parseInt(otherUserId)
        },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      return result.rows;
    } finally {
      await connection.close();
    }
  }


  static async getConversations(userId) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT 
          o.otherUserId,
          o.otherUserName,
          (SELECT MAX(innerM.timestamp) 
           FROM Messages innerM 
           WHERE (innerM.senderId = :userId AND innerM.receiverId = o.otherUserId)
           OR (innerM.senderId = o.otherUserId AND innerM.receiverId = :userId)) as lastMessageTime,
          (SELECT COUNT(*) 
           FROM Messages unread 
           WHERE unread.senderId = o.otherUserId
           AND unread.receiverId = :userId 
           AND unread.isRead = 0) as unreadCount
        FROM (
          SELECT DISTINCT
            CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END as otherUserId,
            CASE WHEN m.senderId = :userId THEN u2.firstName || ' ' || u2.lastName
                 ELSE u1.firstName || ' ' || u1.lastName
            END as otherUserName
          FROM Messages m
          JOIN Users u1 ON m.senderId = u1.userId
          JOIN Users u2 ON m.receiverId = u2.userId
          WHERE m.senderId = :userId OR m.receiverId = :userId
        ) o
        ORDER BY lastMessageTime DESC`,
        { userId },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      return result.rows;
    } finally {
      await connection.close();
    }
  }


  static async markAsRead(receiverId, senderId) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `UPDATE Messages 
         SET isRead = 1
         WHERE senderId = :senderId AND receiverId = :receiverId AND isRead = 0`,
        { 
          receiverId, 
          senderId: parseInt(senderId)
        },
        { autoCommit: true }
      );
      
      return result.rowsAffected;
    } finally {
      await connection.close();
    }
  }


  static async countUnreadMessages(userId) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT COUNT(*) as count
         FROM Messages
         WHERE receiverId = :userId AND isRead = 0`,
        { userId },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      return result.rows[0].COUNT;
    } finally {
      await connection.close();
    }
  }
}

module.exports = Message;