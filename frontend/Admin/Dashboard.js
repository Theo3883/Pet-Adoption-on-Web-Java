import { showLoading, hideLoading } from '../utils/loadingUtils.js';

document.addEventListener('DOMContentLoaded', function() {
    // loading spinner
    const linkElement = document.createElement("link");
    linkElement.rel = "stylesheet";
    linkElement.href = "../utils/loadingUtils.css";
    document.head.appendChild(linkElement);
    
    const token = localStorage.getItem('adminToken');
    if (!token) {
        window.location.href = 'Admin.html';
        return;
    }
    
    try {
        const decodedToken = jwt_decode(token);
        
        if (!decodedToken.isAdmin) {
            console.error('Token does not contain admin privileges');
            localStorage.removeItem('adminToken');
            window.location.href = 'Admin.html';
            return;
        }
        
        document.getElementById('adminEmail').textContent = decodedToken.email || 'Admin';
        
        // Navigation between sections
        const navLinks = document.querySelectorAll('.nav-menu a');
        const sections = document.querySelectorAll('.content-section');
        
        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                
                // Remove active class from all links and sections
                navLinks.forEach(l => l.parentElement.classList.remove('active'));
                sections.forEach(s => s.classList.remove('active'));
                
                // Add active class to current link and section
                this.parentElement.classList.add('active');
                const targetSection = document.getElementById(this.dataset.section);
                if (targetSection) {
                    targetSection.classList.add('active');
                }
            });
        });
        
        document.getElementById('logoutBtn').addEventListener('click', function() {
            localStorage.removeItem('adminToken');
            localStorage.removeItem('adminDashboardData');
            localStorage.removeItem('adminDashboardLastFetch');
            window.location.href = 'Admin.html';
        });
        
        
        loadDashboardDataWithCache();
        
    } catch (error) {
        console.error('Error decoding token:', error);
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminDashboardData');
        localStorage.removeItem('adminDashboardLastFetch');
        window.location.href = 'Admin.html';
    }
});

// Function to load dashboard
async function loadDashboardDataWithCache(forceRefresh = false) {
    try {
        const cacheMaxAge = 3 * 60 * 1000; 
        const now = new Date().getTime();
        const lastFetch = localStorage.getItem('adminDashboardLastFetch');
        const cachedData = localStorage.getItem('adminDashboardData');
        
        if (!forceRefresh && cachedData && lastFetch && (now - parseInt(lastFetch) < cacheMaxAge)) {
            console.log('Using cached dashboard data');
            const dashboardData = JSON.parse(cachedData);
            updateDashboardUI(dashboardData);
            return;
        }
        
        showLoading('Loading dashboard data...');
        
        const token = localStorage.getItem('adminToken');
        if (!token) {
            throw new Error('No authentication token');
        }
        
        const response = await fetch('http://localhost:3000/users/all/details', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch dashboard data');
        }
        
        const usersData = await response.json();
        
        // Process the data to get summary information
        const dashboardData = processApiData(usersData);
        
        updateDashboardUI(dashboardData);
        
        localStorage.setItem('adminDashboardData', JSON.stringify(dashboardData));
        localStorage.setItem('adminDashboardLastFetch', now.toString());
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        
        const cachedData = localStorage.getItem('adminDashboardData');
        if (cachedData) {
            console.log('Using expired cache as fallback');
            const dashboardData = JSON.parse(cachedData);
            updateDashboardUI(dashboardData);
        } else {
            // Use mock data if no cache is available
            updateDashboardUI({
                totalUsers: 0,
                totalPets: 0,
                users: []
            });
        }
    } finally {
        hideLoading();
    }
}

// Process API data to get summary information
function processApiData(usersData) {
    let totalUsers = usersData.length;
    let totalPets = 0;
    let allAnimals = [];
    
    // Calculate total pets and extract all animals
    usersData.forEach(user => {
        if (user.animals && Array.isArray(user.animals)) {
            totalPets += user.animals.length;
            
            // Add owner information to each animal for reference
            const animalsWithOwner = user.animals.map(animal => ({
                ...animal,
                owner: {
                    id: user.USERID,
                    name: `${user.FIRSTNAME} ${user.LASTNAME || ''}`.trim(),
                    email: user.EMAIL
                }
            }));
            
            allAnimals = [...allAnimals, ...animalsWithOwner];
        }
    });
    
    return {
        totalUsers,
        totalPets,
        users: usersData, 
        animals: allAnimals 
    };
}

// Update the dashboard UI with the data
function updateDashboardUI(data) {
    // Update statistics
    document.getElementById('totalUsers').textContent = data.totalUsers;
    document.getElementById('totalPets').textContent = data.totalPets;
    
    // Initialize charts
    if (window.chartModule && typeof window.chartModule.initCharts === 'function') {
        window.chartModule.initCharts(data);
    }
    
    // Initialize users view
    if (window.usersModule && typeof window.usersModule.initUsersView === 'function') {
        window.usersModule.initUsersView(data.users);
    }
    
    // Initialize pets view
    if (window.petsModule && typeof window.petsModule.initPetsView === 'function') {
        window.petsModule.initPetsView(data.animals);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const refreshButton = document.getElementById('refreshData');
    if (refreshButton) {
        refreshButton.addEventListener('click', function() {
            loadDashboardDataWithCache(true); // Force refresh
        });
    }
});

// Make functions available for module scripts
window.updateDashboardUI = updateDashboardUI;
window.loadDashboardDataWithCache = loadDashboardDataWithCache;