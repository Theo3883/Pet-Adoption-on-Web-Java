const { parseRequestBody } = require('../utils/requestUtils');
const Message = require('../models/Message');

// Send a new message
async function sendMessage(req, res) {
  try {
    
    if (!req.user || !req.user.id) {
      console.error('Authentication error: Missing user ID in request', req.user);
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'User authentication required' }));
      return;
    }

    const body = await parseRequestBody(req);
    const { receiverId, content } = body;
    const senderId = req.user.id;

    if (!receiverId || !content) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing required fields' }));
      return;
    }

    console.log('Sending message with:', { senderId, receiverId, content });

    
    const messageId = await Message.create(senderId, receiverId, content);
    
    res.writeHead(201, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ 
      message: 'Message sent successfully',
      messageId 
    }));
  } catch (err) {
    console.error('Error sending message:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

// Get conversation with another user
async function getConversation(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { otherUserId } = body;
    const userId = req.user.id;

    if (!otherUserId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing other user ID' }));
      return;
    }

   
    const messages = await Message.getConversation(userId, otherUserId);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(messages));
  } catch (err) {
    console.error('Error retrieving conversation:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

// Get list of conversations for current user
async function getConversations(req, res) {
  try {
    const userId = req.user.id;
    
    
    const conversations = await Message.getConversations(userId);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(conversations));
  } catch (err) {
    console.error('Error retrieving conversations:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

// Mark messages as read
async function markMessagesAsRead(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { otherUserId } = body;
    const userId = req.user.id;

    if (!otherUserId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing other user ID' }));
      return;
    }

    
    await Message.markAsRead(userId, otherUserId);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Messages marked as read' }));
  } catch (err) {
    console.error('Error marking messages as read:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

async function getUnreadCount(req, res) {
  try {
    const userId = req.user.id;
    
    const unreadCount = await Message.countUnreadMessages(userId);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ count: unreadCount }));
  } catch (err) {
    console.error('Error retrieving unread message count:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

module.exports = { 
  sendMessage, 
  getConversation, 
  getConversations, 
  markMessagesAsRead,
  getUnreadCount,
};