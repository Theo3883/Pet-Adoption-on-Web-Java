document.addEventListener('DOMContentLoaded', function() {
  const adminLoginForm = document.getElementById('adminLoginForm');
  const errorMessage = document.getElementById('error-message');
  const API_URL = 'http://localhost:3000';
  
 
  const existingToken = localStorage.getItem('adminToken');
  if (existingToken) {
    try {
      const decodedToken = jwt_decode(existingToken);
      if (decodedToken.isAdmin) {
        window.location.href = 'Dashboard.html';
        return;
      }
    } catch (error) {
      
      localStorage.removeItem('adminToken');
    }
  }

  adminLoginForm.addEventListener('submit', function(e) {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
   
    errorMessage.textContent = '';
    

    fetch(`${API_URL}/admin/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password })
    })
    .then(response => {
      if (!response.ok) {
        return response.json().then(data => {
          throw new Error(data.error || `HTTP error! Status: ${response.status}`);
        });
      }
      return response.json();
    })
    .then(data => {
      if (data.token) {
     
        localStorage.setItem('adminToken', data.token);
        
     
        window.location.href = 'Dashboard.html';
      } else {
        
        errorMessage.textContent = data.error || 'Login failed';
      }
    })
    .catch(error => {
      console.error('Error:', error);
      errorMessage.textContent = error.message || 'An error occurred. Please try again.';
    });
  });
});