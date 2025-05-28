const { getConnection } = require("../db");
const oracledb = require("oracledb");
const fileUtils = require("../utils/fileStorageUtils");
const fs = require("fs");
const sharp = require("sharp");

class MultiMedia {
  static async create(animalID, media, url, description, upload_date) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `INSERT INTO MultiMedia (animalID, media, url, description, upload_date) 
         VALUES (:animalID, :media, :url, :description, :upload_date)`,
        { animalID, media, url, description, upload_date },
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
        `SELECT * FROM MultiMedia WHERE animalID = :animalID`,
        { animalID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );

      // Process the records to include pipe URLs
      const processedRecords = result.rows.map((record) => {
        return {
          ...record,
          pipeUrl: `/media/pipe/${record.ID}`,
        };
      });

      return processedRecords;
    } finally {
      await connection.close();
    }
  }

  static async findByAnimalIdOnePhoto(animalID) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM MultiMedia WHERE animalID = :animalID AND ROWNUM = 1`,
        { animalID },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );

      // Process the records to include pipe URLs
      const processedRecords = result.rows.map((record) => {
        return {
          ...record,
          pipeUrl: `/media/pipe/${record.ID}`,
        };
      });

      return processedRecords;
    } finally {
      await connection.close();
    }
  }

  static async deleteByAnimalId(animalID) {
    const connection = await getConnection();
    try {
      const mediaRecords = await this.findByAnimalId(animalID);

      // Delete the files 
      for (const record of mediaRecords) {
        if (record.URL) {
          const deleted = fileUtils.deleteFile(record.URL);
          if (!deleted) {
            console.warn(`Could not delete file for media ID ${record.ID}`);
          }
        }
      }

      const result = await connection.execute(
        `DELETE FROM MultiMedia WHERE animalID = :animalID`,
        { animalID },
        { autoCommit: true }
      );

      return result.rowsAffected > 0;
    } catch (error) {
      console.error("Error in deleteByAnimalId:", error);
      throw error;
    } finally {
      await connection.close();
    }
  }

  static async findById(id) {
    const connection = await getConnection();
    try {
      const result = await connection.execute(
        `SELECT * FROM MultiMedia WHERE id = :id`,
        { id },
        { outFormat: oracledb.OUT_FORMAT_OBJECT }
      );

      return result.rows[0];
    } finally {
      await connection.close();
    }
  }

  static async pipeMediaStream(id, res, options = {}, req = null) {
    try {
      // Find the media in the database
      const mediaRecord = await this.findById(id);

      if (!mediaRecord) {
        res.writeHead(404, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Media not found" }));
        return;
      }

      // The URL should be in the format: /server/{mediaType}/{fileName}
      const urlPath = mediaRecord.URL;
      if (!urlPath) {
        res.writeHead(404, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Invalid media path" }));
        return;
      }

      // Check if file exists
      const filePath = fileUtils.resolveFilePath(urlPath);
      const fileExists = await fs.promises
        .access(filePath, fs.constants.F_OK)
        .then(() => true)
        .catch(() => false);

      if (!fileExists) {
        res.writeHead(404, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Media file not found on disk" }));
        return;
      }

      // Determine content type
      const contentType = mediaRecord.MIMETYPE || "application/octet-stream";

      // Add cache control headers for all responses - great for mobile optimization
      const cacheHeaders = {
        "Cache-Control": "public, max-age=86400", // Cache for 24 hours
        "ETag": `"${mediaRecord.ID}-${options.width || 'orig'}"`, // Simple ETag
      };

      // Check if resizing is requested and supported
      const shouldResize =
        options.width &&
        contentType.startsWith("image/") &&
        contentType !== "image/gif";

      if (!shouldResize) {
        // Stream the file directly without modification
        const fileStream = fs.createReadStream(filePath);
        res.writeHead(200, { 
          "Content-Type": contentType,
          ...cacheHeaders
        });
        fileStream.pipe(res);
      } else {
        // Use sharp to resize the image

        // Get requested width
        const width = Math.min(2000, Math.max(50, parseInt(options.width, 10)));

        // Detect if client supports WebP
        let outputFormat = 'jpeg';
        let outputContentType = "image/jpeg";
        const acceptHeader = req && req.headers && req.headers.accept;
        
        if (acceptHeader && acceptHeader.includes('image/webp')) {
          outputFormat = 'webp';
          outputContentType = "image/webp";
        }
        
        // Process the image
        let transformer = sharp(filePath).resize({ 
          width, 
          withoutEnlargement: true,
          kernel: width < 300 ? 'lanczos3' : 'mitchell'  
        });
        
        // Apply format and quality
        if (outputFormat === 'webp') {
          transformer = transformer.webp({ 
            quality: 80, 
            effort: 4 
          });
        } else {
          transformer = transformer.jpeg({ 
            quality: 80, 
            progressive: true,
            optimizeScans: true
          });
        }

        res.writeHead(200, {
          "Content-Type": outputContentType,
          ...cacheHeaders
        });

        transformer.pipe(res);
      }
    } catch (error) {
      console.error("Error in pipeMediaStream:", error);
      if (!res.headersSent) {
        res.writeHead(500, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Internal server error" }));
      }
    }
  }
}

module.exports = MultiMedia;
