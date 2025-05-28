const { getConnection } = require('../db');
const oracledb = require('oracledb'); 

class Address {
  static async create(userID, street, city, state, zipCode, country) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO Address (userID, Street, City, State, ZipCode, Country) 
         VALUES (:userID, :street, :city, :state, :zipCode, :country)`,
        { userID, street, city, state, zipCode, country },
        { autoCommit: true }
      );
      return result;
    } finally {
      await connection.close(); 
    }
  }

  static async findByUserId(userID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Address WHERE userID = :userID`,
        { userID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT } 
      );
      return result.rows;
    } finally {
      await connection.close();
    }
  }
}

module.exports = Address;