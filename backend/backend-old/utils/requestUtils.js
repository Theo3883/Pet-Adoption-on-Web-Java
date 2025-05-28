async function parseRequestBody(req) {
    return new Promise((resolve, reject) => {
      let body = '';
      
      req.on('data', (chunk) => {
        body += chunk.toString();
      });
      
      req.on('end', () => {
        try {
          const parsedBody = body ? JSON.parse(body) : {};
          resolve(parsedBody);
        } catch (error) {
          reject(new Error('Invalid JSON payload'));
        }
      });
      
      req.on('error', (error) => {
        reject(error);
      });
    });
  }
  
  module.exports = {
    parseRequestBody
  };