function initUsersView(users) {
    const usersSection = document.getElementById('users');
    if (usersSection && users) {
        populateUsersTable(users, usersSection);
    }
}

// Populate users table with data
function populateUsersTable(users, container) {
    const tableContainer = container.querySelector('.table-container');
    if (!tableContainer) return;
    
    let table = tableContainer.querySelector('table');
    if (!table) {
        table = document.createElement('table');
        table.className = 'users-table';
        tableContainer.innerHTML = '';
        tableContainer.appendChild(table);
    }
    
    // Generate table content
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Created At</th>
                <th>Pets</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            ${users.map(user => `
                <tr>
                    <td>${user.USERID}</td>
                    <td>${user.FIRSTNAME} ${user.LASTNAME}</td>
                    <td>${user.EMAIL}</td>
                    <td>${user.PHONE || 'N/A'}</td>
                    <td>${formatDate(user.CREATEDAT)}</td>
                    <td>${user.animals ? user.animals.length : 0}</td>
                    <td>
                        <button class="view-btn" data-user-id="${user.USERID}">View</button>
                        <button class="delete-btn" data-user-id="${user.USERID}">Delete</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    
    
    table.querySelectorAll('.view-btn').forEach(button => {
        button.addEventListener('click', function() {
            const userId = this.getAttribute('data-user-id');
            showUserDetails(users.find(u => u.USERID == userId));
        });
    });


    table.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', function() {
            const userId = this.getAttribute('data-user-id');
            handleDeleteUser(userId, users);
        });
    });
}

// Handle user deletion
async function handleDeleteUser(userId, users) {
    if (!confirm('WARNING: Deleting this user will also delete all their pets, addresses, and messages. This action cannot be undone. Are you sure?')) {
        return;
    }
    
    try {
        const token = localStorage.getItem('adminToken');
        if (!token) {
            alert('Authentication required');
            return;
        }
        
        const response = await fetch('http://localhost:3000/users/delete', {
            method: 'DELETE',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ userId: parseInt(userId) })
        });

        if (response.ok) {
            // Update cached data
            const cachedData = localStorage.getItem('adminDashboardData');
            if (cachedData) {
                const dashboardData = JSON.parse(cachedData);
                
                // Remove user from users list
                dashboardData.users = dashboardData.users.filter(user => 
                    user.USERID !== parseInt(userId)
                );
                
                // Remove animals associated with this user
                if (dashboardData.animals) {
                    dashboardData.animals = dashboardData.animals.filter(animal => 
                        animal.USERID !== parseInt(userId)
                    );
                }
                
                
                dashboardData.totalUsers = dashboardData.users.length;
                dashboardData.totalPets = dashboardData.animals ? dashboardData.animals.length : 0;
                
               
                window.updateDashboardUI(dashboardData);
                localStorage.setItem('adminDashboardData', JSON.stringify(dashboardData));
            } else {
             
                window.loadDashboardDataWithCache(true);
            }
            
            alert('User deleted successfully');
        } else {
            const error = await response.json();
            alert(`Error: ${error.error || 'Failed to delete user'}`);
        }
    } catch (error) {
        console.error('Error deleting user:', error);
        alert('An error occurred while deleting the user');
    }
}

// Show user details in a modal 
function showUserDetails(user) {
    
    const existingModal = document.getElementById('user-details-modal');
    if (existingModal) {
        existingModal.remove();
    }
    
    
    const modalBackdrop = document.createElement('div');
    modalBackdrop.id = 'user-details-modal';
    modalBackdrop.className = 'modal-backdrop';
    
    // Format animals list
    let animalsHtml = '<p>No pets registered.</p>';
    if (user.animals && user.animals.length > 0) {
        animalsHtml = `
            <div class="user-pets-list">
                <table class="pets-mini-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Species</th>
                            <th>Breed</th>
                            <th>Age</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${user.animals.map(animal => `
                            <tr>
                                <td>${animal.NAME}</td>
                                <td>${animal.SPECIES || 'N/A'}</td>
                                <td>${animal.BREED || 'N/A'}</td>
                                <td>${animal.AGE || 'N/A'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }
    
    // Construct modal content
    const modalContent = `
        <div class="modal-content">
            <div class="modal-header">
                <h2>${user.FIRSTNAME} ${user.LASTNAME}</h2>
                <button class="close-modal">&times;</button>
            </div>
            <div class="modal-body">
                <div class="user-details-grid">
                    <div class="user-info">
                        <h3>User Information</h3>
                        <p><strong>ID:</strong> ${user.USERID}</p>
                        <p><strong>Email:</strong> ${user.EMAIL}</p>
                        <p><strong>Phone:</strong> ${user.PHONE || 'N/A'}</p>
                        <p><strong>Joined:</strong> ${formatDate(user.CREATEDAT)}</p>
                        
                        <h3>Address</h3>
                        <p>${user.address ? 
                            `${user.address.STREET || ''}<br>
                            ${user.address.CITY || ''} ${user.address.STATE || ''} ${user.address.ZIPCODE || ''}<br>
                            ${user.address.COUNTRY || ''}` : 'No address information'}</p>
                        
                        <h3>Pets</h3>
                        ${animalsHtml}
                    </div>
                </div>
            </div>
        </div>
    `;
    
   
    modalBackdrop.innerHTML = modalContent;
    document.body.appendChild(modalBackdrop);
    
 
    const closeButton = modalBackdrop.querySelector('.close-modal');
    closeButton.addEventListener('click', () => modalBackdrop.remove());
    
    
    modalBackdrop.addEventListener('click', (e) => {
        if (e.target === modalBackdrop) {
            modalBackdrop.remove();
        }
    });
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

window.usersModule = {
    initUsersView
};