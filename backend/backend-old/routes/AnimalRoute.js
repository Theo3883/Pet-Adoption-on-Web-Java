const Animal = require("../models/Animal");
const MultiMedia = require("../models/MultiMedia");
const FeedingSchedule = require("../models/FeedingSchedule");
const MedicalHistory = require("../models/MedicalHistory");
const User = require("../models/User");
const Address = require("../models/Address");
const Relations = require("../models/Relations");
const { parseRequestBody } = require("../utils/requestUtils");
const { sendNewsletterEmails } = require('./NewsletterRoute');

// Get all animals with multimedia
async function getAllAnimals(req, res) {
  try {
    const animals = await Animal.getAll();

    const animalsWithMedia = await Promise.all(
      animals.map(async (animal) => {
        const multimedia = await MultiMedia.findByAnimalIdOnePhoto(
          animal.ANIMALID
        );
        return { ...animal, multimedia };
      })
    );

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify(animalsWithMedia));
  } catch (err) {
    console.error("Error fetching animals:", err);

    if (!res.headersSent) {
      res.writeHead(500, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Internal Server Error" }));
    }
  }
}

async function getAnimalDetailsById(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { animalId } = body;

    if (!animalId) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Animal ID is required" }));
      return;
    }

    const exists = await Animal.animalExists(animalId);
    if (!exists) {
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Animal not found" }));
      return;
    }

    const animal = await Animal.findById(animalId);
    await Animal.incrementViews(animalId);

    const multimedia = await MultiMedia.findByAnimalId(animalId);
    const feedingSchedule = await FeedingSchedule.findByAnimalId(animalId);
    const medicalHistory = await MedicalHistory.findByAnimalId(animalId);
    const owner = await User.findById(animal.USERID);
    const address = await Address.findByUserId(animal.USERID);

    const relations = await Relations.findByAnimalId(animalId);
    let friendRelations = [];
    if (relations && relations.length > 0) {
      friendRelations = relations.map((relation) => ({
        ID: relation.ID,
        FRIENDWITH: relation.FRIENDWITH,
      }));
    }

    const response = {
      animal,
      multimedia,
      feedingSchedule,
      medicalHistory,
      owner,
      address,
      relations: friendRelations,
    };

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify(response));
  } catch (error) {
    console.error("Error fetching animal details:", error);
    res.writeHead(500, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ message: "Failed to fetch animal details" }));
  }
}

async function findBySpecies(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { species } = body;

    if (!species) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Species is required" }));
      return;
    }
    
    // Get animals for this species
    const animals = await Animal.findBySpecies(species);
    
    // Get popular breeds for this species using PL/SQL
    const popularBreeds = await Animal.getPopularBreedsBySpecies(species);

    if (!animals || animals.length === 0) {
      // Still return popular breeds even when no animals are found
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ 
        error: "No animals found for this species",
        popularBreeds: popularBreeds || []
      }));
      return;
    }

    // Add multimedia to each animal
    const animalsWithMedia = await Promise.all(
      animals.map(async (animal) => {
        const multimedia = await MultiMedia.findByAnimalIdOnePhoto(
          animal.ANIMALID
        );
        return { ...animal, multimedia };
      })
    );

    // Return both animals and popular breeds
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({
      animals: animalsWithMedia,
      popularBreeds: popularBreeds || []
    }));
  } catch (err) {
    console.error("Error fetching animals by species:", err);
    res.writeHead(500, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Internal Server Error" }));
  }
}

async function createAnimal(req, res) {
  try {
    const body = await parseRequestBody(req);
    const {
      userID,
      name,
      breed,
      species,
      age,
      gender,
      feedingSchedule,
      medicalHistory,
      multimedia,
      relations,
    } = body;

    if (!userID || !name || !breed || !species || !age || !gender) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Missing required animal fields" }));
      return;
    }

    // Create the animal and get the ID directly
    console.log("Creating animal:", name, breed, species);
    const animalId = await Animal.create(userID, name, breed, species, age, gender);
    
    console.log("Animal created with ID:", animalId);
    
    
    // Handle Feeding Schedule
    if (feedingSchedule) {
      if (Array.isArray(feedingSchedule)) {
      
        const feedingTimes = feedingSchedule.map((item) => item.feedingTime);
        const foodType = feedingSchedule
          .map((item) => item.foodType)
          .join(", ");
        const notes = "Scheduled feeding times";
    
        await FeedingSchedule.create(
          animalId,
          feedingTimes, 
          foodType,
          notes
        );
      }
    }

    // Handle Medical History
    if (medicalHistory) {
      console.log("Medical history:", medicalHistory);
      
      if (Array.isArray(medicalHistory)) {
 
        for (const record of medicalHistory) {
          const { vetNumber, recordDate, description, first_aid_noted } = record;

          // Convert recordDate to Oracle date format
          const formattedDate = new Date(recordDate);
          
          await MedicalHistory.create(
            animalId,
            vetNumber,
            formattedDate,
            description,
            first_aid_noted
          );
        }
      } else {
        
        const { vetNumber, recordDate, description, first_aid_noted } = medicalHistory;

        const formattedDate = new Date(recordDate);
        
        await MedicalHistory.create(
          animalId,
          vetNumber,
          formattedDate,
          description,
          first_aid_noted
        );
      }
    }

    // Handle Multimedia
    if (multimedia && multimedia.length > 0) {
      console.log("Multimedia:", multimedia);
      
      for (const media of multimedia) {
        const { mediaType, url, description } = media;
        const upload_date = new Date();
        
        // Check if the URL exists 
        if (!url) {
          console.warn("Warning: Missing URL for multimedia item");
          continue;
        }
        
        console.log(`Creating multimedia: ${mediaType}, ${url}`);
        
        await MultiMedia.create(
          animalId,
          mediaType,
          url,
          description,
          upload_date
        );
      }
    }

    // Handle Relations
    if (relations && relations.friendWith) {
      await Relations.create(animalId, relations.friendWith);
    }

    res.writeHead(201, { "Content-Type": "application/json" });
    res.end(
      JSON.stringify({
        message: "Animal and related data created successfully",
        animalId,
      })
    );

    // Send newsletter emails 
    sendNewsletterEmails(animalId).catch(err => {
      console.error('Error sending newsletter emails:', err);
    });
    
  } catch (err) {
    console.error("Error parsing request or creating animal:", err);
    res.writeHead(400, { "Content-Type": "application/json" });
    res.end(
      JSON.stringify({ error: "Invalid request data or database error" })
    );
  }
}

async function deleteAnimal(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { animalId } = body;

    if (!animalId) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Animal ID is required" }));
      return;
    }

    // Check if animal exists using PL/SQL function
    const exists = await Animal.animalExists(animalId);
    if (!exists) {
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Animal not found" }));
      return;
    }

    // Delete the animal and all related data using PL/SQL package
    await Animal.deleteAnimalWithRelatedData(animalId);

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(
      JSON.stringify({
        message: "Animal and all related data successfully deleted",
      })
    );
  } catch (err) {
    console.error("Error deleting animal:", err);
    res.writeHead(500, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Internal Server Error" }));
  }
}

async function getTopAnimalsByCity(req, res) {
  try {
    // Parse the URL to extract query parameters
    const url = new URL(req.url, `http://${req.headers.host}`);
    const userId = url.searchParams.get('userId');

    if (!userId) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "User ID is required" }));
      return;
    }

    const topAnimals = await Animal.getTopAnimalsByCity(userId);

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify(topAnimals));
  } catch (error) {
    console.error("Error fetching top animals by city:", error);
    res.writeHead(500, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Internal Server Error" }));
  }
}

module.exports = {
  getAllAnimals,
  getAnimalDetailsById,
  findBySpecies,
  createAnimal,
  deleteAnimal,
  getTopAnimalsByCity,
};
