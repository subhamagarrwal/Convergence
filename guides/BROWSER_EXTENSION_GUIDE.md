# Convergence Browser Extension Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Extension Structure](#extension-structure)
5. [Backend Integration](#backend-integration)
6. [Features Implementation](#features-implementation)
7. [Cross-Browser Support](#cross-browser-support)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Research Topics](#research-topics)

---

## 🎯 Overview

### What We're Building
A **browser extension** that provides:
1. **Quick Search** - Press `Ctrl+Shift+K` → Search popup appears
2. **Quick Save** - Right-click any page → "Save to Convergence"
3. **Bookmark Sync** - Import existing browser bookmarks
4. **Platform Integration** - One-click OAuth for Reddit, YouTube, etc.

### User Experience

**Quick Search:**
```
User presses Ctrl+Shift+K
        │
        ▼
┌─────────────────────────────────────────┐
│  ┌─────────────────────────────────┐   │
│  │ 🔍 Search bookmarks...          │   │
│  └─────────────────────────────────┘   │
│                                         │
│  📌 Recent:                             │
│  • Java Spring Tutorial (YouTube)       │
│  • Redis Guide (Medium)                 │
│  • OAuth Explained (Reddit)             │
│                                         │
│  [Settings] [Sync Now] [Open Dashboard] │
└─────────────────────────────────────────┘
```

**Quick Save:**
```
User right-clicks on page
        │
        ▼
┌─────────────────────────┐
│ 📋 Copy                 │
│ 📋 Paste                │
│ ─────────────────────── │
│ ⭐ Save to Convergence  │  ← Our option
│ 📁 Save to folder...    │
└─────────────────────────┘
        │
        ▼
Notification: "Bookmark saved! ✓"
```

---

## 🛠️ Tech Stack

### Extension Framework
| Component | Technology | Purpose |
|-----------|------------|---------|
| Manifest | Manifest V3 | Extension configuration |
| Popup UI | React + TypeScript | Search & settings popup |
| Background | Service Worker | API calls, sync, auth |
| Content Scripts | TypeScript | Page interaction |
| Storage | chrome.storage | Local data persistence |
| Styling | Tailwind CSS | Consistent UI |

### Build Tools
| Tool | Purpose |
|------|---------|
| Vite | Fast bundling |
| CRXJS | Chrome extension Vite plugin |
| TypeScript | Type safety |
| ESLint | Code quality |

---

## 🏗️ Architecture

### Extension Components
```
┌─────────────────────────────────────────────────────────────┐
│                    BROWSER EXTENSION                         │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Popup     │  │  Options    │  │  Content Script     │ │
│  │   (React)   │  │  Page       │  │  (Page Injection)   │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
│         │                │                     │            │
│         └────────────────┼─────────────────────┘            │
│                          │                                   │
│                          ▼                                   │
│              ┌─────────────────────┐                        │
│              │   Service Worker    │                        │
│              │   (Background)      │                        │
│              └──────────┬──────────┘                        │
│                         │                                    │
└─────────────────────────│────────────────────────────────────┘
                          │
                          ▼
              ┌─────────────────────┐
              │   Spring Boot API   │
              │   (Your Backend)    │
              └─────────────────────┘
```

### Communication Flow
```
Popup ←→ Service Worker ←→ Backend API
  ↓           ↓
Content   chrome.storage
Script
```

---

## 📁 Extension Structure

```
convergence-extension/
├── manifest.json              # Extension manifest (V3)
├── package.json
├── vite.config.ts
├── tsconfig.json
│
├── src/
│   ├── popup/                 # Popup UI (React)
│   │   ├── index.html
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── SearchBar.tsx
│   │   │   ├── ResultsList.tsx
│   │   │   ├── QuickActions.tsx
│   │   │   └── AuthStatus.tsx
│   │   └── styles/
│   │       └── popup.css
│   │
│   ├── options/               # Options/Settings page
│   │   ├── index.html
│   │   ├── main.tsx
│   │   └── Settings.tsx
│   │
│   ├── background/            # Service Worker
│   │   ├── index.ts
│   │   ├── api.ts             # Backend API calls
│   │   ├── auth.ts            # Authentication handling
│   │   ├── sync.ts            # Bookmark sync logic
│   │   └── contextMenu.ts     # Right-click menu
│   │
│   ├── content/               # Content scripts
│   │   ├── index.ts
│   │   └── pageInfo.ts        # Extract page metadata
│   │
│   ├── shared/                # Shared utilities
│   │   ├── types.ts
│   │   ├── constants.ts
│   │   ├── storage.ts
│   │   └── messaging.ts
│   │
│   └── assets/
│       ├── icon-16.png
│       ├── icon-48.png
│       └── icon-128.png
│
└── dist/                      # Built extension
```

### Manifest V3 Configuration

**Research Topics:**
- Manifest V3 vs V2 differences
- Service workers in extensions
- Permission model
- Content Security Policy

**Key manifest.json sections:**
```
manifest.json includes:
- name, version, description
- permissions (storage, tabs, contextMenus)
- host_permissions (your API domain)
- background service worker
- action (popup)
- content_scripts
- commands (keyboard shortcuts)
- options_page
```

---

## ⚙️ Backend Integration

### API Endpoints Used by Extension

| Endpoint | Purpose | When Called |
|----------|---------|-------------|
| `POST /api/auth/login` | User authentication | On extension login |
| `POST /api/auth/refresh` | Refresh JWT token | When token expires |
| `GET /api/bookmarks` | Fetch user bookmarks | On popup open |
| `POST /api/bookmarks` | Save new bookmark | Quick save |
| `POST /api/search` | Search bookmarks | On search input |
| `POST /api/bookmarks/import` | Bulk import | Sync browser bookmarks |
| `GET /api/platforms` | Get connected platforms | Show status |

### Authentication Flow

```
Extension Install
        │
        ▼
User clicks "Login"
        │
        ▼
Open OAuth page in new tab
(or username/password form)
        │
        ▼
Receive JWT token
        │
        ▼
Store in chrome.storage.local
        │
        ▼
Include token in all API calls
Authorization: Bearer <token>
```

### Token Refresh Strategy

```
Every API call:
    │
    ├─→ Check token expiry
    │   └─→ If expired in < 5 min → Refresh first
    │
    ├─→ Make API call
    │   └─→ If 401 response → Try refresh
    │       └─→ If refresh fails → Prompt re-login
    │
    └─→ Return response
```

---

## 🔧 Features Implementation

### Feature 1: Quick Search Popup

**Research Topics:**
- Chrome extension popup lifecycle
- React in extensions
- Debouncing search input
- Keyboard navigation

**Implementation Steps:**
1. Create popup HTML entry point
2. Build React search component
3. Connect to backend search API
4. Handle keyboard shortcuts
5. Display results with platform icons

**Popup Behavior:**
```
- Opens when clicking extension icon
- Also opens on Ctrl+Shift+K
- Closes when clicking outside
- Remembers recent searches
- Shows recent bookmarks on empty search
```

### Feature 2: Quick Save (Context Menu)

**Research Topics:**
- chrome.contextMenus API
- Content script communication
- Extracting page metadata (title, description, favicon)

**Flow:**
```
1. User right-clicks on any webpage
2. Context menu shows "Save to Convergence"
3. User clicks the option
4. Content script extracts page info:
   - URL
   - Title
   - Meta description
   - Open Graph image
5. Service worker sends to API
6. Show notification: "Saved!"
```

**Page Info Extraction:**
```
Extract from page:
- document.title
- meta[name="description"]
- meta[property="og:title"]
- meta[property="og:description"]
- meta[property="og:image"]
- link[rel="icon"]
```

### Feature 3: Bookmark Import/Sync

**Research Topics:**
- chrome.bookmarks API
- Recursive tree traversal
- Batch API requests
- Progress tracking

**Import Flow:**
```
1. User clicks "Import Browser Bookmarks"
2. Request chrome.bookmarks permission
3. Read bookmark tree: chrome.bookmarks.getTree()
4. Flatten to list of {title, url, folder}
5. Filter duplicates (already in Convergence)
6. Batch upload to API (100 at a time)
7. Show progress bar
8. Complete notification
```

**Sync Strategy:**
```
Initial sync: Import all bookmarks
Incremental sync: Only new bookmarks since last sync
Manual sync: Button in popup
Auto sync: Every 24 hours (configurable)
```

### Feature 4: Platform OAuth Integration

**Research Topics:**
- OAuth in browser extensions
- chrome.identity API
- Handling OAuth callbacks

**OAuth Flow:**
```
1. User clicks "Connect Reddit"
2. Extension opens Reddit OAuth URL
3. User approves in browser
4. Reddit redirects to callback URL
5. Extension intercepts callback
6. Extract authorization code
7. Send code to backend
8. Backend exchanges for tokens
9. Store connection status
```

### Feature 5: Keyboard Shortcuts

**Research Topics:**
- chrome.commands API
- Manifest commands configuration
- Custom shortcuts

**Default Shortcuts:**
| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+K` | Open search popup |
| `Ctrl+Shift+S` | Quick save current page |
| `Ctrl+Shift+B` | Open Convergence dashboard |

### Feature 6: Notifications

**Research Topics:**
- chrome.notifications API
- Notification types and options
- User preferences

**Notification Types:**
```
Success: "Bookmark saved! ✓"
Error: "Failed to save. Retry?"
Sync: "Syncing bookmarks... (50/100)"
Auth: "Session expired. Click to login."
```

---

## 🌐 Cross-Browser Support

### Browser Compatibility

| Browser | API Namespace | Manifest | Status |
|---------|---------------|----------|--------|
| Chrome | `chrome.*` | V3 | Primary |
| Edge | `chrome.*` | V3 | Compatible |
| Firefox | `browser.*` | V2/V3 | Needs adapter |
| Safari | `browser.*` | V2 | Needs work |

### Building for Multiple Browsers

**Research Topics:**
- WebExtension polyfill
- Browser-specific manifest fields
- API differences

**Strategy:**
```
1. Use webextension-polyfill for unified API
2. Create browser-specific manifest.json files
3. Build script generates per-browser packages
4. Test on each browser before release
```

**Unified API Usage:**
```typescript
// Instead of:
chrome.storage.local.get(...)

// Use:
import browser from 'webextension-polyfill';
browser.storage.local.get(...)
```

---

## 📅 Implementation Roadmap

### Phase 1: Project Setup (Days 1-2)

**Day 1: Initialize Project**
- [ ] Set up Vite + React + TypeScript
- [ ] Configure CRXJS for Chrome extension
- [ ] Create basic manifest.json
- [ ] Set up folder structure

**Day 2: Basic Popup**
- [ ] Create popup HTML/React entry
- [ ] Style basic search input
- [ ] Test loading extension in Chrome

### Phase 2: Core Features (Days 3-6)

**Day 3: Service Worker + API**
- [ ] Create service worker (background.ts)
- [ ] Implement API client
- [ ] Add authentication storage
- [ ] Handle token refresh

**Day 4: Search Integration**
- [ ] Connect popup to service worker
- [ ] Implement search API calls
- [ ] Display results in popup
- [ ] Add keyboard navigation

**Day 5: Quick Save**
- [ ] Create context menu
- [ ] Implement content script
- [ ] Extract page metadata
- [ ] Save bookmark via API

**Day 6: Notifications**
- [ ] Add success/error notifications
- [ ] Implement loading states
- [ ] Handle offline mode

### Phase 3: Advanced Features (Days 7-10)

**Day 7: Bookmark Import**
- [ ] Request bookmarks permission
- [ ] Read browser bookmark tree
- [ ] Batch upload to backend
- [ ] Show import progress

**Day 8: Platform OAuth**
- [ ] Add "Connect Platform" UI
- [ ] Implement OAuth flow for Reddit
- [ ] Implement OAuth flow for YouTube
- [ ] Store connection status

**Day 9: Settings/Options Page**
- [ ] Create options page
- [ ] Keyboard shortcut customization
- [ ] Sync frequency settings
- [ ] Theme settings (light/dark)

**Day 10: Keyboard Shortcuts**
- [ ] Register global shortcuts
- [ ] Handle shortcut conflicts
- [ ] Allow customization

### Phase 4: Polish & Publishing (Days 11-14)

**Day 11: UI/UX Polish**
- [ ] Smooth animations
- [ ] Platform icons
- [ ] Loading skeletons
- [ ] Error states

**Day 12: Cross-Browser**
- [ ] Add webextension-polyfill
- [ ] Create Firefox manifest
- [ ] Test on Firefox & Edge

**Day 13: Testing**
- [ ] Manual testing all features
- [ ] Fix bugs
- [ ] Performance optimization

**Day 14: Publishing**
- [ ] Create store assets (screenshots, description)
- [ ] Submit to Chrome Web Store
- [ ] Submit to Firefox Add-ons
- [ ] Submit to Edge Add-ons

---

## 📚 Research Topics Summary

### Extension Fundamentals
- [ ] Manifest V3 structure and permissions
- [ ] Service workers (background scripts)
- [ ] Content scripts and messaging
- [ ] chrome.storage API
- [ ] Extension lifecycle

### UI Development
- [ ] React in browser extensions
- [ ] Popup window constraints (800x600 max)
- [ ] Styling with Tailwind in extensions
- [ ] Keyboard navigation patterns

### APIs to Learn
- [ ] chrome.contextMenus - Right-click menus
- [ ] chrome.bookmarks - Browser bookmark access
- [ ] chrome.notifications - System notifications
- [ ] chrome.commands - Keyboard shortcuts
- [ ] chrome.identity - OAuth handling
- [ ] chrome.tabs - Tab management

### Security
- [ ] Content Security Policy for extensions
- [ ] Secure token storage
- [ ] HTTPS-only API calls
- [ ] Permission best practices

### Publishing
- [ ] Chrome Web Store requirements
- [ ] Extension review process
- [ ] Privacy policy requirements
- [ ] Store listing optimization

---

## 🔧 Development Tips

### Loading Unpacked Extension
```
1. Open chrome://extensions/
2. Enable "Developer mode"
3. Click "Load unpacked"
4. Select your dist/ folder
5. Reload after changes
```

### Debugging
```
Popup: Right-click extension icon → Inspect popup
Service Worker: chrome://extensions/ → Service Worker link
Content Script: Regular DevTools on the page
```

### Common Issues

| Issue | Solution |
|-------|----------|
| Popup closes on click | Use event.stopPropagation() |
| Service worker stops | Use alarms API for persistent tasks |
| CORS errors | Add host_permissions in manifest |
| Storage quota exceeded | Use chrome.storage.local (5MB) |

---

## 🔗 Useful Resources

### Official Documentation
- [Chrome Extension Docs](https://developer.chrome.com/docs/extensions/)
- [Manifest V3 Migration](https://developer.chrome.com/docs/extensions/mv3/intro/)
- [Firefox Extension Docs](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions)

### Boilerplates
- [CRXJS Vite Plugin](https://crxjs.dev/vite-plugin) - Modern extension development
- [Chrome Extension Boilerplate React](https://github.com/nicholasadamou/chrome-extension-boilerplate-react)

### Design Inspiration
- [Notion Web Clipper](https://www.notion.so/web-clipper)
- [Pocket Extension](https://getpocket.com/extensions/)
- [Raindrop.io](https://raindrop.io/)

---

## 📊 Extension vs Desktop App Comparison

| Feature | Browser Extension | Desktop App |
|---------|------------------|-------------|
| **Installation** | Easy (store) | Needs download |
| **System Hotkey** | Limited | Full access |
| **Always Running** | When browser open | Independent |
| **Browser Bookmarks** | Direct access | Needs export |
| **Page Content** | Easy access | Needs browser |
| **Cross-Browser** | Separate builds | One app |
| **Updates** | Auto via store | Manual/auto-updater |

### Recommendation
- **Start with Extension** - Easier to build, direct browser integration
- **Add Desktop App** - For power users who want system-wide search

---

*Last Updated: January 2026*
