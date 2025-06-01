import Sidebar from '../SideBar/Sidebar.js';
import { showAnimalDetailsPopup } from '../AnimalCard/AnimalCard.js';
import { requireAuth } from '../utils/authUtils.js';
import { showLoading, hideLoading } from '../utils/loadingUtils.js';
import { setupLazyLoading, addPreconnect } from '../utils/performanceUtils.js';
import { initializeSession } from '../utils/sessionUtils.js';
import { 
  getOptimalImageSize, 
  generateSrcSet, 
  getResponsiveImageUrl, 
  generatePlaceholder 
} from '../utils/imageOptimizer.js';

const API_URL = 'http://localhost:3000';
const token = localStorage.getItem('Token');
let animals = [];
const uniqueSpecies = [];
let filteredAnimals = [];
let user;
let isMobile = window.innerWidth < 768;

window.addEventListener('resize', () => {
  const wasMobile = isMobile;
  isMobile = window.innerWidth < 768;
  
  if (wasMobile !== isMobile) {
    console.log("Viewport changed to:", isMobile ? "mobile" : "desktop");
  }
});

async function initialize() {
  addPreconnect('http://localhost:3000');
  
  user = requireAuth();
  if (!user) return;
  
  await initializeSession();
  
  const sidebarContainer = document.getElementById('sidebar-container');
  if (sidebarContainer) {
    sidebarContainer.innerHTML = Sidebar.render('home');
    
    requestAnimationFrame(() => {
      const sidebar = new Sidebar('home');
      window.sidebarInstance = sidebar;
        
      setTimeout(() => {
        if (window.sidebarInstance && !window.sidebarInstance.isMobile) {
          window.sidebarInstance.enforceSidebarBehavior();
        }
      }, 100);
    });
  }
  
  document.body.style.overflow = '';
  document.body.style.position = '';
  document.documentElement.style.overflow = '';
  document.documentElement.style.position = '';
  
  const mainContent = document.querySelector('.main-content');
  if (mainContent) {
    mainContent.style.overflowY = 'auto';
    mainContent.style.height = 'auto';
  }
  
  await fetchAnimals();
  
  renderSpeciesFilters();
  
  setupLazyLoading();
  
  setupMobileFilters();
  
  setTimeout(ensureSidebarFiltersPopulated, 300);
}

function setupMobileFilters() {
  const filterToggle = document.getElementById('filter-toggle');
  const filterDrawer = document.getElementById('filter-drawer');
  const closeFilter = document.getElementById('close-filter');
  const filterBackdrop = document.getElementById('filter-backdrop');
  
  if (filterToggle && filterDrawer && closeFilter && filterBackdrop) {
    filterToggle.addEventListener('click', () => {
      filterDrawer.classList.add('open');
      filterBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    });
    
    closeFilter.addEventListener('click', () => {
      filterDrawer.classList.remove('open');
      filterBackdrop.classList.remove('open');
      document.body.style.overflow = '';
      document.body.style.overflowY = 'auto';
      document.documentElement.style.overflow = '';
      document.documentElement.style.overflowY = 'auto';
    });
    
    filterBackdrop.addEventListener('click', () => {
      filterDrawer.classList.remove('open');
      filterBackdrop.classList.remove('open');
      document.body.style.overflow = ''; 
      document.body.style.overflowY = 'auto';
      document.documentElement.style.overflow = '';
      document.documentElement.style.overflowY = 'auto';
    });
  }
}

async function fetchAnimals() {
  try {
    showLoading('Loading animals...');
    
    const container = document.getElementById('animal-cards-container');
    container.innerHTML = '';
    
    const response = await fetch(`${API_URL}/animals/all`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch animals');
    }
    
    animals = await response.json();
    
    try {
      localStorage.setItem('cachedAnimals', JSON.stringify(animals));
    } catch (e) {
      console.warn('Could not cache animals:', e);
    }
    
    animals.forEach(animal => {
      if (animal.SPECIES && !uniqueSpecies.includes(animal.SPECIES)) {
        uniqueSpecies.push(animal.SPECIES);
      }
    });
    
    filteredAnimals = [...animals];
    displayAnimals(filteredAnimals);
  } catch (error) {
    console.error('Error fetching animals:', error);
    document.getElementById('animal-cards-container').innerHTML = `
      <div class="error-message">Failed to load animals. Please try again later.</div>
    `;
  } finally {
    hideLoading();
  }
}

function renderSpeciesFilters() {
  renderFilterContainer(document.getElementById('species-filters'));
  
  renderFilterContainer(document.getElementById('mobile-species-filters'));
  
  renderFilterContainer(document.getElementById('sidebar-species-filters'));
  
  if (!document.getElementById('sidebar-species-filters')) {
    const sidebarFilters = document.querySelector('.sidebar .filter-options');
    if (sidebarFilters) {
      renderFilterContainer(sidebarFilters);
    }
  }
}

function renderFilterContainer(filtersContainer) {
  if (!filtersContainer) {
    console.log("Filter container not found");
    return;
  }
  
  console.log("Rendering filters for container:", filtersContainer.id || "unnamed container");
  
  filtersContainer.innerHTML = ''; 
  
  if (filtersContainer.classList.contains('filter-options') && filtersContainer.querySelector('.loader')) {
    filtersContainer.querySelector('.loader').remove();
  }
  
  uniqueSpecies.forEach(species => {
    const filterOption = document.createElement('div');
    filterOption.className = 'filter-option';
    
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    
    let containerId = filtersContainer.id || 'unknown';
    if (containerId === 'sidebar-species-filters') {
      containerId = 'sidebar';
    }
    
    checkbox.id = `species-${species}-${containerId}`;
    checkbox.value = species;
    
    // Add change event listener
    checkbox.addEventListener('change', (e) => {
      // Sync all checkboxes with the same species
      const speciesCheckboxes = document.querySelectorAll(`input[type="checkbox"][value="${species}"]`);
      speciesCheckboxes.forEach(cb => {
        if (cb !== e.target) {
          cb.checked = e.target.checked;
        }
      });
      
      handleSpeciesFilterChange(e);
      
      // close it after selection on mobile
      if (isMobile && filtersContainer.id === 'mobile-species-filters') {
        const filterDrawer = document.getElementById('filter-drawer');
        const filterBackdrop = document.getElementById('filter-backdrop');
        if (filterDrawer && filterDrawer.classList.contains('open')) {
          filterDrawer.classList.remove('open');
          if (filterBackdrop) filterBackdrop.classList.remove('open');
          document.body.style.overflow = '';
        }
      }
    });
    
    const label = document.createElement('label');
    label.htmlFor = checkbox.id;
    label.textContent = species;
    
    filterOption.appendChild(checkbox);
    filterOption.appendChild(label);
    filtersContainer.appendChild(filterOption);
  });
  
  // If no species to display, show a message
  if (uniqueSpecies.length === 0) {
    console.log("No species available for filters");
    const noSpecies = document.createElement('div');
    noSpecies.textContent = 'No species available';
    noSpecies.style.color = '#6b7280';
    noSpecies.style.fontStyle = 'italic';
    filtersContainer.appendChild(noSpecies);
  } else {
    console.log(`Added ${uniqueSpecies.length} species filters to container`);
  }
}

function handleSpeciesFilterChange(event) {
  const species = event.target.value;
  const checkedSpecies = [];
  
  const allCheckboxes = document.querySelectorAll('input[type="checkbox"][id^="species-"]');
  
  allCheckboxes.forEach(checkbox => {
    if (checkbox.checked && !checkedSpecies.includes(checkbox.value)) {
      checkedSpecies.push(checkbox.value);
    }
  });
  
  if (checkedSpecies.length > 0) {

    filteredAnimals = animals.filter(animal => checkedSpecies.includes(animal.SPECIES));

  } else {

    filteredAnimals = [...animals];

  }
  
  displayAnimals(filteredAnimals);
}

function displayAnimals(animals) {
  const container = document.getElementById('animal-cards-container');
  container.innerHTML = '';

  if (animals.length === 0) {
    container.innerHTML = '<div class="no-results">No animals match your filters</div>';
    return;
  }

  // On mobile, limit initial batch to fewer items
  const initialBatchSize = isMobile ? 4 : 6;
  const initialBatch = animals.slice(0, initialBatchSize);
  const remainingBatch = animals.slice(initialBatchSize);
  
  // Render first batch immediately
  initialBatch.forEach((animal, index) => {
    const priority = index === 0 ? 'high' : 'auto';
    renderAnimalCard(animal, container, false, priority);
  });
  
  // Render remaining animals after a slight delay
  if (remainingBatch.length > 0) {
    setTimeout(() => {
      remainingBatch.forEach(animal => renderAnimalCard(animal, container, true, 'low'));
    }, 100); 
  }
}

function renderAnimalCard(animal, container, lazyLoad = false, priority = 'auto') {
  const card = document.createElement('div');
  card.className = 'card';
  
  const imgContainer = document.createElement('div');
  imgContainer.className = 'card-img-container';
  
  // Find appropriate image
  let imageSource = 'https://via.placeholder.com/400x300?text=No+Image';
  let placeholder = generatePlaceholder(animal);
  
  if (animal.multimedia && animal.multimedia.length > 0) {
    const media = animal.multimedia[0];
    if (media.pipeUrl) {
      // Get optimized responsive image URL
      const optimalWidth = isMobile ? 
        Math.min(window.innerWidth - 30, 600) : 
        getOptimalImageSize();
        
      imageSource = getResponsiveImageUrl(`${API_URL}${media.pipeUrl}`, {
        width: optimalWidth,
        quality: isMobile ? 80 : 85 // Lower quality for mobile to save data
      });
    } else if (media.fileData && media.mimeType) {
      imageSource = `data:${media.mimeType};base64,${media.fileData}`;
    } else if (media.URL) {
      imageSource = media.URL;
    }
  }

  // Create image with proper loading strategy
  const img = document.createElement('img');
  img.alt = animal.NAME || 'Pet';
  img.classList.add('loading');
  
  // Set loading priority
  if (priority === 'high') {
    img.setAttribute('fetchpriority', 'high');
    img.loading = 'eager';
  } else if (priority === 'low') {
    img.loading = 'lazy';
  }
  
  img.src = placeholder;
  
  // If lazy loading, use dataset
  if (lazyLoad) {
    img.dataset.src = imageSource;
    img.classList.add('lazy');
  } else {
    // For above-the-fold images, load after a tiny delay
    setTimeout(() => {
      img.src = imageSource;
      
      // Create srcset for responsive images
      const srcset = generateSrcSet(imageSource);
      if (srcset !== imageSource) {
        img.srcset = srcset;
        
        img.sizes = isMobile ? 
          '(max-width: 640px) 95vw, 45vw' : 
          '(max-width: 768px) 45vw, (max-width: 1024px) 30vw, 25vw';
      }
      
      // Handle image load completion
      img.onload = () => {
        img.classList.remove('loading');
        img.classList.add('loaded');
      };
    }, 10);
  }
  
  // Build card structure
  imgContainer.appendChild(img);
  card.appendChild(imgContainer);
  
  // Add the content div
  const contentDiv = document.createElement('div');
  contentDiv.className = 'card-content';
  contentDiv.innerHTML = `
    <h2>${animal.NAME || 'Unknown'}</h2>
    <p>Breed: ${animal.BREED || 'Unknown'}</p>
    <p>Species: ${animal.SPECIES || 'Unknown'}</p>
  `;
  
  card.appendChild(contentDiv);
  card.addEventListener('click', () => openAnimalDetailsPopup(animal.ANIMALID));
  container.appendChild(card);
  
  // Initialize lazy loading if needed
  if (lazyLoad && img.classList.contains('lazy')) {
    observeImage(img);
  }
}

// Update the image observer to handle the loading classes
const imageObserver = new IntersectionObserver((entries, observer) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const img = entry.target;
      if (img.dataset.src) {
        // Set the actual image source
        img.src = img.dataset.src;
        
        // Add srcset if available
        const srcset = generateSrcSet(img.dataset.src);
        if (srcset !== img.dataset.src) {
          img.srcset = srcset;
          img.sizes = isMobile ? 
            '(max-width: 640px) 95vw, 45vw' : 
            '(max-width: 768px) 45vw, (max-width: 1024px) 30vw, 25vw';
        }
        
        // Handle image load completion
        img.onload = () => {
          img.classList.remove('loading');
          img.classList.add('loaded');
          img.classList.remove('lazy');
        };
        
        imageObserver.unobserve(img);
      }
    }
  });
});

function observeImage(img) {
  imageObserver.observe(img);
}

async function openAnimalDetailsPopup(animalId) {
  try {
    console.log('Opening details for animal ID:', animalId, 'Type:', typeof animalId);
    showLoading('Loading animal details...');
    
    const requestBody = { animalId: animalId };
    console.log('Request body:', requestBody);
    console.log('Request body JSON:', JSON.stringify(requestBody));
    
    const response = await fetch(`${API_URL}/animals/details`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestBody),
    });

    console.log('Response status:', response.status);
    console.log('Response ok:', response.ok);
    console.log('Response headers:', Object.fromEntries(response.headers.entries()));

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Error response body:', errorText);
      
      let errorMessage = `Server error: ${response.status}`;
      try {
        const errorData = JSON.parse(errorText);
        errorMessage = errorData.error || errorMessage;
        console.error('Parsed error data:', errorData);
      } catch (e) {
        // If response is not JSON, use the text as is
        errorMessage = errorText || errorMessage;
        console.error('Error response is not JSON:', errorText);
      }
      
      throw new Error(errorMessage);
    }

    const animalDetails = await response.json();
    console.log('Animal details received:', animalDetails);
    console.log('Animal details structure:', {
      hasAnimal: !!animalDetails.animal,
      hasMultimedia: !!animalDetails.multimedia,
      hasFeedingSchedule: !!animalDetails.feedingSchedule,
      hasMedicalHistory: !!animalDetails.medicalHistory,
      hasOwner: !!animalDetails.owner,
      hasAddress: !!animalDetails.address,
      hasRelations: !!animalDetails.relations
    });
    
    if (animalDetails.animal) {
      console.log('Animal object:', animalDetails.animal);
    }
    
    showAnimalDetailsPopup(animalDetails);
  } catch (error) {
    console.error('Error fetching animal details:', error);
    alert(`Error loading animal details: ${error.message}`);
  } finally {
    hideLoading();
  }
}

function ensureSidebarFiltersPopulated() {
  const sidebarFilters = document.getElementById('sidebar-species-filters');
  if (sidebarFilters && sidebarFilters.children.length <= 1) {
    renderFilterContainer(sidebarFilters);
  }
}
initialize();