import { decodeToken } from "./Auth.js"; 
import user from "../models/User.js";
import { redirectIfLoggedIn } from '../utils/authUtils.js';
import { sanitizeInput, validateEmail, getCsrfToken } from '../utils/securityUtils.js';

const API_URL = "http://localhost:3000";

document.addEventListener('DOMContentLoaded', function() {
  // If user is already logged in, redirect to home page
  if (redirectIfLoggedIn()) return;
  
  setupImageSlider();
});

document
  .getElementById("signInForm")
  .addEventListener("submit", async (event) => {
    event.preventDefault();

    // Sanitize inputs before sending to server
    const email = sanitizeInput(document.getElementById("email").value.trim());
    const password = document.getElementById("password").value; 

    if (!validateEmail(email)) {
      showError("Please enter a valid email address");
      return;
    }

    try {
      const response = await fetch(`${API_URL}/users/login`, {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          "X-CSRF-Token": getCsrfToken() 
        },
        body: JSON.stringify({ email, password }),
      });

      if (response.ok) {
        const data = await response.json();
        
        // Store the token in localStorage
        localStorage.setItem("Token", data.token);

        // Decode the token and save user details
        const decoded = jwt_decode(data.token);

        // Save user information to the user model
        user.setUser({
          id: decoded.id,
          email: decoded.email,
          firstName: decoded.firstName,
          lastName: decoded.lastName,
          phone: decoded.phone,
          createdAt: decoded.createdAt 
        });

        // Redirect to the home page
        window.location.href = "../Home/Home.html";
      } else {
        const error = await response.json().catch(() => ({ message: "Authentication failed" }));
        showError(error.message || "Authentication failed");
      }
    } catch (err) {
      console.error("Error during sign-in:", err);
      showError("An error occurred. Please try again.");
    }
  });

function showError(message) {
  const errorElement = document.getElementById("error-message") || createErrorElement();
  errorElement.textContent = message;
  errorElement.style.display = "block";
}

function createErrorElement() {
  const errorElement = document.createElement("div");
  errorElement.id = "error-message";
  errorElement.className = "error-message";
  document.getElementById("signInForm").prepend(errorElement);
  return errorElement;
}

function setupImageSlider() {
  const imageFolder = './images/';
  const imageFiles = ['Imagine1.webp', 'Imagine2.jpg', 'Imagine3.webp','Imagine4.jpg']; 
  const imageSlider = document.getElementById('imageSlider');
  
  if (!imageSlider) return;
  
  while (imageSlider.firstChild) {
    imageSlider.removeChild(imageSlider.firstChild);
  }

  imageFiles.forEach((fileName, index) => {
    const img = document.createElement('img');
    img.src = `${imageFolder}${fileName}`;
    img.alt = `Image ${index + 1}`;
    img.onerror = () => console.error(`Image not found: ${img.src}`);
    if (index === 0) img.classList.add('active');
    imageSlider.appendChild(img);
  });

  // Setup image rotation
  const images = document.querySelectorAll('.photo-section img');
  let currentIndex = 0;

  setInterval(() => {
    images[currentIndex].classList.remove('active');
    currentIndex = (currentIndex + 1) % images.length;
    images[currentIndex].classList.add('active');
  }, 15000);
}