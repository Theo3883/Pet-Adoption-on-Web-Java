export function sanitizeInput(input) {
  if (!input) return '';
  
  // Convert special characters to HTML entities
  return String(input)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export function createElement(tag, attributes = {}, children = null) {
  const element = document.createElement(tag);
  
  // Set attributes
  Object.entries(attributes).forEach(([key, value]) => {
    if (key === 'className') {
      element.className = value;
    } else if (key.startsWith('on') && typeof value === 'string') {
     
      element[key] = new Function(value);
    } else {
      element.setAttribute(key, value);
    }
  });
  
  if (children) {
    if (typeof children === 'string') {
      element.textContent = children;
    } else if (children instanceof Node) {
      element.appendChild(children);
    } else if (Array.isArray(children)) {
      children.forEach(child => {
        if (typeof child === 'string') {
          element.appendChild(document.createTextNode(child));
        } else if (child instanceof Node) {
          element.appendChild(child);
        }
      });
    }
  }
  
  return element;
}


export function getCsrfToken() {
  let token = sessionStorage.getItem('csrfToken');
  if (!token) {
    token = generateCsrfToken();
  }
  return token;
}

export function generateCsrfToken() {
  const token = Math.random().toString(36).substring(2, 15) + 
               Math.random().toString(36).substring(2, 15);
  sessionStorage.setItem('csrfToken', token);
  return token;
}

export function validateEmail(email) {
  const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return re.test(email);
}