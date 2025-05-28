const { getConnection } = require('../db');
const oracledb = require('oracledb'); 

class FeedingSchedule {
  static async create(animalID, feeding_times, food_type, notes) {
    const connection = await getConnection();
    try {

      if (!Array.isArray(feeding_times)) {
        throw new Error('feeding_times must be an array of time strings');
      }
      
      // Create the Oracle array constructor syntax with proper timestamps
      const feedingTimeSQL = `feeding_time_array(${
        feeding_times.map(time => `TO_TIMESTAMP('${time}', 'HH24:MI')`).join(',')
      })`;
      
   
      const result = await connection.execute(
        `INSERT INTO FeedingSchedule (animalID, feeding_time, food_type, notes) 
         VALUES (:animalID, ${feedingTimeSQL}, :food_type, :notes)`,
        { animalID, food_type, notes },
        { autoCommit: true }
      );
      return result;
    } catch (error) {
      console.error('Error creating feeding schedule:', error);
      throw error;
    } finally {
      await connection.close();
    }
  }

  static async findByAnimalId(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM FeedingSchedule WHERE animalID = :animalID`,
        { animalID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows;
    } finally {
      await connection.close();
    }
  }

  static async deleteByAnimalId(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `DELETE FROM FeedingSchedule WHERE animalID = :animalID`,
        { animalID },
        { autoCommit: true }
      );
      return result.rowsAffected > 0;
    } finally {
      await connection.close();
    }
  }
}

module.exports = FeedingSchedule;