import { redirectIfLoggedIn } from '../utils/authUtils.js';
import { sanitizeInput, validateEmail, getCsrfToken } from '../utils/securityUtils.js';

const API_URL = 'http://localhost:3000';

document.addEventListener('DOMContentLoaded', function() {
  // If user is already logged in, redirect to home page
  if (redirectIfLoggedIn()) return;
});

document.getElementById('signUpForm').addEventListener('submit', async (event) => {
  event.preventDefault();

  // Sanitize user inputs
  const firstName = sanitizeInput(document.getElementById('firstName').value.trim());
  const lastName = sanitizeInput(document.getElementById('lastName').value.trim());
  const email = sanitizeInput(document.getElementById('email').value.trim());
  const password = document.getElementById('password').value; // Don't sanitize passwords
  const phone = sanitizeInput(document.getElementById('phone').value.trim());
  const street = sanitizeInput(document.getElementById('street').value.trim());
  const city = sanitizeInput(document.getElementById('city').value.trim());
  const state = sanitizeInput(document.getElementById('state').value.trim());
  const zipCode = sanitizeInput(document.getElementById('zipCode').value.trim());
  const country = sanitizeInput(document.getElementById('country').value.trim());

  // Validate email format
  if (!validateEmail(email)) {
    alert('Please enter a valid email address');
    return;
  }

  const formData = {
    firstName,
    lastName,
    email,
    password,
    phone,
    address: {
      street,
      city,
      state,
      zipCode,
      country,
    },
  };

  try {
    const response = await fetch(`${API_URL}/users/signup`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-CSRF-Token': getCsrfToken() 
      },
      body: JSON.stringify(formData),
    });

    if (response.ok) {
      alert('Sign Up Successful! Redirecting to login page...');
      window.location.href = 'SignIn.html';
    } else {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }));
      alert(`Error: ${error.message}`);
    }
  } catch (err) {
    console.error('Error during sign-up:', err);
    alert('An error occurred. Please try again.');
  }
});