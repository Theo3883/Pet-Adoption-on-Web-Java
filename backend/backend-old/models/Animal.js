const { getConnection } = require('../db');
const oracledb = require('oracledb');
const Relations = require('./Relations');
const MultiMedia = require('./MultiMedia');
const FeedingSchedule = require('./FeedingSchedule');
const MedicalHistory = require('./MedicalHistory');

class Animal {
  static async create(userID, name, breed, species, age, gender) {
    const connection = await getConnection();
    try {

      const result = await connection.execute(
        `INSERT INTO Animal (userID, name, breed, species, age, gender) 
         VALUES (:userID, :name, :breed, :species, :age, :gender)
         RETURNING animalID INTO :animalID`,
        { 
          userID, name, breed, species, age, gender,
          animalID: { type: oracledb.NUMBER, dir: oracledb.BIND_OUT }
        },
        { autoCommit: true }
      );
      
      
      return result.outBinds.animalID[0];
    } finally {
      await connection.close();
    }
  }

  static async getAll() {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Animal`,
        {},
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows;
    } finally {
      await connection.close();
    }
  }

  static async findById(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Animal WHERE animalID = :animalID`,
        { animalID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows[0];
    } finally {
      await connection.close();
    }
  }

  static async findByUserId(userID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Animal WHERE userID = :userID`,
        { userID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows;
    } finally {
      await connection.close();
    }
  }

  static async findBySpecies(species) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Animal WHERE species = :species`,
        { species },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows;
    } finally {
      await connection.close();
    }
  }

  static async deleteAnimalWithRelatedData(animalID) {
    const connection = await getConnection();
    try {
      try {
        await connection.execute(
          `BEGIN
             pet_adoption_utils.delete_animal_safe(:animalID);
           END;`,
          { animalID },
          { autoCommit: true }
        );
        return true;
      } catch (err) {
        console.error('Error using PL/SQL to delete animal, falling back to individual deletes:', err);
        
        try {
          await Relations.deleteByAnimalId(animalID);
          console.log(`Relations deleted`);
        } catch (err) {
          console.error('Error deleting relations:', err);
        }
        
        try {
          await MultiMedia.deleteByAnimalId(animalID);
          console.log(`Multimedia deleted`);
        } catch (err) {
          console.error('Error deleting multimedia:', err);
        }
        
        try {
          await FeedingSchedule.deleteByAnimalId(animalID);
          console.log(`Feeding schedule deleted`);
        } catch (err) {
          console.error('Error deleting feeding schedule:', err);
        }
        
        try {
          await MedicalHistory.deleteByAnimalId(animalID);
          console.log(`Medical history deleted`);
        } catch (err) {
          console.error('Error deleting medical history:', err);
        }

        const animalResult = await connection.execute(
          `DELETE FROM Animal WHERE animalID = :animalID`,
          { animalID },
          { autoCommit: true } 
        );
        
        return animalResult.rowsAffected > 0;
      }
    } catch (error) {
      console.error('Error in deleteAnimalWithRelatedData:', error);
      throw error;
    } finally {
      if (connection) {
        try {
          await connection.close();
        } catch (err) {
          console.error('Error closing connection:', err);
        }
      }
    }
  }

  static async incrementViews(animalID) {
    const connection = await getConnection();
    try {
      await connection.execute(
        `UPDATE Animal SET views = views + 1 WHERE animalID = :animalID`,
        { animalID },
        { autoCommit: true }
      );
    } finally {
      await connection.close();
    }
  }
  
  static async getTopAnimalsByCity(userId) {
    const connection = await getConnection();
    try {
      // First, get the user's city
      const userResult = await connection.execute(
        `SELECT a.CITY FROM ADDRESS a
         WHERE a.USERID = :userId`,
        { userId },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      if (userResult.rows.length === 0 || !userResult.rows[0].CITY) {
        return [];
      }
      
      const city = userResult.rows[0].CITY;
      
      // Get animals in same city
      const result = await connection.execute(
        `SELECT a.* 
         FROM ANIMAL a
         JOIN USERS u ON a.USERID = u.USERID
         JOIN ADDRESS ad ON u.USERID = ad.USERID
         WHERE ad.CITY = :city
         ORDER BY a.VIEWS DESC, a.CREATEDAT DESC`,
        { city },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      // Add multimedia data to each animal
      const animalsWithMedia = await Promise.all(
        result.rows.map(async (animal) => {
          const multimedia = await MultiMedia.findByAnimalIdOnePhoto(animal.ANIMALID);
          return { ...animal, multimedia };
        })
      );
      
      return animalsWithMedia;
    } finally {
      await connection.close();
    }
  }

  static async getAnimalDetailsForUser(userID) {
    const connection = await getConnection();
    try {
      // Get all animals for the user
      const animals = await this.findByUserId(userID);
      
      if (!animals || animals.length === 0) {
        return [];
      }
      
      // For each animal, fetch related data
      const detailedAnimals = await Promise.all(animals.map(async (animal) => {
        const animalID = animal.ANIMALID;
        
        // Get multimedia data
        const multimedia = await MultiMedia.findByAnimalId(animalID);
        
        // Get feeding schedule
        const feedingSchedule = await FeedingSchedule.findByAnimalId(animalID);
        
        // Get medical history
        const medicalHistory = await MedicalHistory.findByAnimalId(animalID);
        
        // Get relations
        const relationsData = await Relations.findByAnimalId(animalID);
        
        return {
          ...animal,
          multimedia: multimedia || [],
          feedingSchedule: feedingSchedule || [],
          medicalHistory: medicalHistory || [],
          relations: relationsData || []
        };
      }));
      
      return detailedAnimals;
    } finally {
      await connection.close();
    }
  }

  static async animalExists(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `DECLARE
           v_exists BOOLEAN;
         BEGIN
           v_exists := pet_adoption_utils.animal_exists(:animalID);
           :result := CASE WHEN v_exists THEN 1 ELSE 0 END;
         END;`,
        { 
          animalID,
          result: { dir: oracledb.BIND_OUT, type: oracledb.NUMBER }
        }
      );
      
      return result.outBinds.result === 1;
    } catch (error) {
      console.error('Error checking if animal exists:', error);
      const fallbackResult = await connection.execute(
        `SELECT COUNT(*) AS count FROM Animal WHERE animalID = :animalID`,
        { animalID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return fallbackResult.rows[0].COUNT > 0;
    } finally {
      await connection.close();
    }
  }

  static async getPopularBreedsBySpecies(species) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT breed, COUNT(*) as breed_count
         FROM Animal
         WHERE species = :species
         GROUP BY breed
         ORDER BY breed_count DESC`,
        { species },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows;
    } catch (error) {
      console.error('Error getting popular breeds:', error);
      return [];
    } finally {
      await connection.close();
    }
  }
}

module.exports = Animal;