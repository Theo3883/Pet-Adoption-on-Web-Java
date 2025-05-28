export function decodeToken() {
    const token = localStorage.getItem('Token');
    if (!token) {
      console.error('No token found in localStorage');
      return null;
    }
  
    try {
      const decoded = jwt_decode(token);
      return decoded;
    } catch (err) {
      console.error('Error decoding token:', err);
      return null;
    }
  }