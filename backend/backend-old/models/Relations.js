const { getConnection } = require('../db');
const oracledb = require('oracledb'); 

class Relations {
  static async create(animalID, friendWith) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO Relations (animalID, friendWith) 
         VALUES (:animalID, :friendWith)`,
        { animalID, friendWith },
        { autoCommit: true }
      );
      return result;
    } finally {
      await connection.close();
    }
  }

  static async findByAnimalId(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Relations WHERE animalID = :animalID`,
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
        `DELETE FROM Relations WHERE animalID = :animalID`,
        { animalID },
        { autoCommit: true }
      );
      return result.rowsAffected > 0;
    } finally {
      await connection.close();
    }
  }
}

module.exports = Relations;