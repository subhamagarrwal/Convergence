const BACKEND_URL = 'http://localhost:8080';

// Main sync function
async function initializeBookmarkSync() {
  try {
    const token = await getAuthToken();
    const bookmarks = await getAllBookmarks();
    await syncToBackend(bookmarks, token);
    
    chrome.storage.local.set({ 
      lastSync: Date.now(),
      syncStatus: 'success',
      bookmarkCount: bookmarks.length
    });
  } catch (error) {
    console.error('Sync failed:', error);
    chrome.storage.local.set({ syncStatus: 'error', lastError: error.message });
  }
}

// Get auth token
function getAuthToken() {
  return new Promise((resolve, reject) => {
    chrome.identity.getAuthToken({ interactive: true }, (token) => {
      chrome.runtime.lastError ? reject(chrome.runtime.lastError) : resolve(token);
    });
  });
}

// Get all bookmarks (flattened)
async function getAllBookmarks() {
  return new Promise((resolve) => {
    chrome.bookmarks.getTree(([root]) => {
      const bookmarks = [];
      
      function flatten(node, folder = '') {
        if (node.url) {
          bookmarks.push({
            id: node.id,
            title: node.title || 'Untitled',
            url: node.url,
            dateAdded: node.dateAdded,
            folder: folder,
            favicon: `https://www.google.com/s2/favicons?domain=${new URL(node.url).hostname}&sz=64`
          });
        }
        if (node.children) {
          const path = node.title ? (folder ? `${folder}/${node.title}` : node.title) : folder;
          node.children.forEach(child => flatten(child, path));
        }
      }
      
      flatten(root);
      resolve(bookmarks);
    });
  });
}

// Sync to backend
async function syncToBackend(bookmarks, token) {
  const response = await fetch(`${BACKEND_URL}/api/bookmarks/sync`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ bookmarks, syncTime: Date.now() })
  });
  
  if (!response.ok) throw new Error(`Sync failed: ${response.status}`);
  return response.json();
}