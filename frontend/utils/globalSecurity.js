

import { getCsrfToken, generateCsrfToken } from './securityUtils.js';
import { fetchCsrfToken } from './fetchUtils.js';

document.addEventListener('DOMContentLoaded', function() {

  initializeCsrfProtection();
  
  applySafelyPreloadedStylesheets();
  
  ensureSecurityHeaders();
});


async function initializeCsrfProtection() {

  let token = getCsrfToken();
  
  if (!token) {
    try {
      // Fetch a new token from the server
      token = await fetchCsrfToken();
    } catch (error) {
      console.warn('Could not fetch CSRF token from server, generating local token instead');
      token = generateCsrfToken();
    }
  }
  
 
  const csrfMeta = document.getElementById('csrf-token');
  if (csrfMeta) {
    csrfMeta.setAttribute('content', token);
  } else {

    const meta = document.createElement('meta');
    meta.id = 'csrf-token';
    meta.name = 'csrf-token';
    meta.content = token;
    document.head.appendChild(meta);
  }
  
  // Add CSRF token to all forms
  document.querySelectorAll('form').forEach(form => {
    let csrfInput = form.querySelector('input[name="csrf-token"]');
    if (!csrfInput) {
      csrfInput = document.createElement('input');
      csrfInput.type = 'hidden';
      csrfInput.name = 'csrf-token';
      csrfInput.value = token;
      form.appendChild(csrfInput);
    } else {
      csrfInput.value = token;
    }
  });


  sessionStorage.setItem('csrfToken', token);
}


function applySafelyPreloadedStylesheets() {
  const preloadLinks = document.querySelectorAll('link[rel="preload"][as="style"]');
  preloadLinks.forEach(link => {
    // Create a new stylesheet link
    const styleSheet = document.createElement('link');
    styleSheet.rel = 'stylesheet';
    styleSheet.href = link.href;
    
    // Insert after the preload link
    if (link.parentNode) {
      link.parentNode.insertBefore(styleSheet, link.nextSibling);
    } else {
      document.head.appendChild(styleSheet);
    }
  });
}


function ensureSecurityHeaders() {

  if (!document.querySelector('meta[http-equiv="Content-Security-Policy"]')) {
    const cspMeta = document.createElement('meta');
    cspMeta.httpEquiv = 'Content-Security-Policy';

    cspMeta.content = "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://maps.googleapis.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' data: http://localhost:3000 https://maps.gstatic.com https://maps.googleapis.com; connect-src 'self' http://localhost:3000 https://maps.googleapis.com; font-src 'self' https://fonts.gstatic.com";
    document.head.insertBefore(cspMeta, document.head.firstChild);
  } else {

    const existingCsp = document.querySelector('meta[http-equiv="Content-Security-Policy"]');
    if (existingCsp) {
      if (!existingCsp.content.includes('unsafe-inline')) {
        existingCsp.content = existingCsp.content.replace("script-src 'self'", "script-src 'self' 'unsafe-inline'");
      }
      if (!existingCsp.content.includes('maps.googleapis.com')) {
        existingCsp.content = existingCsp.content.replace("script-src", "script-src https://maps.googleapis.com");
        existingCsp.content = existingCsp.content.replace("connect-src", "connect-src https://maps.googleapis.com");
      }
      if (!existingCsp.content.includes('fonts.googleapis.com')) {
        existingCsp.content = existingCsp.content.replace("style-src", "style-src https://fonts.googleapis.com");
      }
      if (!existingCsp.content.includes('fonts.gstatic.com')) {
        if (existingCsp.content.includes('font-src')) {
          existingCsp.content = existingCsp.content.replace("font-src", "font-src https://fonts.gstatic.com");
        } else {
          existingCsp.content += "; font-src 'self' https://fonts.gstatic.com";
        }
      }
      
      if (!existingCsp.content.includes('maps.gstatic.com')) {
        existingCsp.content = existingCsp.content.replace("img-src", "img-src https://maps.gstatic.com https://maps.googleapis.com");
      }
    }
  }
  
  // Add XSS protection header
  if (!document.querySelector('meta[http-equiv="X-XSS-Protection"]')) {
    const xssMeta = document.createElement('meta');
    xssMeta.httpEquiv = 'X-XSS-Protection';
    xssMeta.content = '1; mode=block';
    document.head.appendChild(xssMeta);
  }
  

  if (!document.querySelector('meta[http-equiv="X-Content-Type-Options"]')) {
    const ctMeta = document.createElement('meta');
    ctMeta.httpEquiv = 'X-Content-Type-Options';
    ctMeta.content = 'nosniff';
    document.head.appendChild(ctMeta);
  }
}

export { initializeCsrfProtection, applySafelyPreloadedStylesheets, ensureSecurityHeaders }; 