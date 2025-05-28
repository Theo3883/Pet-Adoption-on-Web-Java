
// Force enable scrolling on page
document.addEventListener('DOMContentLoaded', function() {

  document.documentElement.style.overflow = 'auto';
  document.documentElement.style.height = 'auto';
  document.body.style.overflow = 'auto';
  document.body.style.overflowY = 'auto';
  document.body.style.height = 'auto';
  document.body.style.position = 'static';
  
  const mainContent = document.querySelector('.main-content');
  if (mainContent) {
    mainContent.style.overflow = 'auto';
    mainContent.style.overflowY = 'auto';
    mainContent.style.height = 'auto';
  }
  
  setTimeout(function() {
    document.documentElement.style.overflow = 'auto';
    document.documentElement.style.height = 'auto';
    document.body.style.overflow = 'auto';
    document.body.style.overflowY = 'auto';
    document.body.style.height = 'auto';
  }, 500);
}); 