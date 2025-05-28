// Function to load CSS asynchronously
export function loadCssAsync(cssPath) {
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = cssPath;
  document.head.appendChild(link);
}

// Function to determine if an element is in viewport
export function isInViewport(element) {
  const rect = element.getBoundingClientRect();
  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  );
}

// Function to lazy load images
export function setupLazyLoading() {
  if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target;
          img.src = img.dataset.src;
          img.classList.remove('lazy');
          imageObserver.unobserve(img);
        }
      });
    });
    
    document.querySelectorAll('img.lazy').forEach(img => {
      imageObserver.observe(img);
    });
  } else {
    // Fallback for browsers that don't support IntersectionObserver
    document.querySelectorAll('img.lazy').forEach(img => {
      img.src = img.dataset.src;
      img.classList.remove('lazy');
    });
  }
}

// Add preconnect for external domains
export function addPreconnect(domain) {
  if (!document.querySelector(`link[rel="preconnect"][href="${domain}"]`)) {
    const link = document.createElement('link');
    link.rel = 'preconnect';
    link.href = domain;
    document.head.appendChild(link);
  }
}

// Add dns-prefetch for external domains
export function addDnsPrefetch(domain) {
  if (!document.querySelector(`link[rel="dns-prefetch"][href="${domain}"]`)) {
    const link = document.createElement('link');
    link.rel = 'dns-prefetch';
    link.href = domain;
    document.head.appendChild(link);
  }
}

// Inject critical CSS inline
export function injectCriticalCSS(cssText) {
  const style = document.createElement('style');
  style.textContent = cssText;
  document.head.appendChild(style);
}

// Load CSS with preload pattern
export function preloadCSS(href) {
  const link = document.createElement('link');
  link.rel = 'preload';
  link.as = 'style';
  link.href = href;
  link.onload = () => {
    link.onload = null;
    link.rel = 'stylesheet';
  };
  document.head.appendChild(link);
  
  setTimeout(() => {
    if (link.rel !== 'stylesheet') {
      link.rel = 'stylesheet';
    }
  }, 1000);
  
  // Add noscript fallback
  const noscript = document.createElement('noscript');
  const noscriptLink = document.createElement('link');
  noscriptLink.rel = 'stylesheet';
  noscriptLink.href = href;
  noscript.appendChild(noscriptLink);
  document.head.appendChild(noscript);
}