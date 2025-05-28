const { getConnection } = require('../db');
const oracledb = require('oracledb');

class Newsletter {
  // Get user's subscriptions
  static async getByUserId(userId) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Newsletter 
         WHERE userID = :userId AND isActive = 1`,
        { userId },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      return result.rows;
    } finally {
      await connection.close();
    }
  }
  
  // Update user's subscriptions
  static async updateSubscriptions(userId, species) {
    const connection = await getConnection();
    try {
    
      await connection.execute(
        `UPDATE Newsletter SET isActive = 0 WHERE userID = :userId`,
        { userId },
        { autoCommit: false }
      );
      
      
      if (species && species.length > 0) {
        for (const speciesName of species) {
          // Check if subscription exists
          const exists = await connection.execute(
            `SELECT id FROM Newsletter WHERE userID = :userId AND species = :species`,
            { userId, species: speciesName },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
          );
          
          if (exists.rows.length > 0) {
           
            await connection.execute(
              `UPDATE Newsletter SET isActive = 1 WHERE userID = :userId AND species = :species`,
              { userId, species: speciesName },
              { autoCommit: false }
            );
          } else {
           
            await connection.execute(
              `INSERT INTO Newsletter (userID, species) VALUES (:userId, :species)`,
              { userId, species: speciesName },
              { autoCommit: false }
            );
          }
        }
      }
      
      
      await connection.commit();
      return true;
    } catch (error) {
    
      await connection.rollback();
      throw error;
    } finally {
      await connection.close();
    }
  }
  
  
  static async getSubscribersBySpecies(species) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT u.email, u.firstName, u.lastName, u.userID 
         FROM Newsletter n
         JOIN Users u ON n.userID = u.userID
         WHERE n.species = :species AND n.isActive = 1`,
        { species },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      return result.rows;
    } finally {
      await connection.close();
    }
  }
}

module.exports = Newsletter;