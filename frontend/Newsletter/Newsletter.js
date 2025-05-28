import Sidebar from '../SideBar/Sidebar.js';
import { requireAuth } from '../utils/authUtils.js';

const API_URL = 'http://localhost:3000';
let user;
const availableSpecies = [
  'Câine', 'Pisică', 'Papagal', 'Hamster', 'Iepure', 
  'Pește', 'Broască Țestoasă', 'Șarpe', 'Porcușor de Guineea'
];


async function initialize() {
 
  user = requireAuth();
  if (!user) return;
  
 
  document.getElementById('sidebar-container').innerHTML = Sidebar.render('newsletter');
  new Sidebar('newsletter');
  
  await fetchSubscriptions();
  
  document.getElementById('save-preferences').addEventListener('click', savePreferences);
}

// Fetch user's current newsletter subscriptions
async function fetchSubscriptions() {
  try {
    const token = localStorage.getItem('Token');
    
    if (!token) {
      console.error('No authentication token found');
      return;
    }
    
    const response = await fetch(`${API_URL}/newsletter/subscriptions`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch subscriptions');
    }
    
    const subscriptions = await response.json();
    displaySubscriptionOptions(subscriptions);
  } catch (error) {
    console.error('Error fetching subscriptions:', error);
    document.getElementById('species-options').innerHTML = 
      '<p class="error-message">Failed to load your subscriptions. Please try again later.</p>';
  }
}

// Display subscription options 
function displaySubscriptionOptions(subscriptions) {
  const container = document.getElementById('species-options');
  container.innerHTML = '';
  
  // Convert subscriptions array to a set of species
  const subscribedSpecies = new Set(subscriptions.map(sub => sub.SPECIES));
  
  availableSpecies.forEach(species => {
    const isChecked = subscribedSpecies.has(species);
    
    const optionDiv = document.createElement('div');
    optionDiv.className = 'species-option';
    
    optionDiv.innerHTML = `
      <label class="checkbox-container">
        <input type="checkbox" name="species" value="${species}" ${isChecked ? 'checked' : ''}>
        <span class="checkmark"></span>
        ${species}
      </label>
    `;
    
    container.appendChild(optionDiv);
  });
  
  document.getElementById('newsletter-container').classList.remove('loading');
}

// Save user's newsletter preferences
async function savePreferences() {
  try {
    const token = localStorage.getItem('Token');
    
    if (!token) {
      console.error('No authentication token found');
      return;
    }
    
    // Get all selected species
    const selectedCheckboxes = document.querySelectorAll('input[name="species"]:checked');
    const selectedSpecies = Array.from(selectedCheckboxes).map(cb => cb.value);
    
    // Save to backend
    const response = await fetch(`${API_URL}/newsletter/update`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        species: selectedSpecies
      })
    });
    
    if (!response.ok) {
      throw new Error('Failed to update subscriptions');
    }
    
   
    const feedbackElement = document.getElementById('feedback-message');
    feedbackElement.textContent = 'Preferences saved successfully!';
    feedbackElement.className = 'success-message';
    
    setTimeout(() => {
      feedbackElement.textContent = '';
      feedbackElement.className = '';
    }, 3000);
  } catch (error) {
    console.error('Error saving preferences:', error);
    const feedbackElement = document.getElementById('feedback-message');
    feedbackElement.textContent = 'Failed to save preferences. Please try again.';
    feedbackElement.className = 'error-message';
  }
}

document.addEventListener('DOMContentLoaded', initialize);