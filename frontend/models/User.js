class User {
    constructor() {
      this.user = null;

      this.loadFromStorage();
    }
  
    setUser(userDetails) {
      this.user = {
        id: userDetails.id,          
        email: userDetails.email,    
        firstName: userDetails.firstName,  
        lastName: userDetails.lastName,    
        phone: userDetails.phone,
        createdAt: userDetails.createdAt || null 
      };
      
      this.saveToStorage();
    }
  
    getUser() {
      return this.user;
    }
  
    clearUser() {
      this.user = null;
      localStorage.removeItem('User');
    }
  

    saveToStorage() {
      if (this.user) {
        localStorage.setItem('User', JSON.stringify(this.user));
      }
    }
  
    loadFromStorage() {
      const storedUser = localStorage.getItem('User');
      if (storedUser) {
        try {
          this.user = JSON.parse(storedUser);
        } catch (err) {
          console.error('Error parsing stored user data:', err);
          this.user = null;
        }
      }
    }
  
    
    isLoggedIn() {
      return this.user !== null;
    }
  }
  
  const user = new User();
  export default user;