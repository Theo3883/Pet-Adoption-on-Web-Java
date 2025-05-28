const API_URL = 'http://localhost:3000'; 

// Generate responsive image sizes based on device and viewport
export function getResponsiveImageUrl(originalUrl, options = {}) {
  const defaults = {
    width: 400,
    quality: 'auto',
    format: 'auto'
  };
  
  const settings = { ...defaults, ...options };
  
  if (originalUrl.includes('/media/pipe/')) {

    const matches = originalUrl.match(/\/media\/pipe\/(\d+)/);
    if (matches && matches[1]) {
      const id = matches[1];
      let url = `${API_URL}/media/pipe/${id}?width=${settings.width}`;
      
      //quality parameter
      if (settings.quality !== 'auto') {
        url += `&quality=${settings.quality}`;
      }
      
      //format parameter 
      if (settings.format !== 'auto') {
        url += `&format=${settings.format}`;
      }
      
      return url;
    }
  }
  
  return originalUrl;
}

// Generate appropriate srcset for responsive images
export function generateSrcSet(baseUrl) {
  if (!baseUrl.includes('/media/pipe/')) {
    return baseUrl;
  }
  
  const matches = baseUrl.match(/\/media\/pipe\/(\d+)/);
  if (!matches || !matches[1]) {
    return baseUrl;
  }
  
  const id = matches[1];
  
  // Create srcset for different screen sizes with full API URL
  return `
    ${API_URL}/media/pipe/${id}?width=300 300w,
    ${API_URL}/media/pipe/${id}?width=600 600w, 
    ${API_URL}/media/pipe/${id}?width=900 900w,
    ${API_URL}/media/pipe/${id}?width=1200 1200w
  `.trim();
}

//export const createSrcSet = generateSrcSet;

// Calculate optimal image size based on viewport and device pixel ratio
export function getOptimalImageSize() {
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
  const dpr = window.devicePixelRatio || 1;
  
  let baseWidth;
  
  if (viewportWidth < 640) {
    baseWidth = viewportWidth - 30;
  } else if (viewportWidth < 768) {
    baseWidth = Math.floor(viewportWidth / 2) - 30;
  } else if (viewportWidth < 1024) {
    baseWidth = Math.floor(viewportWidth / 3) - 30;
  } else {
    baseWidth = Math.floor(viewportWidth / 4) - 30;
  }
  
  const optimalWidth = Math.ceil((baseWidth * dpr) / 100) * 100;
  
  return Math.min(optimalWidth, 1200);
}

// Create placeholder for images while they load
export function generatePlaceholder(animal) {
  const initial = animal && animal.NAME ? animal.NAME.charAt(0).toUpperCase() : '?';

  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300">
      <rect width="400" height="300" fill="#cccccc" />
      <text x="50%" y="50%" font-family="Arial" font-size="120" fill="#ffffff" text-anchor="middle" dominant-baseline="middle" opacity="0.7">
        ${initial}
      </text>
    </svg>
  `;
  
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg.trim())}`;
}