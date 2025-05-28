const fs = require('fs');
const path = require('path');
const multer = require('multer');
const mime = require('mime-types');


const getProjectRoot = () => {
  return path.resolve(__dirname, '..', '..');
};


const configureStorage = () => {

  let currentMediaType = 'photo';
  
  // storage configuration
  const storage = multer.diskStorage({
    destination: (req, file, cb) => {

      const mediaType = currentMediaType;
      console.log(`Saving file in directory: ${mediaType}`);
      
      const dir = path.join(getProjectRoot(), 'server', mediaType);
      
      // Ensure directory exists
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      
      cb(null, dir);
    },
    filename: (req, file, cb) => {
     
      const originalExt = path.extname(file.originalname);
      const baseName = path.basename(file.originalname, originalExt)
        .replace(/\s+/g, '_')
        .replace(/[^a-zA-Z0-9_-]/g, '');
      
      const fileName = `${Date.now()}_${baseName}${originalExt}`;
      cb(null, fileName);
    }
  });

  // multer instance 
  return multer({
    storage: storage,
    fileFilter: (req, file, cb) => {

      if (req.body && req.body.mediaType) {
        currentMediaType = req.body.mediaType;
      } else {
 
        if (file.mimetype.startsWith('video/')) {
          currentMediaType = 'video';
        } else if (file.mimetype.startsWith('audio/')) {
          currentMediaType = 'audio';
        } else {
          currentMediaType = 'photo';
        }
      }
      
      console.log(`Determined media type: ${currentMediaType} for file ${file.originalname}`);
      cb(null, true);
    }
  });
};


//Get the appropriate MIME type 
const getMimeType = (filePath) => {
  const ext = path.extname(filePath).toLowerCase();
  const mimeType = mime.lookup(ext) || 'application/octet-stream';
  return mimeType;
};

//stream function
const streamFile = async (filePath, res) => {
  try {
    if (!fs.existsSync(filePath)) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'File not found', path: filePath }));
      return;
    }

    const mimeType = getMimeType(filePath);
    
    res.writeHead(200, {
      'Content-Type': mimeType,
      'Cache-Control': 'max-age=31536000'
    });

    const fileStream = fs.createReadStream(filePath);
    fileStream.pipe(res);

    fileStream.on('error', (error) => {
      console.error('Error streaming file:', error);
      if (!res.headersSent) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Error streaming file' }));
      }
    });
  } catch (error) {
    console.error('Error in streamFile:', error);
    if (!res.headersSent) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal server error' }));
    }
  }
};

//delete the file 
const deleteFile = (urlPath) => {
  try {
    if (!urlPath) return false;
    
    const normalizedPath = urlPath.startsWith('/') ? urlPath.substring(1) : urlPath;
    const filePath = path.join(getProjectRoot(), normalizedPath);

    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
      console.log(`Successfully deleted file: ${filePath}`);
      return true;
    } else {
      console.warn(`File not found, cannot delete: ${filePath}`);
      return false;
    }
  } catch (error) {
    console.error('Error deleting file:', error);
    return false;
  }
};

//create the path
const resolveFilePath = (urlPath) => {
  if (!urlPath) return null;
  
  const normalizedPath = urlPath.startsWith('/') ? urlPath.substring(1) : urlPath;
  return path.join(getProjectRoot(), normalizedPath);
};


const getPublicUrl = (mediaType, filename) => {
  return `/server/${mediaType}/${filename}`;
};

module.exports = {
  configureStorage,
  streamFile,
  deleteFile,
  resolveFilePath,
  getProjectRoot,
  getMimeType,
  getPublicUrl
};