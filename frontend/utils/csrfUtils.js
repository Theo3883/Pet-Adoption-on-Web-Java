
import { generateCsrfToken } from './securityUtils.js';


document.addEventListener('DOMContentLoaded', function() {
  const csrfToken = generateCsrfToken();
  

  const metaTag = document.getElementById('csrf-token');
  if (metaTag) {
    metaTag.setAttribute('content', csrfToken);
  } else {
    const meta = document.createElement('meta');
    meta.id = 'csrf-token';
    meta.name = 'csrf-token';
    meta.content = csrfToken;
    document.head.appendChild(meta);
  }
  
  // Store token in session storage
  sessionStorage.setItem('csrfToken', csrfToken);
});