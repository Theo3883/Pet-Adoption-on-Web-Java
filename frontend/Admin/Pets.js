

function initPetsView(animals) {
    const petsSection = document.getElementById('pets');
    if (petsSection && animals) {
        populatePetsTable(animals, petsSection);
        setupPetsSearchAndFilter(animals);
    }
}

// Populate pets table with data
function populatePetsTable(animals, container) {
    const tableContainer = container.querySelector('.table-container');
    if (!tableContainer) return;
    
   
    let table = tableContainer.querySelector('table');
    if (!table) {
        table = document.createElement('table');
        table.className = 'pets-table';
        tableContainer.innerHTML = '';
        tableContainer.appendChild(table);
    }
    
    // Generate table content
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Species</th>
                <th>Breed</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Owner</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            ${animals.map(animal => `
                <tr>
                    <td>${animal.ANIMALID}</td>
                    <td>${animal.NAME}</td>
                    <td>${animal.SPECIES || 'N/A'}</td>
                    <td>${animal.BREED || 'N/A'}</td>
                    <td>${animal.AGE || 'N/A'}</td>
                    <td>${animal.GENDER || 'N/A'}</td>
                    <td>${animal.owner ? animal.owner.name : 'N/A'}</td>
                    <td>
                        <button class="view-pet-btn" data-animal-id="${animal.ANIMALID}">View</button>
                        <button class="delete-pet-btn" data-animal-id="${animal.ANIMALID}">Delete</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    
    // Add event listeners to view buttons
    table.querySelectorAll('.view-pet-btn').forEach(button => {
        button.addEventListener('click', function() {
            const animalId = this.getAttribute('data-animal-id');
            showPetDetails(animals.find(a => a.ANIMALID == animalId));
        });
    });
    
    table.querySelectorAll('.delete-pet-btn').forEach(button => {
        button.addEventListener('click', handleDeleteAnimal);
    });
}

// Set up the pets search and filter functionality
function setupPetsSearchAndFilter(animals) {
    if (!animals || animals.length === 0) return;
    
    const searchInput = document.getElementById('petSearch');
    const speciesFilter = document.getElementById('petSpeciesFilter');
    
    if (!searchInput || !speciesFilter) return;
    
   
    const species = new Set();
    animals.forEach(animal => {
        if (animal.SPECIES) {
            species.add(animal.SPECIES);
        }
    });
    
    
    while (speciesFilter.options.length > 1) {
        speciesFilter.remove(1);
    }
    
    Array.from(species).sort().forEach(speciesName => {
        const option = document.createElement('option');
        option.value = speciesName;
        option.textContent = speciesName;
        speciesFilter.appendChild(option);
    });
    
    
    function filterPets() {
        const searchTerm = searchInput.value.toLowerCase();
        const selectedSpecies = speciesFilter.value;
        
        const filteredAnimals = animals.filter(animal => {
            const nameMatch = animal.NAME && animal.NAME.toLowerCase().includes(searchTerm);
            const breedMatch = animal.BREED && animal.BREED.toLowerCase().includes(searchTerm);
            const speciesMatch = !selectedSpecies || (animal.SPECIES === selectedSpecies);
            
            return (nameMatch || breedMatch) && speciesMatch;
        });
        
        const petsSection = document.getElementById('pets');
        populatePetsTable(filteredAnimals, petsSection);
    }
    
    searchInput.addEventListener('input', filterPets);
    speciesFilter.addEventListener('change', filterPets);
}

// Show pet details in a modal
function showPetDetails(pet) {
    const existingModal = document.getElementById('pet-details-modal');
    if (existingModal) {
        existingModal.remove();
    }
    

    const modalBackdrop = document.createElement('div');
    modalBackdrop.id = 'pet-details-modal';
    modalBackdrop.className = 'modal-backdrop';
    
    // Media display
    let mediaDisplay = '<div class="no-image">No images available</div>';
    if (pet.multimedia && pet.multimedia.length > 0) {
        const mediaItem = pet.multimedia[0];
        let imageUrl;
        
        if (mediaItem.pipeUrl) {
            imageUrl = `http://localhost:3000${mediaItem.pipeUrl}`;
        } else if (mediaItem.URL) {
            imageUrl = mediaItem.URL;
        }
        
        if (imageUrl) {
            mediaDisplay = `<img src="${imageUrl}" alt="${pet.NAME}" class="pet-image">`;
        }
    }
    
    // Format feeding schedule
    let feedingScheduleHtml = '<p>No feeding schedule available.</p>';
    if (pet.feedingSchedule && pet.feedingSchedule.length > 0) {
        feedingScheduleHtml = `
            <ul class="feeding-list">
                ${pet.feedingSchedule.map(schedule => `
                    <li>
                        <strong>Time:</strong> ${formatTime(schedule.FEEDING_TIME)}<br>
                        <strong>Food:</strong> ${schedule.FOOD_TYPE || 'Not specified'}
                        ${schedule.NOTES ? `<br><em>${schedule.NOTES}</em>` : ''}
                    </li>
                `).join('')}
            </ul>
        `;
    }
    
    // Format medical history
    let medicalHistoryHtml = '<p>No medical records available.</p>';
    if (pet.medicalHistory && pet.medicalHistory.length > 0) {
        medicalHistoryHtml = `
            <ul class="medical-list">
                ${pet.medicalHistory.map(record => `
                    <li>
                        <strong>Date:</strong> ${formatDate(record.RECORDDATE)}<br>
                        <strong>Vet #:</strong> ${record.VETNUMBER || 'N/A'}<br>
                        <strong>Description:</strong> ${record.DESCRIPTION || 'No details'}<br>
                        ${record.FIRST_AID_NOTED ? `<strong>First Aid Notes:</strong> ${record.FIRST_AID_NOTED}` : ''}
                    </li>
                `).join('')}
            </ul>
        `;
    }
    
    // Construct modal content
    const modalContent = `
        <div class="modal-content">
            <div class="modal-header">
                <h2>${pet.NAME}</h2>
                <button class="close-modal">&times;</button>
            </div>
            <div class="modal-body">
                <div class="pet-details-grid">
                    <div class="pet-image-container">
                        ${mediaDisplay}
                        <button class="delete-pet-btn modal-delete-btn" data-animal-id="${pet.ANIMALID}">Delete Animal</button>
                    </div>
                    <div class="pet-info">
                        <h3>Basic Information</h3>
                        <p><strong>ID:</strong> ${pet.ANIMALID}</p>
                        <p><strong>Species:</strong> ${pet.SPECIES || 'N/A'}</p>
                        <p><strong>Breed:</strong> ${pet.BREED || 'N/A'}</p>
                        <p><strong>Age:</strong> ${pet.AGE || 'N/A'}</p>
                        <p><strong>Gender:</strong> ${pet.GENDER || 'N/A'}</p>
                        
                        <h3>Owner Information</h3>
                        <p><strong>Name:</strong> ${pet.owner ? pet.owner.name : 'N/A'}</p>
                        <p><strong>Email:</strong> ${pet.owner ? pet.owner.email : 'N/A'}</p>
                        
                        <h3>Feeding Schedule</h3>
                        ${feedingScheduleHtml}
                        
                        <h3>Medical History</h3>
                        ${medicalHistoryHtml}
                    </div>
                </div>
            </div>
        </div>
    `;
    
 
    modalBackdrop.innerHTML = modalContent;
    document.body.appendChild(modalBackdrop);
    
  
    const closeButton = modalBackdrop.querySelector('.close-modal');
    closeButton.addEventListener('click', () => modalBackdrop.remove());
    
    
    const deleteButton = modalBackdrop.querySelector('.modal-delete-btn');
    deleteButton.addEventListener('click', (e) => {
        modalBackdrop.remove(); 
        handleDeleteAnimal(e); 
    });
    
    
    modalBackdrop.addEventListener('click', (e) => {
        if (e.target === modalBackdrop) {
            modalBackdrop.remove();
        }
    });
}

// Handle animal deletion
async function handleDeleteAnimal(event) {
    const animalId = event.target.dataset.animalId;
    
    
    if (!confirm('Are you sure you want to delete this animal? This action cannot be undone.')) {
        return;
    }
    
    try {
        const token = localStorage.getItem('adminToken');
        if (!token) {
            alert('Authentication required');
            return;
        }
        
        const response = await fetch('http://localhost:3000/animals/delete', {
            method: 'DELETE',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ animalId: parseInt(animalId) })
        });

        if (response.ok) {
            
            const cachedData = localStorage.getItem('adminDashboardData');
            if (cachedData) {
                const dashboardData = JSON.parse(cachedData);
                
                
                dashboardData.animals = dashboardData.animals.filter(animal => 
                    animal.ANIMALID !== parseInt(animalId)
                );
                
               
                dashboardData.totalPets = dashboardData.animals.length;
                
                
                window.updateDashboardUI(dashboardData);
                
                
                localStorage.setItem('adminDashboardData', JSON.stringify(dashboardData));
            } else {
            
                window.loadDashboardDataWithCache(true);
            }
            alert('Animal deleted successfully');
        } else {
            const error = await response.json();
            alert(`Error: ${error.error || 'Failed to delete animal'}`);
        }
    } catch (error) {
        console.error('Error deleting animal:', error);
        alert('An error occurred while deleting the animal');
    }
}


function formatTime(timeString) {
    if (!timeString) return 'N/A';
    try {
        
        return timeString.includes(':') ? timeString : 'Time not specified';
    } catch (error) {
        return 'N/A';
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

window.petsModule = {
    initPetsView
};