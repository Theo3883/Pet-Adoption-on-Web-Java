import userModel from "../models/User.js";
import { requireAuth } from "../utils/authUtils.js";
import { showLoading, hideLoading } from "../utils/loadingUtils.js";
import { createElement, sanitizeInput, getCsrfToken } from "../utils/securityUtils.js";

const API_URL = "http://localhost:3000";
const token = localStorage.getItem("Token");
let user;

// Initialize the page
document.addEventListener("DOMContentLoaded", function () {
  user = requireAuth();
  if (!user) return;

  const feedingContainer = document.getElementById("feeding-schedule-container");
  if (
    feedingContainer &&
    feedingContainer.querySelectorAll(".feeding-schedule-entry").length === 0
  ) {
    console.log("No feeding entries found, adding initial entry");
    addInitialFeedingEntry();
  }

  updateDeleteButtons();
  
  initializeEventListeners();

  const photoInput = document.getElementById("photo");
  if (photoInput) {
    photoInput.addEventListener("change", function () {
      if (this.files && this.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
          const img = document.createElement('img');
          img.onload = function() {
            const previewContainer = document.getElementById("photo-preview");
            if (previewContainer) {
              // Clear container safely
              while (previewContainer.firstChild) {
                previewContainer.removeChild(previewContainer.firstChild);
              }
              previewContainer.appendChild(img);
              previewContainer.style.display = "block";
            }
          };
          img.style.maxWidth = "100%";
          img.style.maxHeight = "200px";
          img.src = e.target.result;
        };
        reader.readAsDataURL(this.files[0]);
      }
    });
  }

  // Add multimedia entry by default
  addMultimediaEntry();
});

function initializeEventListeners() {
  // Back to home button
  const backBtn = document.getElementById('backToHomeBtn');
  if (backBtn) {
    backBtn.addEventListener('click', redirectToHome);
  }
  
  // Form submission
  const publishForm = document.getElementById('publishForm');
  if (publishForm) {
    publishForm.addEventListener('submit', submitPublishForm);
  }
  
  // Species selection for breed options
  const speciesSelect = document.getElementById('species');
  if (speciesSelect) {
    speciesSelect.addEventListener('change', updateBreedOptions);
  }
  
  // Add event listeners for add/delete buttons
  const addFeedingBtn = document.getElementById('addFeedingScheduleBtn');
  if (addFeedingBtn) {
    addFeedingBtn.addEventListener('click', addFeedingScheduleEntry);
  }
  
  const addMedicalBtn = document.getElementById('addMedicalHistoryBtn');
  if (addMedicalBtn) {
    addMedicalBtn.addEventListener('click', addMedicalHistoryEntry);
  }
  
  const addMultimediaBtn = document.getElementById('addMultimediaBtn');
  if (addMultimediaBtn) {
    addMultimediaBtn.addEventListener('click', addMultimediaEntry);
  }
  
 
  updateDeleteButtons();
}

function updateDeleteButtons() {
  // Feeding schedule delete buttons
  document.querySelectorAll('.feeding-schedule-entry .delete-entry-btn').forEach(button => {
    button.removeEventListener('click', deleteFeedingScheduleEntry);
    button.addEventListener('click', function() {
      deleteFeedingScheduleEntry(this);
    });
  });
  
  // Medical history delete buttons
  document.querySelectorAll('.medical-history-entry .delete-entry-btn').forEach(button => {
    button.removeEventListener('click', deleteMedicalHistoryEntry);
    button.addEventListener('click', function() {
      deleteMedicalHistoryEntry(this);
    });
  });
  
  // Multimedia delete buttons
  document.querySelectorAll('.multimedia-entry .delete-entry-btn').forEach(button => {
    button.removeEventListener('click', deleteMultimediaEntry);
    button.addEventListener('click', function() {
      deleteMultimediaEntry(this);
    });
  });
  
  // Hide/show delete buttons based on entry position
  const entries = document.querySelectorAll(".feeding-schedule-entry");
  entries.forEach((entry, index) => {
    const deleteButton = entry.querySelector(".delete-entry-btn");
    if (index === 0 && entries.length === 1) {
      deleteButton.style.display = "none";
    } else {
      deleteButton.style.display = "inline-block";
    }
  });
}

// Add initial feeding entry
function addInitialFeedingEntry() {
  const container = document.getElementById("feeding-schedule-container");
  const initialEntry = createElement('div', { className: 'feeding-schedule-entry' });
  
  // Create form group for feeding time
  const timeGroup = createElement('div', { className: 'form-group' });
  timeGroup.appendChild(createElement('label', { for: 'feedingTime' }, 'Ora Hrănire'));
  timeGroup.appendChild(createElement('input', { type: 'time', name: 'feedingTime', required: true }));
  initialEntry.appendChild(timeGroup);
  
  // Create form group for food type
  const foodGroup = createElement('div', { className: 'form-group full-width' });
  foodGroup.appendChild(createElement('label', { for: 'foodType' }, 'Tip de Hrană'));
  foodGroup.appendChild(createElement('textarea', { 
    name: 'foodType', 
    placeholder: 'Introdu tipurile de hrană', 
    rows: '2'
  }));
  initialEntry.appendChild(foodGroup);
  
  const deleteButton = createElement('button', { 
    type: 'button', 
    className: 'btn delete-entry-btn'
  }, 'Șterge');
  deleteButton.addEventListener('click', function() {
    deleteFeedingScheduleEntry(this);
  });
  initialEntry.appendChild(deleteButton);
  
  container.appendChild(initialEntry);
  updateDeleteButtons();
}

// Medical history entry functions 
function addMedicalHistoryEntry() {
  const container = document.getElementById("medical-history-container");
  const newEntry = createElement('div', { className: 'medical-history-entry' });
  
  const dateGroup = createElement('div', { className: 'form-group' });
  dateGroup.appendChild(createElement('label', { for: 'recordDate' }, 'Dată Înregistrare'));
  dateGroup.appendChild(createElement('input', { 
    type: 'date', 
    name: 'recordDate', 
    value: '2025-04-14', 
    required: true 
  }));
  newEntry.appendChild(dateGroup);
  
  // Create description textarea
  const descGroup = createElement('div', { className: 'form-group full-width' });
  descGroup.appendChild(createElement('label', { for: 'description' }, 'Descriere'));
  descGroup.appendChild(createElement('textarea', { 
    name: 'description', 
    placeholder: 'Introdu detalii despre vaccinuri, deparazitări etc.', 
    rows: '3' 
  }));
  newEntry.appendChild(descGroup);
  
  // Create delete button with event listener
  const deleteButton = createElement('button', { 
    type: 'button', 
    className: 'btn delete-entry-btn'
  }, 'Șterge');
  deleteButton.addEventListener('click', function() {
    deleteMedicalHistoryEntry(this);
  });
  newEntry.appendChild(deleteButton);
  
  container.appendChild(newEntry);
  updateDeleteButtons();
}

function deleteMedicalHistoryEntry(button) {
  const entry = button.parentElement;
  entry.remove();
}

// Multimedia entry functions 
function addMultimediaEntry() {
  const container = document.getElementById("multimedia-container");
  const newEntry = createElement('div', { className: 'multimedia-entry' });
  
  // Create media type select
  const typeGroup = createElement('div', { className: 'form-group' });
  typeGroup.appendChild(createElement('label', { for: 'mediaType' }, 'Tip Media'));
  
  const select = createElement('select', { name: 'mediaType', required: true });
  ['photo', 'video', 'audio'].forEach(type => {
    const option = createElement('option', { value: type }, 
      type === 'photo' ? 'Poză' : type === 'video' ? 'Video' : 'Audio');
    select.appendChild(option);
  });
  
  typeGroup.appendChild(select);
  newEntry.appendChild(typeGroup);
  
  const fileGroup = createElement('div', { className: 'form-group' });
  fileGroup.appendChild(createElement('label', { for: 'file' }, 'Încarcă Fișier'));
  fileGroup.appendChild(createElement('input', { 
    type: 'file', 
    name: 'file', 
    accept: 'image/*,video/*,audio/*', 
    required: true 
  }));
  newEntry.appendChild(fileGroup);
  
  // Create description textarea
  const descGroup = createElement('div', { className: 'form-group full-width' });
  descGroup.appendChild(createElement('label', { for: 'description' }, 'Descriere'));
  descGroup.appendChild(createElement('textarea', { 
    name: 'description', 
    placeholder: 'Introdu descrierea', 
    rows: '3' 
  }));
  newEntry.appendChild(descGroup);
  
  const deleteButton = createElement('button', { 
    type: 'button', 
    className: 'btn delete-entry-btn'
  }, 'Șterge');
  deleteButton.addEventListener('click', function() {
    deleteMultimediaEntry(this);
  });
  newEntry.appendChild(deleteButton);
  
  container.appendChild(newEntry);
  updateDeleteButtons();
}

function deleteMultimediaEntry(button) {
  const entry = button.parentElement;
  entry.remove();
}

// Navigation function
function redirectToHome() {
  window.location.href = "../Home/Home.html";
}

// Breed selection options
function updateBreedOptions() {
  const species = document.getElementById("species").value;
  const breedSelect = document.getElementById("breed");

  breedSelect.innerHTML = '<option value="">Selectează o rasă</option>';

  const breeds = {
    Câine: ["Labrador", "Golden Retriever", "Ciobănesc German", "Bulldog"],
    Pisică: ["Siameză", "Persană", "Maine Coon", "Bengaleză"],
    Papagal: ["Ara", "Cacadu", "Peruș", "Nimfă"],
    Hamster: ["Sirian", "Pitic Alb", "Roborovski", "Chinezesc"],
    Iepure: ["Olandez", "Cap de Leu", "Rex", "Fluture"],
    Pește: ["Guppy", "Neon", "Betta", "Scalar"],
    "Broască Țestoasă": [
      "Țestoasă de Florida",
      "Țestoasă Grecească",
      "Țestoasă de Apă",
    ],
    Șarpe: ["Python Regal", "Boa Constrictor", "Șarpe de Porumb"],
    "Porcușor de Guineea": ["Abisinian", "American", "Peruvian", "Texel"],
  };

  if (breeds[species]) {
    breeds[species].forEach((breed) => {
      const option = document.createElement("option");
      option.value = breed;
      option.textContent = breed;
      breedSelect.appendChild(option);
    });
  }
}

// Add feeding schedule entry
function addFeedingScheduleEntry() {
  const container = document.getElementById("feeding-schedule-container");
  const newEntry = createElement('div', { className: 'feeding-schedule-entry' });
  
  // Create time input
  const timeGroup = createElement('div', { className: 'form-group' });
  timeGroup.appendChild(createElement('label', { for: 'feedingTime' }, 'Ora Hrănire'));
  timeGroup.appendChild(createElement('input', { 
    type: 'time', 
    name: 'feedingTime', 
    required: true 
  }));
  newEntry.appendChild(timeGroup);
  
  // Create food type textarea
  const foodGroup = createElement('div', { className: 'form-group full-width' });
  foodGroup.appendChild(createElement('label', { for: 'foodType' }, 'Tip de Hrană'));
  foodGroup.appendChild(createElement('textarea', { 
    name: 'foodType', 
    placeholder: 'Introdu tipurile de hrană', 
    rows: '2' 
  }));
  newEntry.appendChild(foodGroup);
  
  // Create delete button with event listener
  const deleteButton = createElement('button', { 
    type: 'button', 
    className: 'btn delete-entry-btn'
  }, 'Șterge');
  deleteButton.addEventListener('click', function() {
    deleteFeedingScheduleEntry(this);
  });
  newEntry.appendChild(deleteButton);
  
  container.appendChild(newEntry);
  updateDeleteButtons();
}

// Delete feeding schedule entry
function deleteFeedingScheduleEntry(button) {
  const entry = button.parentElement;
  const container = document.getElementById("feeding-schedule-container");
  
  if (container.querySelectorAll(".feeding-schedule-entry").length > 1) {
    entry.remove();
  } else {
    alert("Trebuie să existe cel puțin o înregistrare pentru programul de hrănire.");
  }
  
  updateDeleteButtons();
}

// Function to verify animal exists by checking animal details endpoint
async function verifyAnimalExists(animalId, maxRetryTimeMs = 60000) {
  console.log(`Verifying animal ${animalId} exists in database...`);
  const startTime = Date.now();
  const retryInterval = 2000; // Check every 2 seconds
  
  while (Date.now() - startTime < maxRetryTimeMs) {
    try {
      console.log(`Checking if animal ${animalId} exists... (${Math.round((Date.now() - startTime) / 1000)}s elapsed)`);
      
      const response = await fetch(`${API_URL}/animals/details`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-CSRF-Token': getCsrfToken()
        },
        body: JSON.stringify({ animalId: animalId })
      });

      if (response.ok) {
        console.log(`✅ Animal ${animalId} verified to exist in database`);
        return true;
      } else if (response.status === 404) {
        // Animal not found, continue retrying
        console.log(`Animal ${animalId} not found yet, retrying in ${retryInterval/1000}s...`);
      } else {
        // Other error, log but continue retrying
        console.log(`Error checking animal ${animalId} (status ${response.status}), retrying...`);
      }
    } catch (error) {
      console.log(`Network error checking animal ${animalId}: ${error.message}, retrying...`);
    }
    
    // Wait before next retry
    await new Promise(resolve => setTimeout(resolve, retryInterval));
  }
  
  console.error(`❌ Failed to verify animal ${animalId} exists after ${maxRetryTimeMs/1000} seconds`);
  return false;
}

// Helper function to make requests with better error handling
async function makeAnimalDataRequest(url, data, dataType) {
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
        "X-CSRF-Token": getCsrfToken()
      },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const errorText = await response.text();
      let errorMessage = `Failed to add ${dataType}`;
      
      try {
        const errorData = JSON.parse(errorText);
        errorMessage = errorData.error || errorMessage;
      } catch (e) {
        errorMessage = errorText || errorMessage;
      }
      
      // Check if it's an "Animal not found" error
      if (errorMessage.includes("Animal not found")) {
        console.error(`❌ CRITICAL: ${errorMessage} - This should not happen after verification!`);
        throw new Error(`${dataType} failed: ${errorMessage}`);
      } else {
        console.error(`Failed to add ${dataType}:`, errorMessage);
        return false;
      }
    } else {
      console.log(`${dataType} added successfully`);
      return true;
    }
  } catch (error) {
    if (error.message.includes("Animal not found")) {
      throw error; // Re-throw critical errors
    }
    console.error(`Error adding ${dataType}:`, error);
    return false;
  }
}

// Form submission handling
async function submitPublishForm(event) {
  if (event) event.preventDefault();
  
  try {
    // Show loading spinner
    showLoading("Publicare în curs...");
    
    const userID = user.id;
    const name = sanitizeInput(document.getElementById("name").value.trim());
    const species = sanitizeInput(document.getElementById("species").value);
    const breed = sanitizeInput(document.getElementById("breed").value);
    const age = parseInt(document.getElementById("age").value, 10);
    const gender = document.getElementById("gender").value;

    // Step 1: Create basic animal first
    const basicAnimalPayload = {
      name,
      breed,
      species,
      age,
      gender
    };

    console.log("Step 1: Creating basic animal with payload:", basicAnimalPayload);
    
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 15000); // 15 second timeout
    
    let animalResponse;
    try {
      console.log(`Connecting to ${API_URL}/animals/create`);
      const response = await fetch(`${API_URL}/animals/create`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
          "X-CSRF-Token": getCsrfToken()
        },
        body: JSON.stringify(basicAnimalPayload),
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorText = await response.text();
        let errorMessage;
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.error || `Server error: ${response.status}`;
        } catch (e) {
          errorMessage = `Server error: ${response.status}`;
        }
        throw new Error(errorMessage);
      }

      animalResponse = await response.json();
      console.log("Basic animal created successfully:", animalResponse);
      
    } catch (fetchError) {
      clearTimeout(timeoutId);
      
      if (fetchError.name === 'AbortError') {
        throw new Error("Request timed out. Please check your server connection.");
      } else {
        throw fetchError;
      }
    }

    const animalId = animalResponse.ANIMALID;
    console.log(`Animal created with ID: ${animalId}, now verifying it exists before adding additional data...`);

    // Step 1.5: Verify animal exists in database before proceeding
    const animalExists = await verifyAnimalExists(animalId);
    if (!animalExists) {
      throw new Error(`Animal with ID ${animalId} could not be verified in database after 60 seconds. Please try again.`);
    }

    console.log(`Animal ${animalId} verified, proceeding with additional data...`);

    // Step 2: Add feeding schedule if present
    const feedingSchedule = [];
    const feedingEntries = document.querySelectorAll(".feeding-schedule-entry");
    
    console.log(`Found ${feedingEntries.length} feeding schedule entries`);

    feedingEntries.forEach((entry, index) => {
      const timeInput = entry.querySelector('[name="feedingTime"]');
      const foodTypeInput = entry.querySelector('[name="foodType"]');
      
      console.log(`Entry ${index}:`, {
        timeInput: timeInput ? timeInput.value : 'not found',
        foodTypeInput: foodTypeInput ? foodTypeInput.value : 'not found'
      });

      if (timeInput && timeInput.value) {
        const feedingData = {
          feedingTime: timeInput.value,
          foodType: sanitizeInput(foodTypeInput ? foodTypeInput.value : ""),
          feedingTimes: [timeInput.value], // Backend expects feedingTimes array
          notes: "" // Add empty notes field
        };
        console.log(`Adding feeding schedule entry ${index}:`, feedingData);
        feedingSchedule.push(feedingData);
      } else {
        console.log(`Skipping entry ${index} - no time value`);
      }
    });

    console.log(`Total feeding schedule entries to send: ${feedingSchedule.length}`);
    if (feedingSchedule.length > 0) {
      console.log("Step 2: Adding feeding schedule:", feedingSchedule);
      await makeAnimalDataRequest(
        `${API_URL}/animals/${animalId}/feeding-schedule`,
        feedingSchedule,
        "feeding schedule"
      );
    } else {
      console.log("No feeding schedule entries to send");
    }

    // Step 3: Add medical history if present
    const medicalHistoryEntries = document.querySelectorAll(".medical-history-entry");
    const medicalHistory = [];

    medicalHistoryEntries.forEach((entry) => {
      const recordDateInput = entry.querySelector('[name="recordDate"]');
      const descriptionInput = entry.querySelector('[name="description"]');

      if (recordDateInput && recordDateInput.value) {
        medicalHistory.push({
          recordDate: recordDateInput.value,
          description: sanitizeInput(descriptionInput ? descriptionInput.value : ""),
          vetNumber: sanitizeInput(document.getElementById("vetNumber").value),
          first_aid_noted: sanitizeInput(document.getElementById("firstAidNoted").value)
        });
      }
    });

    if (medicalHistory.length > 0) {
      console.log("Step 3: Adding medical history:", medicalHistory);
      await makeAnimalDataRequest(
        `${API_URL}/animals/${animalId}/medical-history`,
        medicalHistory,
        "medical history"
      );
    }

    // Step 4: Upload and add multimedia if present
    const multimedia = [];
    
    // Main photo
    const photoInput = document.getElementById("photo");
    if (photoInput && photoInput.files.length > 0) {
      const photoFile = photoInput.files[0];
      const mediaType = "photo";

      try {
        const serverPath = await uploadFileToServer(photoFile, mediaType);
        multimedia.push({
          mediaType: mediaType,
          url: serverPath,
          description: "Main photo",
        });
      } catch (uploadError) {
        console.error("Error uploading main photo:", uploadError);
        // Continue with other operations instead of failing completely
      }
    }

    // Additional multimedia entries
    const multimediaEntries = document.querySelectorAll(".multimedia-entry");
    for (const entry of multimediaEntries) {
      const mediaTypeSelect = entry.querySelector('[name="mediaType"]');
      const fileInput = entry.querySelector('[name="file"]');
      const descriptionInput = entry.querySelector('[name="description"]');

      if (fileInput && fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const mediaType = mediaTypeSelect ? mediaTypeSelect.value : "photo";
        const description = sanitizeInput(descriptionInput ? descriptionInput.value : "");

        try {
          const serverPath = await uploadFileToServer(file, mediaType);
          multimedia.push({
            mediaType: mediaType,
            url: serverPath,
            description: description,
          });
        } catch (uploadError) {
          console.error("Error uploading additional media:", uploadError);
          // Continue with other files instead of failing completely
        }
      }
    }

    if (multimedia.length > 0) {
      console.log("Step 4: Adding multimedia:", multimedia);
      await makeAnimalDataRequest(
        `${API_URL}/animals/${animalId}/multimedia`,
        multimedia,
        "multimedia"
      );
    }

    // Step 5: Add relations if present
    const relationsInput = document.getElementById("relations");
    if (relationsInput && relationsInput.value) {
      const relations = {
        friendWith: sanitizeInput(relationsInput.value)
          .split(",")
          .map((name) => name.trim())
          .filter((name) => name)
          .join(","),
      };

      if (relations.friendWith) {
        console.log("Step 5: Adding relations:", relations);
        await makeAnimalDataRequest(
          `${API_URL}/animals/${animalId}/relations`,
          relations,
          "relations"
        );
      }
    }

    console.log("All steps completed successfully!");
    
    // Hide loading spinner
    hideLoading();

    // Set a redirect flag in case direct redirection fails
    sessionStorage.setItem("redirect_after_publish", "true");
    
    // Redirect to home page
    console.log("Animal creation process complete, redirecting...");
    window.location.href = "../Home/Home.html";
    
  } catch (error) {
    console.error("Error during publication:", error);
    hideLoading();
    alert(`Error: ${error.message || "Failed to publish animal"}`);
  }
}

// Helper function to upload files to the server
async function uploadFileToServer(file, mediaType) {

  const formData = new FormData();
  
  // Append the file with its original name to preserve the extension
  formData.append("file", file, file.name);
  formData.append("mediaType", mediaType);
  
  // Log file details for debugging
  console.log(`Uploading file: ${file.name}, type: ${file.type}, size: ${file.size}`);
  
  // Set a timeout for the upload
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout for uploads
  
  try {
    console.log(`Sending to ${API_URL}/upload`);
    const uploadResponse = await fetch(`${API_URL}/upload`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "X-CSRF-Token": getCsrfToken() 
      },
      body: formData,
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    
    if (!uploadResponse.ok) {
      throw new Error(`Upload failed with status ${uploadResponse.status}`);
    }

    const responseData = await uploadResponse.json();
    console.log(`Upload successful: ${file.name}, server path: ${responseData.filePath}`);
    return responseData.filePath;
    
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error("Upload timed out. Please try again with a smaller file or check your connection.");
    }
    throw error;
  }
}
