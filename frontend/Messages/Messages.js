import userModel from '../models/User.js';
import { requireAuth } from '../utils/authUtils.js';
import Sidebar from '../SideBar/Sidebar.js';
import { showLoading, hideLoading } from '../utils/loadingUtils.js';

const API_URL = 'http://localhost:3000';
const token = localStorage.getItem('Token');
let user;
let currentConversationUser = null;
let conversations = [];
let currentMessages = [];
let pollingInterval;

async function initialize() {
  // loading spinner
  const linkElement = document.createElement("link");
  linkElement.rel = "stylesheet";
  linkElement.href = "../utils/loadingUtils.css";
  document.head.appendChild(linkElement);

  user = requireAuth();
  if (!user) return;
  
  // Render sidebar
  document.getElementById('sidebar-container').innerHTML = Sidebar.render('messages');
  new Sidebar('messages');
  
  document.getElementById('send-message-form').addEventListener('submit', handleSendMessage);
  
  initializeMobileView();
  
  setupMobileNavigation();
  
  // Set up mutation observer to detect when messages are added and scroll to bottom
  setupScrollObserver();
  
  await loadConversations(true);
  
  // Set up polling for new messages (every 30 seconds)
  pollingInterval = setInterval(async () => {
    if (currentConversationUser) {
      await loadConversation(currentConversationUser.userId, false);
    }
    await loadConversations(false);
  }, 30000); 
  
  window.addEventListener('beforeunload', () => {
    if (pollingInterval) {
      clearInterval(pollingInterval);
    }
  });
}

// Initialize mobile view state
function initializeMobileView() {
  const isMobile = window.innerWidth <= 768;
  const conversationsList = document.querySelector('.conversations-list');
  const messagesContainer = document.querySelector('.messages-container');
  
  // Define preventScroll function outside the if-block so it's accessible everywhere
  const preventScroll = (e) => {
    if (!e.target.closest('.messages') && !e.target.closest('#message-input')) {
      e.preventDefault();
    }
  };
  
  if (isMobile) {
    // Start with conversation list visible
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    // Reset any transform styling to ensure proper display
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
    
    // Fix scrolling - only message box should scroll
    document.body.style.overflow = 'hidden';
    document.querySelector('.main-content').style.overflow = 'hidden';
    
    // Make sure the messages div is scrollable
    const messagesDiv = document.querySelector('.messages');
    if (messagesDiv) {
      messagesDiv.style.overflowY = 'auto';
    }
    
    // Prevent other elements from scrolling
    document.addEventListener('touchmove', preventScroll, { passive: false });
  } else {
    // On desktop, both should be visible
    messagesContainer.classList.add('active');
    document.body.style.overflow = '';
    document.removeEventListener('touchmove', preventScroll);
  }
}

async function loadConversations(showLoader = true) {
  try {
    if (showLoader) {
      showLoading('Loading conversations...');
    }
    
    const response = await fetch(`${API_URL}/messages/conversations`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to load conversations');
    }
    
    conversations = await response.json();
    displayConversations(conversations);
  } catch (error) {
    console.error('Error loading conversations:', error);

    if (showLoader) {
      document.getElementById('conversation-list').innerHTML = 
        '<div class="error-message">Failed to load conversations</div>';
    }
  } finally {
    if (showLoader) {
      hideLoading();
    }
  }
}

function displayConversations(conversations) {
  const container = document.getElementById('conversation-list');
  
  if (conversations.length === 0) {
    container.innerHTML = `<div class="empty-state">No conversations yet</div>`;
    return;
  }
  
  container.innerHTML = conversations.map(conv => `
    <div class="conversation-item ${conv.unreadCount > 0 ? 'unread' : ''} ${currentConversationUser && currentConversationUser.userId === conv.OTHERUSERID ? 'selected' : ''}" data-user-id="${conv.OTHERUSERID}">
      <div class="conversation-avatar">${getInitials(conv.OTHERUSERNAME)}</div>
      <div class="conversation-info">
        <div class="conversation-name">${conv.OTHERUSERNAME}</div>
        <div class="conversation-preview">
          <span class="last-message-preview">${conv.LASTMESSAGECONTENT ? truncateMessage(conv.LASTMESSAGECONTENT) : ''}</span>
          <span class="last-message-time">${formatTimestamp(conv.LASTMESSAGETIME)}</span>
        </div>
      </div>
      ${conv.unreadCount > 0 ? `<div class="unread-badge">${conv.unreadCount}</div>` : ''}
    </div>
  `).join('');
  
  // Add event listeners to conversation items
  document.querySelectorAll('.conversation-item').forEach(item => {
    item.addEventListener('click', () => {
      const userId = parseInt(item.dataset.userId);
      loadConversation(userId);
      
      // Update sidebar unread count after opening a conversation
      if (window.sidebarInstance) {
        setTimeout(() => {
          window.sidebarInstance.fetchUnreadMessageCount();
        }, 500);
      }
    });
  });
}

// Add this helper function to truncate long messages in the preview
function truncateMessage(message) {
  return message.length > 30 ? message.substring(0, 27) + '...' : message;
}

// Add mobile navigation setup
function setupMobileNavigation() {
  const backButton = document.querySelector('.back-button');
  const conversationsList = document.querySelector('.conversations-list');
  const messagesContainer = document.querySelector('.messages-container');
  
  // Make conversations list visible by default on mobile
  if (window.innerWidth <= 768) {
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    // Set transforms explicitly
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
  }
  
  backButton.addEventListener('click', (e) => {
    e.preventDefault();
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    // Set transforms explicitly
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
    
    // Reset conversation state
    currentConversationUser = null;
  });
  
  document.addEventListener('click', (e) => {
    const conversationItem = e.target.closest('.conversation-item');
    if (conversationItem && window.innerWidth <= 768) {
      conversationsList.classList.remove('active');
      messagesContainer.classList.add('active');
      
      // Set transforms explicitly
      conversationsList.style.transform = 'translateX(-100%)';
      messagesContainer.style.transform = 'translateX(0)';
    }
  });
  
  // Adjust UI when keyboard appears on mobile
  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', () => {
      if (window.innerWidth <= 768) {
        const keyboardHeight = window.innerHeight - window.visualViewport.height;
        
        // If keyboard is visible
        if (keyboardHeight > 100) {
          document.querySelector('.message-input-area').style.bottom = '0px';
        } else {
          document.querySelector('.message-input-area').style.bottom = '60px';
        }
      }
    });
  }
}

async function loadConversation(otherUserId, showLoader = true) {
  try {
    if (showLoader) {
      showLoading('Loading messages...');
    }
    
    // Mark messages as read when opening conversation
    await fetch(`${API_URL}/messages/read`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ otherUserId })
    });
    
    // Fetch conversation
    const response = await fetch(`${API_URL}/messages/conversation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ otherUserId })
    });
    
    if (!response.ok) {
      throw new Error('Failed to load conversation');
    }
    
    currentMessages = await response.json();
    
    // Find user details from the messages
    if (currentMessages.length > 0) {
      const message = currentMessages[0];
      if (message.SENDERID === otherUserId) {
        currentConversationUser = {
          userId: otherUserId,
          name: `${message.SENDERFIRSTNAME} ${message.SENDERLASTNAME}`
        };
      } else {
        currentConversationUser = {
          userId: otherUserId,
          name: `${message.RECEIVERFIRSTNAME} ${message.RECEIVERLASTNAME}`
        };
      }
    }
    
    displayMessages(currentMessages, otherUserId);
    
    // Update conversation UI
    document.getElementById('conversation-title').textContent = currentConversationUser ? 
      currentConversationUser.name : 'Messages';
    
    const messagesContainer = document.querySelector('.messages-container');
    const conversationsList = document.querySelector('.conversations-list');
    
    if (window.innerWidth <= 768) {
      conversationsList.classList.remove('active');
      messagesContainer.classList.add('active');
      
      conversationsList.style.transform = 'translateX(-100%)';
      messagesContainer.style.transform = 'translateX(0)';
    } else {
      messagesContainer.classList.add('active');
    }
    
    // Mark the selected conversation in the list
    document.querySelectorAll('.conversation-item').forEach(item => {
      item.classList.toggle('selected', parseInt(item.dataset.userId) === otherUserId);
    });
    
    // Scroll to bottom of messages to ensure the last message is visible
    scrollToBottom();
    
    // Add an extra scroll after a delay to handle any layout shifts
    setTimeout(scrollToBottom, 500);
    
  } catch (error) {
    console.error('Error loading conversation:', error);
    if (showLoader) {
      document.getElementById('messages').innerHTML = 
        '<div class="error-message">Failed to load messages</div>';
    }
  } finally {
    if (showLoader) {
      hideLoading();
    }
  }
}

// Update displayMessages function to ensure message visibility
function displayMessages(messages, otherUserId) {
  const container = document.getElementById('messages');
  
  if (messages.length === 0) {
    container.innerHTML = `<div class="empty-state">No messages yet. Start the conversation!</div>`;
    return;
  }
  
  container.innerHTML = messages.map(msg => {
    const isSentByMe = msg.SENDERID === user.id;
    
    const readStatus = isSentByMe ? `
      <span class="read-status ${msg.ISREAD ? 'read' : 'delivered'}">
        <span class="checkmark">✓</span><span class="checkmark">✓</span>
      </span>
    ` : '';
    
    return `
      <div class="message ${isSentByMe ? 'sent' : 'received'}">
        <div class="message-content">
          ${msg.CONTENT}
          <span class="message-time">
            ${formatTimeOnly(msg.TIMESTAMP)} ${readStatus}
          </span>
        </div>
      </div>
    `;
  }).join('');
  
  // Ensure proper scrolling to the bottom with a small delay
  setTimeout(() => {
    scrollToBottom();
  }, 100);
}

// Set up an observer to detect content changes and scroll to bottom
function setupScrollObserver() {
  const messagesContainer = document.getElementById('messages');
  if (messagesContainer) {
    // Create a MutationObserver to watch for changes in the messages container
    const observer = new MutationObserver((mutations) => {
      // If content changed (messages added/removed), scroll to bottom
      scrollToBottom();
    });
    
    // Start observing the container for changes in its children
    observer.observe(messagesContainer, { 
      childList: true,
      subtree: true
    });
  }
}


function scrollToBottom() {
  const messagesDiv = document.getElementById('messages');
  if (messagesDiv) {
    const isMobile = window.innerWidth <= 768;
    const extraOffset = isMobile ? 2000 : 1000; // Extra padding on mobile
    messagesDiv.scrollTop = messagesDiv.scrollHeight + extraOffset;
    
    setTimeout(() => {
      messagesDiv.scrollTop = messagesDiv.scrollHeight + extraOffset;
    }, 50);
  }
}

function formatTimeOnly(timestamp) {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Handle sending messages
async function handleSendMessage(event) {
  event.preventDefault();
  
  const messageInput = document.getElementById('message-input');
  const content = messageInput.value.trim();
  
  if (!content || !currentConversationUser) {
    return;
  }
  
  messageInput.value = '';
  
  try {
    const tempId = `temp-${Date.now()}`;
    const messagesContainer = document.querySelector('.messages');
    const tempMessage = document.createElement('div');
    tempMessage.id = tempId;
    tempMessage.className = 'message sent';
    
    // Format current time
    const now = new Date();
    const timeString = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    tempMessage.innerHTML = `
      <div class="message-content">
        ${content}
        <span class="message-time">
          ${timeString} <span class="read-status delivered"><span class="checkmark">✓</span><span class="checkmark">✓</span></span>
        </span>
      </div>
    `;
    messagesContainer.appendChild(tempMessage);
    
    scrollToBottom();
    
    await sendMessage(currentConversationUser.userId, content);
    
    await loadConversations(false);
    
    await loadConversation(currentConversationUser.userId, false);
    
    messageInput.focus();
  } catch (error) {
    console.error('Error sending message:', error);
    const tempMessage = document.getElementById(tempId);
    if (tempMessage) {
      tempMessage.querySelector('.message-time').textContent = 'Failed to send';
      tempMessage.style.opacity = '0.7';
    }
  }
}

async function sendMessage(receiverId, content) {
  try {
    const token = localStorage.getItem('Token');
    
    if (!token) {
      console.error('No authentication token found');
      return false;
    }
    
    const response = await fetch(`${API_URL}/messages/send`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        receiverId,
        content
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to send message');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error sending message:', error);
    throw error;
  }
}

// "Contact Owner" button 
export function setupContactButton(ownerId, ownerName) {
  const contactButton = document.querySelector('.contact-button');
  if (contactButton) {
    contactButton.addEventListener('click', () => {
      window.location.href = `../Messages/Messages.html?userId=${ownerId}&name=${encodeURIComponent(ownerName)}`;
    });
  }
}

// Helper functions
function getInitials(name) {
  return name
    .split(' ')
    .map(part => part.charAt(0).toUpperCase())
    .join('')
    .slice(0, 2);
}

function formatTimestamp(timestamp) {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  const now = new Date();
  const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
  
  if (diffDays === 0) {
    // Today - show time
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    // Yesterday
    return 'Yesterday';
  } else if (diffDays < 7) {
    // This week - show day name
    return date.toLocaleDateString([], { weekday: 'short' });
  } else {
    // Older - show date
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
}

// Check URL for direct conversation opening
function checkUrlForDirectMessage() {
  const urlParams = new URLSearchParams(window.location.search);
  const userId = urlParams.get('userId');
  const name = urlParams.get('name');
  
  if (userId) {
    currentConversationUser = {
      userId: parseInt(userId),
      name: name || 'User'
    };
    loadConversation(parseInt(userId));
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initialize();
  
  setTimeout(checkUrlForDirectMessage, 500);
});