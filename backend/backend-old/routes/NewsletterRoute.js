const { parseRequestBody } = require('../utils/requestUtils');
const Newsletter = require('../models/Newsletter');
const Animal = require('../models/Animal');
const nodemailer = require('nodemailer');

// Get user's newsletter subscriptions
async function getSubscriptions(req, res) {
  try {
    const userId = req.user.id;
    const subscriptions = await Newsletter.getByUserId(userId);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(subscriptions));
  } catch (err) {
    console.error('Error getting newsletter subscriptions:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

// Update user's newsletter subscriptions
async function updateSubscriptions(req, res) {
  try {
    const userId = req.user.id;
    const body = await parseRequestBody(req);
    
    
    await Newsletter.updateSubscriptions(userId, body.species);
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
  } catch (err) {
    console.error('Error updating newsletter subscriptions:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal Server Error' }));
  }
}

// Send newsletter emails for new animal
async function sendNewsletterEmails(animalId) {
  try {
   
    if (!global.emailConfigValid) {
      console.warn('Cannot send newsletter emails: Email configuration is invalid');
      return;
    }

    const animal = await Animal.findById(animalId);
    if (!animal) return;
    
    const species = animal.SPECIES;
    const subscribers = await Newsletter.getSubscribersBySpecies(species);
    
    if (subscribers.length === 0) return;
    
   
    const emailAddress = process.env.EMAIL_ADDRESS;
    const emailPassword = process.env.EMAIL_PASSWORD;
    
    
    const transporter = nodemailer.createTransport({
      service: 'gmail',
      auth: {
        user: emailAddress,
        pass: emailPassword
      }
    });
    
   
    const emailSubject = `New ${species} Added: ${animal.NAME}`;
    const emailContent = `
      <h2>A new ${species} has been added to Pet Adoption!</h2>
      <p><strong>Name:</strong> ${animal.NAME}</p>
      <p><strong>Breed:</strong> ${animal.BREED}</p>
      <p><strong>Age:</strong> ${animal.AGE}</p>
      <p><a href="http://localhost:8080/Home/Home.html">View on Pet Adoption</a></p>
      <hr>
      <p>You received this email because you subscribed to ${species} notifications. 
      To update your preferences, visit your <a href="http://localhost:8080/Newsletter/Newsletter.html">Newsletter settings</a>.</p>
    `;
    
    
    for (const subscriber of subscribers) {
      await transporter.sendMail({
        from: `"Pet Adoption" <${emailAddress}>`,
        to: subscriber.EMAIL,
        subject: emailSubject,
        html: emailContent
      });
    }
    
    console.log(`Newsletter emails sent for new ${species}`);
  } catch (err) {
    console.error('Error sending newsletter emails:', err);
  }
}

module.exports = {
  getSubscriptions,
  updateSubscriptions,
  sendNewsletterEmails
};