const Admin = require('../models/Admin');
const { generateToken } = require('../middleware/auth');
const { parseRequestBody } = require('../utils/requestUtils');

// Function to authenticate an admin
async function adminLogin(req, res) {
  try {
    const body = await parseRequestBody(req);
    const { email, password } = body;

    if (!email || !password) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing email or password' }));
      return;
    }

    const admin = await Admin.findByEmailAndPassword(email, password);
    if (!admin) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid email or password' }));
      return;
    }

    admin.isAdmin = true; 
    const token = generateToken(admin);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Admin authentication successful', token }));
  } catch (err) {
    console.error('Error authenticating admin:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

module.exports = { adminLogin };