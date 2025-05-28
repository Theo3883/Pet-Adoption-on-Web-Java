const { getConnection } = require('../db');
const oracledb = require('oracledb'); 

class MedicalHistory {
  static async create(animalID, vetNumber, recordDate, description, firstAidNoted) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO MedicalHistory (animalID, vetNumber, recordDate, description, first_aid_noted) 
         VALUES (:animalID, :vetNumber, :recordDate, :description, :firstAidNoted)`,
        { animalID, vetNumber, recordDate, description, firstAidNoted },
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
        `SELECT * FROM MedicalHistory WHERE animalID = :animalID`,
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
        `DELETE FROM MedicalHistory WHERE animalID = :animalID`,
        { animalID },
        { autoCommit: true }
      );
      return result.rowsAffected > 0;
    } finally {
      await connection.close();
    }
  }
}

module.exports = MedicalHistory;