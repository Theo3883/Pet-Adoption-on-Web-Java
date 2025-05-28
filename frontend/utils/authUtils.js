import userModel from '../models/User.js';


export function requireAuth() {
  const user = userModel.getUser();
  
  if (!user || !user.id) {
    console.log('No authenticated user found, redirecting to login');
    window.location.href = '../Auth/SignIn.html';
    return null;
  }
  
  return user;
}

export function redirectIfLoggedIn() {
  const user = userModel.getUser();
  
  if (user && user.id) {
    console.log('User already logged in, redirecting to home');
    window.location.href = '../Home/Home.html';
    return true;
  }
  
  return false;
}