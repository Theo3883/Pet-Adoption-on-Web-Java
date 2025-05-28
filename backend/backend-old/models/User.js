const { getConnection } = require('../db');
const Animal = require('./Animal');
const oracledb = require('oracledb'); 

class User {
  static async create(firstName, lastName, email, password, phone) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO Users (firstName, lastName, email, password, phone) 
         VALUES (:firstName, :lastName, :email, :password, :phone)`,
        { firstName, lastName, email, password, phone },
        { autoCommit: true }
      );
      return result;
    } finally {
      await connection.close();
    }
  }

  static async findById(userID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Users WHERE userID = :userID`,
        { userID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT } 
      );
      return result.rows[0]; 
    } finally {
      await connection.close();
    }
  }

  static async findByEmailAndPassword(email, password) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM Users WHERE email = :email AND password = :password`,
        { email, password },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows[0]; 
    } finally {
      await connection.close();
    }
  }

  static async findByEmail(email) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT USERID FROM Users WHERE email = :email`,
        { email },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      return result.rows[0];
    } finally {
      await connection.close();
    }
  }

  static async getAllUsersWithDetails() {
    const connection = await getConnection();
    try {
     
      const result = await connection.execute(
        `SELECT u.*, a.addressID, a.Street, a.City, a.State, a.ZipCode, a.Country
         FROM Users u
         LEFT JOIN Address a ON u.userID = a.userID`,
        {},
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );

     
      const users = {};
      
      result.rows.forEach(row => {
        const userId = row.USERID;
        
        if (!users[userId]) {
          users[userId] = {
            USERID: row.USERID,
            FIRSTNAME: row.FIRSTNAME,
            LASTNAME: row.LASTNAME,
            EMAIL: row.EMAIL,
            PHONE: row.PHONE,
            CREATEDAT: row.CREATEDAT,
            address: null
          };
        }
        
        
        if (row.ADDRESSID) {
          users[userId].address = {
            ADDRESSID: row.ADDRESSID,
            STREET: row.STREET,
            CITY: row.CITY,
            STATE: row.STATE,
            ZIPCODE: row.ZIPCODE,
            COUNTRY: row.COUNTRY
          };
        }
      });
      
      return Object.values(users);
    } finally {
      await connection.close();
    }
  }

  static async deleteUserWithRelatedData(userID) {
    const connection = await getConnection();
    try {
      // get all animals for this user to delete their related records
      const animalResult = await connection.execute(
        `SELECT animalID FROM Animal WHERE userID = :userID`,
        { userID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );
      
      const animals = animalResult.rows;
      
  
      for (const animal of animals) {
        const animalID = animal.ANIMALID;
        
        await Animal.deleteAnimalWithRelatedData(animalID);
      }
      
      // delete address data
      try {
        await connection.execute(
          `DELETE FROM Address WHERE userID = :userID`,
          { userID },
          { autoCommit: true }
        );
        console.log(`Address deleted for user ${userID}`);
      } catch (err) {
        console.error('Error deleting address:', err);
      }
      
      // delete messages data
      try {
        await connection.execute(
          `DELETE FROM Messages WHERE senderId = :userID OR receiverId = :userID`,
          { userID },
          { autoCommit: true }
        );
        console.log(`Messages deleted for user ${userID}`);
      } catch (err) {
        console.error('Error deleting messages:', err);
      }
      
      // delete the user
      const userResult = await connection.execute(
        `DELETE FROM Users WHERE userID = :userID`,
        { userID },
        { autoCommit: true }
      );
      
      return userResult.rowsAffected > 0;
    } catch (error) {
      console.error('Error in deleteUserWithRelatedData:', error);
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
}

module.exports = User;