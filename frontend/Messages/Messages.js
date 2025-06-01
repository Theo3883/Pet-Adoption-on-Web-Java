import userModel from '../models/User.js';
import { requireAuth } from '../utils/authUtils.js';
import Sidebar from '../SideBar/Sidebar.js';
import { showLoading, hideLoading } from '../utils/loadingUtils.js';
import { initializeSession } from '../utils/sessionUtils.js';

const API_URL = 'http://localhost:3000';
const token = localStorage.getItem('Token');
let user;
let currentConversationUser = null;
let conversations = [];
let currentMessages = [];
let pollingInterval;
let currentContactOnlineStatus = false;

async function initialize() {
  const linkElement = document.createElement("link");
  linkElement.rel = "stylesheet";
  linkElement.href = "../utils/loadingUtils.css";
  document.head.appendChild(linkElement);

  user = requireAuth();
  if (!user) return;
  
  // Initialize session for online status tracking
  await initializeSession();
  
  document.getElementById('sidebar-container').innerHTML = Sidebar.render('messages');
  new Sidebar('messages');
  
  document.getElementById('send-message-form').addEventListener('submit', handleSendMessage);
  
  initializeMobileView();
  setupMobileNavigation();
  setupScrollObserver();
  
  await loadConversations(true);
  
  pollingInterval = setInterval(async () => {
    if (currentConversationUser) {
      await loadConversation(currentConversationUser.userId, false);
    }
    await loadConversations(false);
  }, 2000);
  
  window.addEventListener('beforeunload', () => {
    if (pollingInterval) {
      clearInterval(pollingInterval);
    }
  });
}

function initializeMobileView() {
  const isMobile = window.innerWidth <= 768;
  const conversationsList = document.querySelector('.conversations-list');
  const messagesContainer = document.querySelector('.messages-container');
  
  const preventScroll = (e) => {
    if (!e.target.closest('.messages') && !e.target.closest('#message-input')) {
      e.preventDefault();
    }
  };
  
  if (isMobile) {
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
    
    document.body.style.overflow = 'hidden';
    document.querySelector('.main-content').style.overflow = 'hidden';
    
    const messagesDiv = document.querySelector('.messages');
    if (messagesDiv) {
      messagesDiv.style.overflowY = 'auto';
    }
    
    document.addEventListener('touchmove', preventScroll, { passive: false });
  } else {
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
  
  document.querySelectorAll('.conversation-item').forEach(item => {
    item.addEventListener('click', () => {
      const userId = parseInt(item.dataset.userId);
      loadConversation(userId);
      
      if (window.sidebarInstance) {
        setTimeout(() => {
          window.sidebarInstance.fetchUnreadMessageCount();
        }, 500);
      }
    });
  });
}

function truncateMessage(message) {
  return message.length > 30 ? message.substring(0, 27) + '...' : message;
}

function setupMobileNavigation() {
  const backButton = document.querySelector('.back-button');
  const conversationsList = document.querySelector('.conversations-list');
  const messagesContainer = document.querySelector('.messages-container');
  
  if (window.innerWidth <= 768) {
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
  }
  
  backButton.addEventListener('click', (e) => {
    e.preventDefault();
    conversationsList.classList.add('active');
    messagesContainer.classList.remove('active');
    
    conversationsList.style.transform = 'translateX(0)';
    messagesContainer.style.transform = 'translateX(100%)';
    
    currentConversationUser = null;
  });
  
  document.addEventListener('click', (e) => {
    const conversationItem = e.target.closest('.conversation-item');
    if (conversationItem && window.innerWidth <= 768) {
      conversationsList.classList.remove('active');
      messagesContainer.classList.add('active');
      
      conversationsList.style.transform = 'translateX(-100%)';
      messagesContainer.style.transform = 'translateX(0)';
    }
  });
  
  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', () => {
      if (window.innerWidth <= 768) {
        const keyboardHeight = window.innerHeight - window.visualViewport.height;
        
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
    
    await fetch(`${API_URL}/messages/read`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ otherUserId })
    });
    
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
    
    const conversationTitleElem = document.getElementById('conversation-title');
    if (currentConversationUser) {
      conversationTitleElem.innerHTML = `${currentConversationUser.name} <span id="contact-online-status" class="status-container"><span class="offline-dot"></span> <span class="offline-text">Offline</span></span>`;
        currentContactOnlineStatus = false;
      fetchContactOnlineStatus(currentConversationUser.userId);
      
      if (window.contactStatusInterval) clearInterval(window.contactStatusInterval);
      window.contactStatusInterval = setInterval(() => {
        fetchContactOnlineStatus(currentConversationUser.userId);
      }, 5000); // Check every 5 seconds instead of 15
    } else {
      conversationTitleElem.textContent = 'Messages';
      if (window.contactStatusInterval) clearInterval(window.contactStatusInterval);
    }
    
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
    
    document.querySelectorAll('.conversation-item').forEach(item => {
      item.classList.toggle('selected', parseInt(item.dataset.userId) === otherUserId);
    });
    
    scrollToBottom();
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
  
  setTimeout(() => {
    scrollToBottom();
  }, 100);
}

function setupScrollObserver() {
  const messagesContainer = document.getElementById('messages');
  if (messagesContainer) {
    const observer = new MutationObserver((mutations) => {
      scrollToBottom();
    });
    
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
    const extraOffset = isMobile ? 2000 : 1000;
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
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    return 'Yesterday';
  } else if (diffDays < 7) {
    return date.toLocaleDateString([], { weekday: 'short' });
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
}

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

async function fetchContactOnlineStatus(userId) {
  try {
    const token = localStorage.getItem('Token');
    if (!token || !userId) return;
    
    // Add a timestamp to prevent caching
    const timestamp = new Date().getTime();
    const response = await fetch(`http://localhost:3000/messages/online-status/${userId}?t=${timestamp}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache'
      }
    });
    
    if (!response.ok) {
      console.debug(`Error fetching online status (${response.status}): ${response.statusText}`);
      return;
    }
    
    const data = await response.json();
    console.debug(`User ${userId} online status: ${data.isOnline}`);
    updateContactOnlineStatus(data.isOnline === true);
  } catch (e) {
    console.debug('Could not fetch online status:', e.message);
  }
}

function updateContactOnlineStatus(isOnline) {
  const statusElem = document.getElementById('contact-online-status');
  if (!statusElem) return;
  
  if (currentContactOnlineStatus === isOnline) return;
  
  currentContactOnlineStatus = isOnline;
  
  const dotClass = isOnline ? 'online-dot' : 'offline-dot';
  const textClass = isOnline ? 'online-text' : 'offline-text';
  const statusText = isOnline ? 'Online' : 'Offline';
  
  statusElem.innerHTML = `<span class="${dotClass}"></span> <span class="${textClass}">${statusText}</span>`;
}

window.addEventListener('beforeunload', () => {
  if (window.contactStatusInterval) clearInterval(window.contactStatusInterval);
  
  const token = localStorage.getItem('Token');
  const sessionId = localStorage.getItem('sessionId');
  
  if (token && sessionId && navigator.sendBeacon) {
    const data = new Blob([JSON.stringify({ sessionId: sessionId })], 
      { type: 'application/json' });
    
    navigator.sendBeacon('http://localhost:3000/messages/session/unregister', data);
    console.log('Sent session unregister beacon on page unload');
  }
});