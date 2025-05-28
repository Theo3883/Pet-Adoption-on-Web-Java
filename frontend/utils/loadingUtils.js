let styleInjected = false;

function injectLoadingStyle() {
  if (styleInjected) return;
  
  const style = document.createElement('style');
  style.textContent = `
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(255, 255, 255, 0.8);
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      opacity: 1;
      transition: opacity 0.3s ease;
    }
    
    .loading-overlay.fade-out {
      opacity: 0;
    }
    
    .loading-spinner {
      width: 60px;
      height: 60px;
      border: 6px solid #f3f3f3;
      border-top: 6px solid #fca311;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 15px;
    }
    
    .loading-message {
      font-family: Arial, sans-serif;
      color: #333;
      font-size: 16px;
      font-weight: 600;
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `;
  
  document.head.appendChild(style);
  styleInjected = true;
}

export function showLoading(message = "Loading...") {
  injectLoadingStyle();
  
  const existingSpinner = document.getElementById("global-loading-spinner");
  if (existingSpinner) {
    return existingSpinner;
  }
  
  const spinnerOverlay = document.createElement("div");
  spinnerOverlay.id = "global-loading-spinner";
  spinnerOverlay.className = "loading-overlay";
  
  const spinner = document.createElement("div");
  spinner.className = "loading-spinner";
  
  const messageElement = document.createElement("div");
  messageElement.className = "loading-message";
  messageElement.textContent = message;
  
  spinnerOverlay.appendChild(spinner);
  spinnerOverlay.appendChild(messageElement);
  document.body.appendChild(spinnerOverlay);
  
  return spinnerOverlay;
}

export function hideLoading() {
  const spinner = document.getElementById("global-loading-spinner");
  if (spinner) {
    spinner.classList.add("fade-out");
    setTimeout(() => {
      if (spinner.parentNode) {
        spinner.parentNode.removeChild(spinner);
      }
    }, 300);
  }
}