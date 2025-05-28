const jwt = require('jsonwebtoken');
require('dotenv').config();

const secret = process.env.JWT_SECRET || 'your_jwt_secret';

// Function to generate a JWT token for a user
const generateToken = (user) => {
  
  const payload = {
    id: user.USERID || user.adminId || user.id,
    email: user.EMAIL || user.email,
    firstName: user.FIRSTNAME || user.firstName,
    lastName: user.LASTNAME || user.lastName,
    phone: user.PHONE || user.phone,
    createdAt: user.CREATEDAT || user.createdAt,
    isAdmin: user.isAdmin || false
  };

  return jwt.sign(
    payload,
    secret,
    { expiresIn: '24h' } 
  );
};

// Middleware to authenticate requests using JWT
const authenticate = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  if (!authHeader) {
    res.writeHead(401, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Authorization header missing' }));
    return;
  }

  const token = authHeader.split(' ')[1];
  if (!token) {
    res.writeHead(401, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Token missing' }));
    return;
  }

  try {
    const decoded = jwt.verify(token, secret);
    req.user = decoded; 
    next(); 
  } catch (err) {
    console.error('JWT verification failed:', err);
    res.writeHead(403, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Invalid or expired token' }));
  }
};

// Admin authentication middleware
const authenticateAdmin = (req, res, next) => {
  authenticate(req, res, () => {
    if (req.user && req.user.isAdmin) {
      next();
    } else {
      res.writeHead(403, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Admin access required' }));
    }
  });
};

module.exports = { generateToken, authenticate, authenticateAdmin };