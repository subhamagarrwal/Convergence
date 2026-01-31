# Convergence Desktop App - Spotlight-Style Search Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Backend Implementation](#backend-implementation)
5. [Desktop Frontend Implementation](#desktop-frontend-implementation)
6. [Hotkey System](#hotkey-system)
7. [Natural Language Search](#natural-language-search)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Research Topics](#research-topics)

---

## 🎯 Overview

### What We're Building
A **desktop application** that works like Apple Spotlight:
- Press `Ctrl+Space` (or custom hotkey) → Search bar appears
- Type in natural language → "show me reddit posts about java"
- Results appear instantly from all your bookmarks
- Press `Enter` → Opens the bookmark
- Press `Escape` → Closes search bar

### User Experience Flow
```
User presses Ctrl+Space
        │
        ▼
┌─────────────────────────────────────┐
│  ╔═══════════════════════════════╗  │
│  ║  🔍 Search your bookmarks...  ║  │
│  ╚═══════════════════════════════╝  │
│                                     │
│  Recent:                            │
│  📄 Spring Boot Tutorial (YouTube)  │
│  📄 Redis Caching Guide (Medium)    │
│  📄 OAuth2 Explained (Reddit)       │
└─────────────────────────────────────┘

User types: "java tutorials from youtube"
        │
        ▼
┌─────────────────────────────────────┐
│  ╔═══════════════════════════════╗  │
│  ║  java tutorials from youtube  ║  │
│  ╚═══════════════════════════════╝  │
│                                     │
│  Results:                           │
│  ▶ Java Spring Boot Course (YouTube)│
│  ▶ Java Collections Deep Dive (YT)  │
│  ▶ JPA Tutorial for Beginners (YT)  │
│  ▶ Java 21 New Features (YouTube)   │
└─────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Option 1: Electron + React (Recommended for Beginners)
| Layer | Technology | Why |
|-------|------------|-----|
| Desktop Framework | Electron | Cross-platform (Windows, Mac, Linux) |
| Frontend | React + TypeScript | Component-based UI |
| Styling | Tailwind CSS | Fast styling |
| State Management | Zustand | Simple state management |
| HTTP Client | Axios | API calls to backend |
| Hotkey | electron-globalShortcut | System-wide hotkeys |

### Option 2: Tauri + React (Lightweight, Rust-based)
| Layer | Technology | Why |
|-------|------------|-----|
| Desktop Framework | Tauri | Smaller bundle, better performance |
| Frontend | React + TypeScript | Same as above |
| Backend Bridge | Rust | Native performance |
| Hotkey | tauri-plugin-global-shortcut | System-wide hotkeys |

### Option 3: JavaFX (Pure Java)
| Layer | Technology | Why |
|-------|------------|-----|
| Desktop Framework | JavaFX | Native Java, same language as backend |
| Styling | CSS | JavaFX CSS support |
| HTTP Client | HttpClient (Java 11+) | Built-in |
| Hotkey | JNativeHook | Global keyboard hooks |

---

## 🏗️ Architecture

### System Overview
```
┌─────────────────────────────────────────────────────────────┐
│                    DESKTOP APPLICATION                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Hotkey    │  │   Search    │  │    Results          │ │
│  │   Listener  │──│   Input     │──│    Display          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│         │               │                    │              │
└─────────│───────────────│────────────────────│──────────────┘
          │               │                    │
          │               ▼                    │
          │    ┌─────────────────────┐        │
          │    │   Local Cache       │        │
          │    │   (SQLite/IndexedDB)│        │
          │    └─────────────────────┘        │
          │               │                    │
          ▼               ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Auth      │  │   Search    │  │    NLP              │ │
│  │   Service   │  │   Service   │  │    Processor        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                          │                                   │
│                          ▼                                   │
│                ┌─────────────────┐                          │
│                │   PostgreSQL    │                          │
│                │   + Full Text   │                          │
│                └─────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### Search Flow
```
1. User presses Ctrl+Space
   └─→ Electron catches global shortcut
   └─→ Shows overlay window

2. User types "java tutorials youtube"
   └─→ Debounce input (300ms)
   └─→ Check local cache first
   └─→ If not cached, call backend API

3. Backend processes query
   └─→ Parse natural language
   └─→ Extract: keywords=["java", "tutorials"], platform=YOUTUBE
   └─→ Query database with filters
   └─→ Return ranked results

4. Display results
   └─→ Show in overlay
   └─→ Highlight matching terms
   └─→ User navigates with arrow keys

5. User presses Enter
   └─→ Open URL in default browser
   └─→ Close overlay
```

---

## ⚙️ Backend Implementation

### 1. Search API Endpoint

**Create endpoint for natural language search:**

```
POST /api/search
Content-Type: application/json
Authorization: Bearer <jwt_token>

Request:
{
    "query": "java tutorials from youtube",
    "limit": 10,
    "offset": 0
}

Response:
{
    "results": [
        {
            "id": "uuid",
            "title": "Java Spring Boot Course",
            "url": "https://youtube.com/...",
            "platform": "YOUTUBE",
            "relevanceScore": 0.95,
            "highlightedTitle": "<mark>Java</mark> Spring Boot Course"
        }
    ],
    "total": 45,
    "queryParsed": {
        "keywords": ["java", "tutorials"],
        "platform": "YOUTUBE",
        "contentType": null
    }
}
```

### 2. Natural Language Query Parser

**Research Topics:**
- How to extract keywords from natural language
- How to identify platform names in queries
- How to handle synonyms (yt → youtube, vid → video)

**Query Patterns to Handle:**
| User Query | Parsed As |
|------------|-----------|
| "java tutorials" | keywords: [java, tutorials] |
| "videos about spring boot" | keywords: [spring, boot], type: VIDEO |
| "reddit posts on microservices" | keywords: [microservices], platform: REDDIT |
| "saved articles from medium" | platform: MEDIUM, type: ARTICLE |
| "youtube java" | platform: YOUTUBE, keywords: [java] |

### 3. Search Service Structure

**Files to create:**

```
src/main/java/com/subham/convergence/
├── controller/
│   └── SearchController.java
├── service/
│   ├── SearchService.java
│   └── QueryParserService.java
├── dto/
│   ├── request/
│   │   └── SearchRequest.java
│   └── response/
│       ├── SearchResponse.java
│       └── SearchResultItem.java
└── repository/
    └── BookmarkSearchRepository.java
```

### 4. Full-Text Search in PostgreSQL

**Research Topics:**
- PostgreSQL `tsvector` and `tsquery`
- Full-text search indexes
- Ranking with `ts_rank`

**Database Setup:**
```sql
-- Add full-text search column
ALTER TABLE bookmarks ADD COLUMN search_vector tsvector;

-- Create index for fast search
CREATE INDEX idx_search_vector ON bookmarks USING GIN(search_vector);

-- Update search vector on insert/update
CREATE TRIGGER bookmarks_search_update
BEFORE INSERT OR UPDATE ON bookmarks
FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(search_vector, 'pg_catalog.english', title, description);
```

**Repository Query:**
```java
@Query(value = """
    SELECT b.*, ts_rank(search_vector, plainto_tsquery(:query)) as rank
    FROM bookmarks b
    WHERE b.user_id = :userId
    AND search_vector @@ plainto_tsquery(:query)
    ORDER BY rank DESC
    LIMIT :limit
    """, nativeQuery = true)
List<Bookmark> fullTextSearch(
    @Param("userId") UUID userId,
    @Param("query") String query,
    @Param("limit") int limit
);
```

### 5. Caching for Instant Results

**Research Topics:**
- Redis caching for search results
- Cache invalidation strategies
- Local cache (SQLite) in desktop app

**Cache Strategy:**
```
User searches "java"
    │
    ├─→ Check local cache (SQLite in Electron)
    │   └─→ If found and fresh (<5 min) → Return immediately
    │
    ├─→ Check Redis cache (backend)
    │   └─→ If found → Return and update local cache
    │
    └─→ Query database
        └─→ Store in Redis (5 min TTL)
        └─→ Return to client
        └─→ Client stores in local cache
```

---

## 🖥️ Desktop Frontend Implementation

### Project Setup (Electron + React)

**Research Topics:**
- How Electron works (main process vs renderer process)
- Electron IPC (inter-process communication)
- Creating transparent/overlay windows

**Project Structure:**
```
convergence-desktop/
├── package.json
├── electron/
│   ├── main.ts           # Electron main process
│   ├── preload.ts        # Bridge between main and renderer
│   └── tray.ts           # System tray icon
├── src/
│   ├── App.tsx           # Main React component
│   ├── components/
│   │   ├── SearchBar.tsx
│   │   ├── ResultsList.tsx
│   │   └── ResultItem.tsx
│   ├── hooks/
│   │   ├── useSearch.ts
│   │   └── useHotkey.ts
│   ├── services/
│   │   ├── api.ts
│   │   └── cache.ts
│   └── styles/
│       └── spotlight.css
└── electron-builder.json  # Build configuration
```

### Key Components

#### 1. Main Electron Process
```
electron/main.ts responsibilities:
- Create transparent overlay window
- Register global shortcut (Ctrl+Space)
- Handle window show/hide
- Manage system tray
- IPC communication with renderer
```

#### 2. Search Bar Component
```
SearchBar.tsx responsibilities:
- Autofocus on window show
- Debounce input (300ms delay)
- Show loading state
- Handle keyboard navigation (up/down arrows)
- Clear on Escape
```

#### 3. Results List Component
```
ResultsList.tsx responsibilities:
- Display search results
- Highlight matching text
- Show platform icons
- Handle item selection
- Keyboard navigation
```

### Window Behavior

**Research Topics:**
- Electron `BrowserWindow` options
- Transparent windows
- Always-on-top windows
- Focus/blur events

**Window Properties:**
```
- Frameless (no title bar)
- Transparent background
- Always on top
- Centered on screen
- Fixed width (600px), dynamic height
- Blur/close when clicking outside
- Smooth fade in/out animation
```

---

## ⌨️ Hotkey System

### Global Shortcut Registration

**Research Topics:**
- Electron `globalShortcut` module
- Handling conflicts with other apps
- Customizable shortcuts
- Platform-specific shortcuts (Cmd on Mac)

**Default Shortcuts:**
| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| Open search | `Ctrl+Space` | `Cmd+Space` |
| Close search | `Escape` | `Escape` |
| Navigate down | `↓` or `Ctrl+N` | `↓` or `Cmd+N` |
| Navigate up | `↑` or `Ctrl+P` | `↑` or `Cmd+P` |
| Open selected | `Enter` | `Enter` |
| Open in browser | `Ctrl+Enter` | `Cmd+Enter` |

### Handling Conflicts

**Problem:** `Cmd+Space` is used by macOS Spotlight

**Solutions:**
1. Use different default: `Ctrl+Shift+Space`
2. Let user customize in settings
3. Detect conflict and suggest alternatives

---

## 🧠 Natural Language Search

### Query Understanding

**Research Topics:**
- Basic NLP concepts (tokenization, stemming)
- Keyword extraction
- Intent classification
- Fuzzy matching

### Query Parser Logic

**Step 1: Tokenize**
```
Input: "java tutorials from youtube"
Tokens: ["java", "tutorials", "from", "youtube"]
```

**Step 2: Identify Platform**
```
Platform keywords:
- youtube, yt, video → YOUTUBE
- reddit, r/ → REDDIT
- medium, article → MEDIUM
- github, gh, repo → GITHUB
- chrome, firefox, edge → BROWSER

Found: "youtube" → platform = YOUTUBE
```

**Step 3: Identify Content Type**
```
Content type keywords:
- video, vid, watch → VIDEO
- article, post, read → ARTICLE
- podcast, listen → PODCAST

Found: "tutorials" (context: youtube) → type = VIDEO
```

**Step 4: Extract Search Keywords**
```
Remove: platform names, content types, stop words (from, the, a, an)
Remaining: ["java", "tutorials"]
Keywords: ["java", "tutorials"]
```

**Step 5: Build Query**
```
{
    keywords: ["java", "tutorials"],
    platform: "YOUTUBE",
    contentType: "VIDEO"
}
```

### Advanced Features (Future)

**Research Topics:**
- Semantic search with embeddings
- Vector databases (Pinecone, Weaviate)
- LLM-powered search understanding

**Examples:**
```
Query: "that video I saved last week about databases"
Understanding:
- Time filter: last 7 days
- Content type: VIDEO
- Keywords: databases

Query: "spring boot articles but not from baeldung"
Understanding:
- Keywords: spring boot
- Content type: ARTICLE
- Exclude: baeldung.com
```

---

## 📅 Implementation Roadmap

### Phase 1: Backend Search API (Days 1-3)

**Day 1: Basic Search Endpoint**
- [ ] Create `SearchController` with POST `/api/search`
- [ ] Create `SearchRequest` and `SearchResponse` DTOs
- [ ] Basic keyword search in `BookmarkRepository`

**Day 2: Full-Text Search**
- [ ] Add `search_vector` column to bookmarks
- [ ] Create PostgreSQL full-text search index
- [ ] Implement `fullTextSearch` query in repository

**Day 3: Query Parser**
- [ ] Create `QueryParserService`
- [ ] Extract platform from query
- [ ] Extract content type from query
- [ ] Extract keywords from query

### Phase 2: Desktop App Setup (Days 4-6)

**Day 4: Electron Setup**
- [ ] Initialize Electron + React project
- [ ] Configure transparent overlay window
- [ ] Set up IPC communication

**Day 5: Global Shortcut**
- [ ] Register `Ctrl+Space` global shortcut
- [ ] Show/hide window on shortcut
- [ ] Handle focus/blur events

**Day 6: Basic UI**
- [ ] Create SearchBar component
- [ ] Style as floating overlay
- [ ] Connect to backend API

### Phase 3: Search Experience (Days 7-9)

**Day 7: Results Display**
- [ ] Create ResultsList component
- [ ] Fetch and display results
- [ ] Show loading and empty states

**Day 8: Keyboard Navigation**
- [ ] Arrow key navigation
- [ ] Enter to open bookmark
- [ ] Escape to close

**Day 9: Polish**
- [ ] Highlight matching text
- [ ] Platform icons
- [ ] Smooth animations

### Phase 4: Advanced Features (Days 10-12)

**Day 10: Local Cache**
- [ ] Set up SQLite in Electron
- [ ] Cache recent searches
- [ ] Sync bookmarks locally

**Day 11: Settings**
- [ ] Custom hotkey configuration
- [ ] Theme (light/dark)
- [ ] Startup on boot

**Day 12: System Tray**
- [ ] Add system tray icon
- [ ] Quick actions menu
- [ ] Connection status indicator

### Phase 5: Packaging (Days 13-14)

**Day 13: Build Configuration**
- [ ] Configure electron-builder
- [ ] Windows installer (.exe)
- [ ] macOS app (.dmg)
- [ ] Linux package (.AppImage)

**Day 14: Testing & Release**
- [ ] Test on all platforms
- [ ] Create installer/updater
- [ ] Write installation guide

---

## 📚 Research Topics Summary

### Electron Fundamentals
- [ ] Main process vs Renderer process
- [ ] IPC (ipcMain, ipcRenderer)
- [ ] BrowserWindow options
- [ ] globalShortcut module
- [ ] Tray module
- [ ] electron-builder for packaging

### React for Desktop
- [ ] Electron-React integration
- [ ] State management (Zustand/Redux)
- [ ] Keyboard event handling
- [ ] CSS animations/transitions

### Search Implementation
- [ ] PostgreSQL full-text search
- [ ] tsvector and tsquery
- [ ] Search ranking algorithms
- [ ] Debouncing user input

### Natural Language Processing
- [ ] Tokenization basics
- [ ] Stop word removal
- [ ] Keyword extraction
- [ ] Intent classification

### Performance
- [ ] Caching strategies (Redis + Local)
- [ ] Lazy loading results
- [ ] Search indexing
- [ ] Query optimization

---

## 🔗 Useful Resources

### Electron
- [Electron Documentation](https://www.electronjs.org/docs)
- [Electron + React Boilerplate](https://github.com/electron-react-boilerplate/electron-react-boilerplate)

### PostgreSQL Full-Text Search
- [PostgreSQL FTS Documentation](https://www.postgresql.org/docs/current/textsearch.html)
- [Full-Text Search in Spring Boot](https://www.baeldung.com/spring-data-jpa-full-text-search)

### Spotlight-like Apps (Inspiration)
- [Raycast](https://www.raycast.com/) - Modern Spotlight alternative
- [Alfred](https://www.alfredapp.com/) - macOS productivity app
- [Cerebro](https://github.com/cerebroapp/cerebro) - Open source Electron launcher

---

*Last Updated: January 2026*
