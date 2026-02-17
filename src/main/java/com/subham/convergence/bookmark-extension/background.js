importScripts('bookmark.js');

chrome.runtime.onInstalled.addListener(() =>
{
    initializeBookmarkSync();
});

chrome.bookmarks.onCreated.addListener(() => initializeBookmarkSync());
chrome.bookmarks.onRemoved.addListener(() => initializeBookmarkSync());
chrome.bookmarks.onChanged.addListener(() => initializeBookmarkSync());

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'manualSync') {
    initializeBookmarkSync()
      .then(() => sendResponse({ success: true }))
      .catch(error => sendResponse({ success: false, error: error.message }));
    return true;
  }
  
  if (request.action === 'getStatus') {
    chrome.storage.local.get(['lastSync', 'syncStatus', 'bookmarkCount'], sendResponse);
    return true;
  }
});

// Auto-sync every 30 minutes
chrome.alarms.create('autoSync', { periodInMinutes: 30 });
chrome.alarms.onAlarm.addListener(() => initializeBookmarkSync());