import Sidebar from '../SideBar/Sidebar.js';
import { showAnimalDetailsPopup } from '../AnimalCard/AnimalCard.js';
import { showLoading, hideLoading } from '../utils/loadingUtils.js';
import { 
  getOptimalImageSize, 
  generateSrcSet, 
  getResponsiveImageUrl, 
  generatePlaceholder 
} from '../utils/imageOptimizer.js';
import { addPreconnect } from '../utils/performanceUtils.js';

const API_URL = 'http://localhost:3000';
const token = localStorage.getItem('Token');
let isMobile = window.innerWidth < 768;

// Listen for viewport changes
window.addEventListener('resize', () => {
  const wasMobile = isMobile;
  isMobile = window.innerWidth < 768;
});

async function initialize() {
  addPreconnect('http://localhost:3000');
  
  // Render sidebar
  document.getElementById('sidebar-container').innerHTML = Sidebar.render('popular');
  new Sidebar('popular');

  await fetchTopAnimals();
}

async function fetchTopAnimals() {
  try {
    showLoading('Loading popular animals...');
    
    const userString = localStorage.getItem('User');
    if (!userString) {
      throw new Error('User is missing. Please log in again.');
    }
    const user = JSON.parse(userString);
    const userId = user.id;

    const response = await fetch(`${API_URL}/animals/top-by-city?userId=${userId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch top animals');
    }

    const topAnimals = await response.json();
    displayAnimals(topAnimals);
  } catch (error) {
    console.error('Error fetching top animals:', error);
    document.getElementById('animal-cards-container').innerHTML =
      '<div class="error">Failed to load top animals. Please try again later.</div>';
  } finally {
    hideLoading();
  }
}

function displayAnimals(animals) {
  const container = document.getElementById('animal-cards-container');
  container.innerHTML = '';

  if (animals.length === 0) {
    container.innerHTML = '<div class="no-results">No popular animals found in your area.</div>';
    return;
  }

  // On mobile, limit initial batch to fewer items
  const initialBatchSize = isMobile ? 4 : 6;
  const initialBatch = animals.slice(0, initialBatchSize);
  const remainingBatch = animals.slice(initialBatchSize);
  
  // Render first batch immediately with high priority for first item
  initialBatch.forEach((animal, index) => {
    const priority = index === 0 ? 'high' : 'auto';
    renderAnimalCard(animal, container, false, priority);
  });
  
  // Render remaining animals with lazy loading
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

  // Create image 
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
    //  For above-the-fold images, load after a tiny delay 
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

// Set up intersection observer for lazy loading
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
    showLoading('Loading animal details...');
    
    const response = await fetch(`${API_URL}/animals/details`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ animalId }),
    });

    if (!response.ok) {
      throw new Error('Failed to fetch animal details');
    }

    const animalDetails = await response.json();
    showAnimalDetailsPopup(animalDetails);
  } catch (error) {
    console.error('Error fetching animal details:', error);
  } finally {
    hideLoading();
  }
}

initialize();