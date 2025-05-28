import { getCsrfToken } from './securityUtils.js';

export async function fetchCsrfToken() {
  try {
    const response = await fetch('http://localhost:3000/csrf-token');
    if (!response.ok) {
      throw new Error('Failed to fetch CSRF token');
    }
    
    const data = await response.json();
    // Store token in sessionStorage
    sessionStorage.setItem('csrfToken', data.csrfToken);
    return data.csrfToken;
  } catch (error) {
    console.error('Error fetching CSRF token:', error);
    throw error;
  }
}

export async function secureFetch(url, options = {}) {
  // Get stored token or fetch a new one
  let token = getCsrfToken();
  if (!token) {
    try {
      token = await fetchCsrfToken();
    } catch (error) {
      console.warn('Could not get CSRF token, proceeding without it');
    }
  }
  
  const headers = {
    ...options.headers
  };
  
  // Add CSRF token if available
  if (token) {
    headers['X-CSRF-Token'] = token;
  }
  
  // Add content type for JSON requests
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  }
  
  // Add auth token if available
  const authToken = localStorage.getItem('Token');
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  
  return fetch(url, {
    ...options,
    headers
  });
}