const API_URL = "http://localhost:3000";

export function generateSessionId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

export async function registerUserSession(token) {
  try {
    const existingSessionId = localStorage.getItem("sessionId");
    if (existingSessionId) {
      console.log("Session already registered:", existingSessionId);
      return;
    }

    const sessionId = generateSessionId();
    localStorage.setItem("sessionId", sessionId);

    const response = await fetch(`${API_URL}/messages/session/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ sessionId: sessionId }),
    });

    if (response.ok) {
      console.log("User session registered successfully:", sessionId);
    } else {
      console.error("Failed to register user session");
      localStorage.removeItem("sessionId");
    }
  } catch (error) {
    console.error("Error registering user session:", error);
    localStorage.removeItem("sessionId");
  }
}

export async function unregisterUserSession() {
  try {
    const token = localStorage.getItem("Token");
    const sessionId = localStorage.getItem("sessionId");

    if (!token || !sessionId) {
      console.log("No session to unregister");
      return false;
    }

    console.log("Unregistering session:", sessionId);
    let unregistrationSuccessful = false;

    try {
      const response = await fetch(`${API_URL}/messages/session/unregister`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
          "Cache-Control": "no-cache, no-store, must-revalidate",
          "Pragma": "no-cache"
        },
        body: JSON.stringify({ sessionId: sessionId }),
      });

      if (response.ok) {
        console.log("User session unregistered successfully via fetch");
        unregistrationSuccessful = true;
      } else {
        console.error("Failed to unregister session via fetch:", response.status);
        
        if (navigator.sendBeacon) {
          const success = navigator.sendBeacon(
            `${API_URL}/messages/session/unregister`,
            new Blob([JSON.stringify({ sessionId: sessionId })], { 
              type: "application/json" 
            })
          );
          console.log("Beacon sent for session unregistration:", success);
          unregistrationSuccessful = success;
        }
      }
    } catch (error) {
      console.error("Error in fetch, trying alternative methods:", error);
      
      try {
        const xhr = new XMLHttpRequest();
        xhr.open("POST", `${API_URL}/messages/session/unregister`, false);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Authorization", `Bearer ${token}`);
        xhr.setRequestHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        xhr.setRequestHeader("Pragma", "no-cache");
        xhr.send(JSON.stringify({ sessionId: sessionId }));
        console.log("XHR session unregister result:", xhr.status);
        unregistrationSuccessful = (xhr.status >= 200 && xhr.status < 300);
      } catch (e) {
        console.error("All session unregister methods failed:", e);
      }
    }

    localStorage.removeItem("sessionId");
    console.log("Session ID removed from localStorage");
    return unregistrationSuccessful;
  } catch (error) {
    console.error("Error unregistering user session:", error);
    localStorage.removeItem("sessionId");
    return false;
  }
}

export async function initializeSession() {
  const token = localStorage.getItem("Token");
  if (token) {
    await registerUserSession(token);
    setupSessionCleanup();
  }
}

export function setupSessionCleanup() {
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
      console.log("Page hidden - user may appear less active");
    } else {
      console.log("Page visible - user is active again");
    }
  });

  window.addEventListener("pagehide", () => {
    const token = localStorage.getItem("Token");
    const sessionId = localStorage.getItem("sessionId");

    if (token && sessionId) {
      if (navigator.sendBeacon) {
        const blob = new Blob([JSON.stringify({ sessionId: sessionId })], { 
          type: "application/json" 
        });
        
        const headers = {
          type: "application/json",
          Authorization: `Bearer ${token}`
        };
        
        navigator.sendBeacon(
          `${API_URL}/messages/session/unregister`,
          blob
        );
      }
      
      localStorage.removeItem("sessionId");
      console.log("Session cleanup sent via pagehide");
    }
  });

  window.addEventListener("beforeunload", () => {
    const token = localStorage.getItem("Token");
    const sessionId = localStorage.getItem("sessionId");

    if (token && sessionId) {
      try {
        const xhr = new XMLHttpRequest();
        xhr.open("POST", `${API_URL}/messages/session/unregister`, false);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Authorization", `Bearer ${token}`);
        xhr.setRequestHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        xhr.send(JSON.stringify({ sessionId: sessionId }));
      } catch (e) {
        console.error("Failed to unregister on beforeunload:", e);
      }
      
      localStorage.removeItem("sessionId");
      console.log("Session ID removed on beforeunload");
    }
  });
}

export function startSessionHeartbeat(intervalMs = 30000) {
  const heartbeatInterval = setInterval(async () => {
    const token = localStorage.getItem("Token");
    const sessionId = localStorage.getItem("sessionId");

    if (!token || !sessionId) {
      clearInterval(heartbeatInterval);
      return;
    }

    try {
      const timestamp = new Date().getTime();
      await fetch(`${API_URL}/messages/online-count?t=${timestamp}`, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Cache-Control": "no-cache, no-store, must-revalidate",
          "Pragma": "no-cache"
        },
      });
      console.log("Session heartbeat sent");
    } catch (error) {
      console.error("Session heartbeat failed:", error);
    }
  }, intervalMs);

  return heartbeatInterval;
}
