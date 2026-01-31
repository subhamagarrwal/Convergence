# Convergence Frontend Guide - TanStack Query & Optimizations

## 📋 Table of Contents
1. [Overview](#overview)
2. [TanStack Query Fundamentals](#tanstack-query-fundamentals)
3. [Project Setup](#project-setup)
4. [API Layer Architecture](#api-layer-architecture)
5. [Query Patterns](#query-patterns)
6. [Mutations & Optimistic Updates](#mutations--optimistic-updates)
7. [Caching Strategies](#caching-strategies)
8. [Desktop App Optimizations](#desktop-app-optimizations)
9. [Browser Extension Optimizations](#browser-extension-optimizations)
10. [Performance Best Practices](#performance-best-practices)
11. [Implementation Roadmap](#implementation-roadmap)

---

## 🎯 Overview

### What We're Building
A **high-performance frontend** for Convergence using:
- **TanStack Query (React Query v5)** - Server state management
- **React 18+** - UI framework
- **TypeScript** - Type safety
- **Zustand** - Client state management
- **Tailwind CSS** - Styling

### Why TanStack Query?

| Traditional Approach | TanStack Query |
|---------------------|----------------|
| Manual loading states | Automatic loading/error states |
| Custom caching logic | Built-in intelligent caching |
| Manual refetching | Auto background refetching |
| Complex state management | Declarative data fetching |
| No offline support | Built-in offline support |

---

## 📚 TanStack Query Fundamentals

### Core Concepts

```
┌─────────────────────────────────────────────────────────────┐
│                    TANSTACK QUERY                            │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Queries   │  │  Mutations  │  │   Query Client      │ │
│  │   (GET)     │  │  (POST/PUT) │  │   (Cache Manager)   │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
│         │                │                     │            │
│         └────────────────┼─────────────────────┘            │
│                          │                                   │
│                          ▼                                   │
│              ┌─────────────────────┐                        │
│              │    Query Cache      │                        │
│              │   (In-Memory)       │                        │
│              └─────────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Key Terms

| Term | Description | Example |
|------|-------------|---------|
| **Query** | Fetches data (GET requests) | `useQuery(['bookmarks'], fetchBookmarks)` |
| **Mutation** | Modifies data (POST/PUT/DELETE) | `useMutation(createBookmark)` |
| **Query Key** | Unique identifier for cached data | `['bookmarks', userId, { platform: 'REDDIT' }]` |
| **Stale Time** | How long data is considered fresh | `staleTime: 5 * 60 * 1000` (5 min) |
| **Cache Time** | How long unused data stays in cache | `gcTime: 30 * 60 * 1000` (30 min) |
| **Invalidation** | Mark cached data as outdated | `queryClient.invalidateQueries(['bookmarks'])` |

---

## 🛠️ Project Setup

### Installation

```bash
# For React project (Desktop or Web)
npm install @tanstack/react-query @tanstack/react-query-devtools

# Additional utilities
npm install axios zustand
```

### Project Structure

```
src/
├── api/                      # API layer
│   ├── client.ts             # Axios instance
│   ├── endpoints.ts          # API endpoints
│   └── types.ts              # API response types
│
├── hooks/                    # Custom hooks
│   ├── queries/              # TanStack Query hooks
│   │   ├── useBookmarks.ts
│   │   ├── useUser.ts
│   │   ├── usePlatformConnections.ts
│   │   └── useSearch.ts
│   └── mutations/            # Mutation hooks
│       ├── useCreateBookmark.ts
│       ├── useDeleteBookmark.ts
│       └── useConnectPlatform.ts
│
├── providers/                # Context providers
│   └── QueryProvider.tsx
│
├── store/                    # Zustand stores (client state)
│   ├── authStore.ts
│   └── uiStore.ts
│
├── components/               # UI components
├── pages/                    # Page components
└── utils/                    # Utilities
```

### Query Provider Setup

```typescript
// src/providers/QueryProvider.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ReactNode, useState } from 'react';

// Default options for all queries
const createQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,      // 5 minutes
      gcTime: 30 * 60 * 1000,        // 30 minutes (formerly cacheTime)
      retry: 3,                       // Retry failed requests 3 times
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
      refetchOnWindowFocus: true,    // Refetch when window regains focus
      refetchOnReconnect: true,      // Refetch when network reconnects
    },
    mutations: {
      retry: 1,
    },
  },
});

interface QueryProviderProps {
  children: ReactNode;
}

export function QueryProvider({ children }: QueryProviderProps) {
  // Create client once per app instance
  const [queryClient] = useState(() => createQueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {/* DevTools only in development */}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  );
}
```

### App Entry Point

```typescript
// src/App.tsx
import { QueryProvider } from './providers/QueryProvider';
import { Router } from './Router';

function App() {
  return (
    <QueryProvider>
      <Router />
    </QueryProvider>
  );
}

export default App;
```

---

## 🔌 API Layer Architecture

### Axios Client Setup

```typescript
// src/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle errors & token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;
    
    // Handle 401 - Token expired
    if (error.response?.status === 401 && originalRequest) {
      try {
        // Try to refresh token
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        // Retry original request
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);
```

### API Endpoints

```typescript
// src/api/endpoints.ts
import { apiClient } from './client';
import type {
  Bookmark,
  BookmarkFilters,
  CreateBookmarkRequest,
  PaginatedResponse,
  PlatformConnection,
  SearchRequest,
  SearchResponse,
  User,
} from './types';

// ============ AUTH ============
export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<{ accessToken: string; refreshToken: string }>('/auth/login', {
      email,
      password,
    }),
    
  register: (data: { username: string; email: string; password: string }) =>
    apiClient.post<User>('/auth/register', data),
    
  logout: () => apiClient.post('/auth/logout'),
  
  getCurrentUser: () => apiClient.get<User>('/auth/me'),
};

// ============ BOOKMARKS ============
export const bookmarksApi = {
  getAll: (filters?: BookmarkFilters) =>
    apiClient.get<PaginatedResponse<Bookmark>>('/bookmarks', { params: filters }),
    
  getById: (id: string) =>
    apiClient.get<Bookmark>(`/bookmarks/${id}`),
    
  create: (data: CreateBookmarkRequest) =>
    apiClient.post<Bookmark>('/bookmarks', data),
    
  update: (id: string, data: Partial<CreateBookmarkRequest>) =>
    apiClient.put<Bookmark>(`/bookmarks/${id}`, data),
    
  delete: (id: string) =>
    apiClient.delete(`/bookmarks/${id}`),
    
  search: (data: SearchRequest) =>
    apiClient.post<SearchResponse>('/search', data),
};

// ============ PLATFORM CONNECTIONS ============
export const platformsApi = {
  getAll: () =>
    apiClient.get<PlatformConnection[]>('/platforms'),
    
  connect: (platform: string, authCode: string) =>
    apiClient.post<PlatformConnection>('/platforms/connect', { platform, authCode }),
    
  disconnect: (platformId: string) =>
    apiClient.delete(`/platforms/${platformId}`),
    
  sync: (platformId: string) =>
    apiClient.post<{ syncedCount: number }>(`/platforms/${platformId}/sync`),
};
```

### API Types

```typescript
// src/api/types.ts

// ============ ENUMS ============
export type PlatformType = 'YOUTUBE' | 'REDDIT' | 'MEDIUM' | 'X' | 'CHROME' | 'FIREFOX' | 'EDGE' | 'OTHER';
export type ContentType = 'VIDEO' | 'ARTICLE' | 'POST' | 'PODCAST' | 'OTHER';
export type UserRole = 'USER' | 'ADMIN' | 'SUPER_ADMIN';
export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'DELETED';

// ============ ENTITIES ============
export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface Bookmark {
  id: string;
  url: string;
  title: string;
  description: string | null;
  platform: PlatformType;
  contentType: ContentType;
  metadata: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformConnection {
  id: string;
  platform: PlatformType;
  platformUsername: string;
  isActive: boolean;
  connectedAt: string;
  lastSyncAt: string;
}

// ============ REQUESTS ============
export interface BookmarkFilters {
  platform?: PlatformType;
  contentType?: ContentType;
  search?: string;
  page?: number;
  limit?: number;
  sortBy?: 'createdAt' | 'title';
  sortOrder?: 'asc' | 'desc';
}

export interface CreateBookmarkRequest {
  url: string;
  title: string;
  description?: string;
  platform: PlatformType;
  contentType: ContentType;
}

export interface SearchRequest {
  query: string;
  limit?: number;
  offset?: number;
}

// ============ RESPONSES ============
export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface SearchResponse {
  results: SearchResult[];
  total: number;
  queryParsed: {
    keywords: string[];
    platform: PlatformType | null;
    contentType: ContentType | null;
  };
}

export interface SearchResult extends Bookmark {
  relevanceScore: number;
  highlightedTitle: string;
}
```

---

## 🔍 Query Patterns

### Basic Query Hook

```typescript
// src/hooks/queries/useBookmarks.ts
import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { bookmarksApi } from '@/api/endpoints';
import type { BookmarkFilters } from '@/api/types';

// Query key factory - Consistent key generation
export const bookmarkKeys = {
  all: ['bookmarks'] as const,
  lists: () => [...bookmarkKeys.all, 'list'] as const,
  list: (filters: BookmarkFilters) => [...bookmarkKeys.lists(), filters] as const,
  details: () => [...bookmarkKeys.all, 'detail'] as const,
  detail: (id: string) => [...bookmarkKeys.details(), id] as const,
};

// ============ FETCH ALL BOOKMARKS ============
export function useBookmarks(filters?: BookmarkFilters) {
  return useQuery({
    queryKey: bookmarkKeys.list(filters ?? {}),
    queryFn: async () => {
      const response = await bookmarksApi.getAll(filters);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// ============ FETCH SINGLE BOOKMARK ============
export function useBookmark(id: string) {
  return useQuery({
    queryKey: bookmarkKeys.detail(id),
    queryFn: async () => {
      const response = await bookmarksApi.getById(id);
      return response.data;
    },
    enabled: !!id, // Only fetch if id exists
  });
}

// ============ INFINITE SCROLL ============
export function useInfiniteBookmarks(filters?: Omit<BookmarkFilters, 'page'>) {
  return useInfiniteQuery({
    queryKey: [...bookmarkKeys.list(filters ?? {}), 'infinite'],
    queryFn: async ({ pageParam = 1 }) => {
      const response = await bookmarksApi.getAll({ ...filters, page: pageParam });
      return response.data;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      if (lastPage.page < lastPage.totalPages) {
        return lastPage.page + 1;
      }
      return undefined;
    },
  });
}
```

### Search Query with Debouncing

```typescript
// src/hooks/queries/useSearch.ts
import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { bookmarksApi } from '@/api/endpoints';

// Debounce hook
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}

// Search query keys
export const searchKeys = {
  all: ['search'] as const,
  query: (q: string) => [...searchKeys.all, q] as const,
};

// ============ SEARCH HOOK ============
export function useSearch(query: string, options?: { enabled?: boolean }) {
  // Debounce search query (300ms)
  const debouncedQuery = useDebounce(query, 300);

  return useQuery({
    queryKey: searchKeys.query(debouncedQuery),
    queryFn: async () => {
      const response = await bookmarksApi.search({ query: debouncedQuery });
      return response.data;
    },
    enabled: (options?.enabled ?? true) && debouncedQuery.length >= 2,
    staleTime: 2 * 60 * 1000, // 2 minutes for search results
    gcTime: 10 * 60 * 1000,   // Keep in cache for 10 minutes
  });
}

// ============ SEARCH WITH FILTERS ============
export function useSearchWithFilters(
  query: string,
  filters: { platform?: string; contentType?: string }
) {
  const debouncedQuery = useDebounce(query, 300);

  return useQuery({
    queryKey: [...searchKeys.query(debouncedQuery), filters],
    queryFn: async () => {
      const response = await bookmarksApi.search({
        query: debouncedQuery,
        ...filters,
      });
      return response.data;
    },
    enabled: debouncedQuery.length >= 2,
  });
}
```

### Platform Connections Query

```typescript
// src/hooks/queries/usePlatformConnections.ts
import { useQuery } from '@tanstack/react-query';
import { platformsApi } from '@/api/endpoints';

export const platformKeys = {
  all: ['platforms'] as const,
  list: () => [...platformKeys.all, 'list'] as const,
};

export function usePlatformConnections() {
  return useQuery({
    queryKey: platformKeys.list(),
    queryFn: async () => {
      const response = await platformsApi.getAll();
      return response.data;
    },
    staleTime: 10 * 60 * 1000, // 10 minutes - connections don't change often
  });
}

// Check if specific platform is connected
export function useIsPlatformConnected(platform: string) {
  const { data: connections } = usePlatformConnections();
  
  return connections?.some(
    (conn) => conn.platform === platform && conn.isActive
  ) ?? false;
}
```

---

## ✏️ Mutations & Optimistic Updates

### Create Bookmark Mutation

```typescript
// src/hooks/mutations/useCreateBookmark.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { bookmarksApi } from '@/api/endpoints';
import { bookmarkKeys } from '@/hooks/queries/useBookmarks';
import type { Bookmark, CreateBookmarkRequest } from '@/api/types';

export function useCreateBookmark() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateBookmarkRequest) => {
      const response = await bookmarksApi.create(data);
      return response.data;
    },
    
    // ============ OPTIMISTIC UPDATE ============
    onMutate: async (newBookmark) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: bookmarkKeys.lists() });

      // Snapshot previous value
      const previousBookmarks = queryClient.getQueryData(bookmarkKeys.lists());

      // Optimistically update cache
      queryClient.setQueryData(bookmarkKeys.lists(), (old: any) => {
        if (!old) return old;
        
        const optimisticBookmark: Bookmark = {
          id: `temp-${Date.now()}`, // Temporary ID
          ...newBookmark,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          description: newBookmark.description ?? null,
          metadata: null,
        };
        
        return {
          ...old,
          data: [optimisticBookmark, ...old.data],
          total: old.total + 1,
        };
      });

      // Return context with snapshot
      return { previousBookmarks };
    },
    
    // ============ ERROR - ROLLBACK ============
    onError: (err, newBookmark, context) => {
      // Rollback to previous state
      if (context?.previousBookmarks) {
        queryClient.setQueryData(bookmarkKeys.lists(), context.previousBookmarks);
      }
    },
    
    // ============ SUCCESS ============
    onSuccess: (data) => {
      // Replace optimistic bookmark with real one
      queryClient.setQueryData(bookmarkKeys.lists(), (old: any) => {
        if (!old) return old;
        
        return {
          ...old,
          data: old.data.map((bookmark: Bookmark) =>
            bookmark.id.startsWith('temp-') ? data : bookmark
          ),
        };
      });
    },
    
    // ============ ALWAYS - CLEANUP ============
    onSettled: () => {
      // Refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: bookmarkKeys.lists() });
    },
  });
}
```

### Delete Bookmark Mutation

```typescript
// src/hooks/mutations/useDeleteBookmark.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { bookmarksApi } from '@/api/endpoints';
import { bookmarkKeys } from '@/hooks/queries/useBookmarks';
import type { Bookmark } from '@/api/types';

export function useDeleteBookmark() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await bookmarksApi.delete(id);
      return id;
    },
    
    // Optimistic delete
    onMutate: async (deletedId) => {
      await queryClient.cancelQueries({ queryKey: bookmarkKeys.lists() });
      
      const previousBookmarks = queryClient.getQueryData(bookmarkKeys.lists());
      
      // Remove from cache immediately
      queryClient.setQueryData(bookmarkKeys.lists(), (old: any) => {
        if (!old) return old;
        return {
          ...old,
          data: old.data.filter((b: Bookmark) => b.id !== deletedId),
          total: old.total - 1,
        };
      });
      
      return { previousBookmarks };
    },
    
    onError: (err, deletedId, context) => {
      if (context?.previousBookmarks) {
        queryClient.setQueryData(bookmarkKeys.lists(), context.previousBookmarks);
      }
    },
    
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: bookmarkKeys.lists() });
    },
  });
}
```

### Connect Platform Mutation

```typescript
// src/hooks/mutations/useConnectPlatform.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { platformsApi } from '@/api/endpoints';
import { platformKeys } from '@/hooks/queries/usePlatformConnections';

export function useConnectPlatform() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ platform, authCode }: { platform: string; authCode: string }) => {
      const response = await platformsApi.connect(platform, authCode);
      return response.data;
    },
    
    onSuccess: () => {
      // Invalidate platform connections to refetch
      queryClient.invalidateQueries({ queryKey: platformKeys.list() });
      // Also invalidate bookmarks since we might have new ones
      queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
    },
  });
}

export function useSyncPlatform() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (platformId: string) => {
      const response = await platformsApi.sync(platformId);
      return response.data;
    },
    
    onSuccess: () => {
      // Refetch bookmarks after sync
      queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
      queryClient.invalidateQueries({ queryKey: platformKeys.list() });
    },
  });
}
```

---

## 💾 Caching Strategies

### Cache Configuration by Data Type

```typescript
// src/config/queryConfig.ts

export const queryConfig = {
  // User data - rarely changes
  user: {
    staleTime: 30 * 60 * 1000,  // 30 minutes
    gcTime: 60 * 60 * 1000,     // 1 hour
  },
  
  // Bookmarks - moderate freshness needed
  bookmarks: {
    staleTime: 5 * 60 * 1000,   // 5 minutes
    gcTime: 30 * 60 * 1000,     // 30 minutes
  },
  
  // Search results - short cache
  search: {
    staleTime: 2 * 60 * 1000,   // 2 minutes
    gcTime: 10 * 60 * 1000,     // 10 minutes
  },
  
  // Platform connections - rarely changes
  platforms: {
    staleTime: 10 * 60 * 1000,  // 10 minutes
    gcTime: 60 * 60 * 1000,     // 1 hour
  },
};
```

### Prefetching Data

```typescript
// Prefetch on hover (for faster navigation)
function BookmarkCard({ bookmark }: { bookmark: Bookmark }) {
  const queryClient = useQueryClient();
  
  const prefetchDetails = () => {
    queryClient.prefetchQuery({
      queryKey: bookmarkKeys.detail(bookmark.id),
      queryFn: () => bookmarksApi.getById(bookmark.id).then(r => r.data),
      staleTime: 5 * 60 * 1000,
    });
  };
  
  return (
    <div onMouseEnter={prefetchDetails}>
      {/* Card content */}
    </div>
  );
}
```

### Persisting Cache (Offline Support)

```typescript
// src/providers/QueryProvider.tsx
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister';

const persister = createSyncStoragePersister({
  storage: window.localStorage,
  key: 'convergence-cache',
});

export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => createQueryClient());

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister, maxAge: 24 * 60 * 60 * 1000 }} // 24 hours
    >
      {children}
    </PersistQueryClientProvider>
  );
}
```

---

## 🖥️ Desktop App Optimizations

### Electron-Specific Optimizations

```
┌─────────────────────────────────────────────────────────────┐
│                 DESKTOP OPTIMIZATIONS                        │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  1. LOCAL SQLITE CACHE                                │  │
│  │     - Store bookmarks locally                         │  │
│  │     - Instant startup                                 │  │
│  │     - Offline access                                  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  2. BACKGROUND SYNC                                   │  │
│  │     - Sync with server in background                  │  │
│  │     - Don't block UI                                  │  │
│  │     - Show sync status indicator                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  3. WINDOW STATE MANAGEMENT                           │  │
│  │     - Remember window position                        │  │
│  │     - Minimize to tray                                │  │
│  │     - Quick show/hide                                 │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Local SQLite Integration

```typescript
// src/services/localDb.ts (Electron renderer)
import Database from 'better-sqlite3';

class LocalDatabase {
  private db: Database.Database;
  
  constructor() {
    const dbPath = window.electron.getPath('userData') + '/convergence.db';
    this.db = new Database(dbPath);
    this.initialize();
  }
  
  private initialize() {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS bookmarks (
        id TEXT PRIMARY KEY,
        url TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT,
        platform TEXT NOT NULL,
        content_type TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        synced_at TEXT
      );
      
      CREATE INDEX IF NOT EXISTS idx_platform ON bookmarks(platform);
      CREATE INDEX IF NOT EXISTS idx_created ON bookmarks(created_at);
      CREATE VIRTUAL TABLE IF NOT EXISTS bookmarks_fts USING fts5(title, description);
    `);
  }
  
  // Fast local search
  searchBookmarks(query: string): Bookmark[] {
    const stmt = this.db.prepare(`
      SELECT b.* FROM bookmarks b
      INNER JOIN bookmarks_fts fts ON b.id = fts.rowid
      WHERE bookmarks_fts MATCH ?
      ORDER BY rank
      LIMIT 50
    `);
    return stmt.all(query);
  }
  
  // Cache bookmarks locally
  upsertBookmarks(bookmarks: Bookmark[]) {
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO bookmarks 
      (id, url, title, description, platform, content_type, created_at, updated_at, synced_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
    `);
    
    const transaction = this.db.transaction((items: Bookmark[]) => {
      for (const b of items) {
        stmt.run(b.id, b.url, b.title, b.description, b.platform, b.contentType, b.createdAt, b.updatedAt);
      }
    });
    
    transaction(bookmarks);
  }
}

export const localDb = new LocalDatabase();
```

### Hybrid Query (Local + Remote)

```typescript
// src/hooks/queries/useHybridSearch.ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { localDb } from '@/services/localDb';
import { bookmarksApi } from '@/api/endpoints';

export function useHybridSearch(query: string) {
  const queryClient = useQueryClient();
  
  // Step 1: Instant local search
  const localResults = useQuery({
    queryKey: ['search', 'local', query],
    queryFn: () => localDb.searchBookmarks(query),
    enabled: query.length >= 2,
    staleTime: Infinity, // Local data doesn't go stale
  });
  
  // Step 2: Background remote search
  const remoteResults = useQuery({
    queryKey: ['search', 'remote', query],
    queryFn: async () => {
      const response = await bookmarksApi.search({ query });
      // Update local cache with remote results
      localDb.upsertBookmarks(response.data.results);
      return response.data;
    },
    enabled: query.length >= 2,
    staleTime: 2 * 60 * 1000,
  });
  
  // Combine: Show local immediately, merge with remote when ready
  return {
    data: remoteResults.data?.results ?? localResults.data ?? [],
    isLoading: localResults.isLoading,
    isFetching: remoteResults.isFetching,
    isStale: !remoteResults.data,
  };
}
```

### Window Performance

```typescript
// src/hooks/useWindowOptimizations.ts
import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export function useWindowOptimizations() {
  const queryClient = useQueryClient();
  
  useEffect(() => {
    // Pause queries when window is hidden
    const handleVisibilityChange = () => {
      if (document.hidden) {
        queryClient.setDefaultOptions({
          queries: { enabled: false },
        });
      } else {
        queryClient.setDefaultOptions({
          queries: { enabled: true },
        });
        // Refetch stale queries when visible again
        queryClient.refetchQueries({ stale: true });
      }
    };
    
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [queryClient]);
}
```

### Spotlight Search Component

```typescript
// src/components/SpotlightSearch.tsx
import { useState, useEffect, useRef } from 'react';
import { useHybridSearch } from '@/hooks/queries/useHybridSearch';

export function SpotlightSearch() {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  
  const { data: results, isLoading, isFetching } = useHybridSearch(query);
  
  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex(i => Math.min(i + 1, results.length - 1));
          break;
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex(i => Math.max(i - 1, 0));
          break;
        case 'Enter':
          if (results[selectedIndex]) {
            window.open(results[selectedIndex].url, '_blank');
          }
          break;
        case 'Escape':
          window.electron.hideWindow();
          break;
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [results, selectedIndex]);
  
  // Auto-focus on show
  useEffect(() => {
    inputRef.current?.focus();
  }, []);
  
  return (
    <div className="spotlight-container">
      <div className="search-input-wrapper">
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search your bookmarks..."
          className="spotlight-input"
        />
        {isFetching && <span className="sync-indicator">⟳</span>}
      </div>
      
      <div className="results-list">
        {results.map((result, index) => (
          <div
            key={result.id}
            className={`result-item ${index === selectedIndex ? 'selected' : ''}`}
            onClick={() => window.open(result.url, '_blank')}
          >
            <span className="platform-icon">{getPlatformIcon(result.platform)}</span>
            <span className="result-title">{result.title}</span>
          </div>
        ))}
        
        {query.length >= 2 && results.length === 0 && !isLoading && (
          <div className="no-results">No bookmarks found</div>
        )}
      </div>
    </div>
  );
}
```

---

## 🌐 Browser Extension Optimizations

### Extension-Specific Challenges

```
┌─────────────────────────────────────────────────────────────┐
│              BROWSER EXTENSION CHALLENGES                    │
│                                                              │
│  ❌ Limited memory (popup closes = memory cleared)          │
│  ❌ No persistent connections (service worker sleeps)       │
│  ❌ Small popup window (max 800x600)                        │
│  ❌ Cross-origin restrictions                               │
│                                                              │
│              SOLUTIONS                                       │
│                                                              │
│  ✅ Use chrome.storage for persistence                      │
│  ✅ Aggressive prefetching                                  │
│  ✅ Compact UI design                                       │
│  ✅ Background sync via alarms                              │
└─────────────────────────────────────────────────────────────┘
```

### Chrome Storage Persistence

```typescript
// src/services/extensionStorage.ts

interface CachedData<T> {
  data: T;
  timestamp: number;
  expiresAt: number;
}

export const extensionStorage = {
  async get<T>(key: string): Promise<T | null> {
    return new Promise((resolve) => {
      chrome.storage.local.get(key, (result) => {
        const cached = result[key] as CachedData<T> | undefined;
        
        if (!cached) {
          resolve(null);
          return;
        }
        
        // Check expiration
        if (Date.now() > cached.expiresAt) {
          chrome.storage.local.remove(key);
          resolve(null);
          return;
        }
        
        resolve(cached.data);
      });
    });
  },
  
  async set<T>(key: string, data: T, ttlMs: number = 5 * 60 * 1000): Promise<void> {
    const cached: CachedData<T> = {
      data,
      timestamp: Date.now(),
      expiresAt: Date.now() + ttlMs,
    };
    
    return new Promise((resolve) => {
      chrome.storage.local.set({ [key]: cached }, resolve);
    });
  },
  
  async remove(key: string): Promise<void> {
    return new Promise((resolve) => {
      chrome.storage.local.remove(key, resolve);
    });
  },
};
```

### Custom Storage Persister for Extension

```typescript
// src/providers/ExtensionQueryProvider.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { extensionStorage } from '@/services/extensionStorage';

// Custom persister using chrome.storage
const extensionPersister = {
  persistClient: async (client: any) => {
    await extensionStorage.set('query-cache', client, 24 * 60 * 60 * 1000);
  },
  restoreClient: async () => {
    return await extensionStorage.get('query-cache');
  },
  removeClient: async () => {
    await extensionStorage.remove('query-cache');
  },
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 2 * 60 * 1000,     // 2 minutes
      gcTime: 10 * 60 * 1000,       // 10 minutes
      retry: 1,                      // Minimal retries for extension
      refetchOnWindowFocus: false,   // Popup closes on blur anyway
    },
  },
});

export function ExtensionQueryProvider({ children }: { children: ReactNode }) {
  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister: extensionPersister }}
    >
      {children}
    </PersistQueryClientProvider>
  );
}
```

### Background Sync with Alarms

```typescript
// src/background/sync.ts (Service Worker)

// Set up periodic sync
chrome.alarms.create('sync-bookmarks', { periodInMinutes: 60 });

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name === 'sync-bookmarks') {
    await syncBookmarks();
  }
});

async function syncBookmarks() {
  const token = await chrome.storage.local.get('accessToken');
  if (!token.accessToken) return;
  
  try {
    const response = await fetch('http://localhost:8080/api/bookmarks', {
      headers: { Authorization: `Bearer ${token.accessToken}` },
    });
    
    if (response.ok) {
      const bookmarks = await response.json();
      await chrome.storage.local.set({
        'cached-bookmarks': {
          data: bookmarks,
          timestamp: Date.now(),
          expiresAt: Date.now() + 60 * 60 * 1000, // 1 hour
        },
      });
    }
  } catch (error) {
    console.error('Sync failed:', error);
  }
}

// Sync on extension install/update
chrome.runtime.onInstalled.addListener(() => {
  syncBookmarks();
});
```

### Extension Popup Component

```typescript
// src/popup/App.tsx
import { useState, useEffect } from 'react';
import { useBookmarks } from '@/hooks/queries/useBookmarks';
import { useSearch } from '@/hooks/queries/useSearch';

export function PopupApp() {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  
  // Use cached bookmarks for instant display
  const { data: recentBookmarks, isLoading } = useBookmarks({ limit: 5 });
  const { data: searchResults } = useSearch(query);
  
  const displayItems = query.length >= 2 
    ? searchResults?.results ?? []
    : recentBookmarks?.data ?? [];
  
  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex(i => Math.min(i + 1, displayItems.length - 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex(i => Math.max(i - 1, 0));
      } else if (e.key === 'Enter' && displayItems[selectedIndex]) {
        chrome.tabs.create({ url: displayItems[selectedIndex].url });
        window.close();
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [displayItems, selectedIndex]);
  
  return (
    <div className="popup-container w-[400px] max-h-[500px]">
      {/* Search Input */}
      <div className="p-3 border-b">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search bookmarks..."
          className="w-full px-3 py-2 rounded-lg bg-gray-100 focus:outline-none"
          autoFocus
        />
      </div>
      
      {/* Results */}
      <div className="overflow-y-auto max-h-[400px]">
        {isLoading ? (
          <div className="p-4 text-center text-gray-500">Loading...</div>
        ) : displayItems.length === 0 ? (
          <div className="p-4 text-center text-gray-500">
            {query.length >= 2 ? 'No results found' : 'No recent bookmarks'}
          </div>
        ) : (
          displayItems.map((item, index) => (
            <div
              key={item.id}
              className={`px-3 py-2 cursor-pointer flex items-center gap-3
                ${index === selectedIndex ? 'bg-blue-50' : 'hover:bg-gray-50'}`}
              onClick={() => {
                chrome.tabs.create({ url: item.url });
                window.close();
              }}
            >
              <img 
                src={`https://www.google.com/s2/favicons?domain=${new URL(item.url).hostname}`}
                className="w-4 h-4"
                alt=""
              />
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium truncate">{item.title}</div>
                <div className="text-xs text-gray-500 truncate">{item.url}</div>
              </div>
              <span className="text-xs text-gray-400 uppercase">{item.platform}</span>
            </div>
          ))
        )}
      </div>
      
      {/* Footer */}
      <div className="p-2 border-t flex justify-between text-xs text-gray-500">
        <span>↑↓ Navigate</span>
        <span>↵ Open</span>
        <span>Esc Close</span>
      </div>
    </div>
  );
}
```

---

## ⚡ Performance Best Practices

### 1. Query Key Factories

```typescript
// Consistent, type-safe query keys
export const queryKeys = {
  bookmarks: {
    all: ['bookmarks'] as const,
    lists: () => [...queryKeys.bookmarks.all, 'list'] as const,
    list: (filters: BookmarkFilters) => [...queryKeys.bookmarks.lists(), filters] as const,
    details: () => [...queryKeys.bookmarks.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.bookmarks.details(), id] as const,
  },
  search: {
    all: ['search'] as const,
    query: (q: string) => [...queryKeys.search.all, q] as const,
  },
  platforms: {
    all: ['platforms'] as const,
    list: () => [...queryKeys.platforms.all, 'list'] as const,
  },
  user: {
    current: ['user', 'current'] as const,
  },
};
```

### 2. Selective Invalidation

```typescript
// ❌ Bad - Invalidates everything
queryClient.invalidateQueries();

// ✅ Good - Invalidate specific queries
queryClient.invalidateQueries({ queryKey: queryKeys.bookmarks.lists() });

// ✅ Better - Invalidate only affected data
queryClient.setQueryData(
  queryKeys.bookmarks.detail(id),
  (old) => ({ ...old, title: newTitle })
);
```

### 3. Parallel Queries

```typescript
// Fetch multiple independent queries in parallel
import { useQueries } from '@tanstack/react-query';

function Dashboard() {
  const results = useQueries({
    queries: [
      {
        queryKey: queryKeys.bookmarks.list({}),
        queryFn: () => bookmarksApi.getAll().then(r => r.data),
      },
      {
        queryKey: queryKeys.platforms.list(),
        queryFn: () => platformsApi.getAll().then(r => r.data),
      },
      {
        queryKey: queryKeys.user.current,
        queryFn: () => authApi.getCurrentUser().then(r => r.data),
      },
    ],
  });
  
  const [bookmarks, platforms, user] = results;
  
  // All three load in parallel!
}
```

### 4. Suspense Mode (React 18+)

```typescript
// Enable suspense for cleaner loading states
const { data } = useSuspenseQuery({
  queryKey: queryKeys.bookmarks.list({}),
  queryFn: () => bookmarksApi.getAll().then(r => r.data),
});

// Wrap in Suspense
<Suspense fallback={<LoadingSpinner />}>
  <BookmarksList />
</Suspense>
```

### 5. Request Deduplication

```typescript
// TanStack Query automatically deduplicates identical requests
// These two components will only make ONE API call:

function ComponentA() {
  const { data } = useBookmarks(); // ← Makes request
}

function ComponentB() {
  const { data } = useBookmarks(); // ← Uses cached response
}
```

---

## 📅 Implementation Roadmap

### Phase 1: Foundation (Days 1-3)
- [ ] Set up TanStack Query provider
- [ ] Create API client with interceptors
- [ ] Define TypeScript types
- [ ] Create query key factories

### Phase 2: Core Queries (Days 4-6)
- [ ] Implement `useBookmarks` hook
- [ ] Implement `useSearch` hook with debouncing
- [ ] Implement `usePlatformConnections` hook
- [ ] Add loading/error states

### Phase 3: Mutations (Days 7-9)
- [ ] Implement `useCreateBookmark` with optimistic updates
- [ ] Implement `useDeleteBookmark` with optimistic updates
- [ ] Implement platform connection mutations
- [ ] Add error handling and rollback

### Phase 4: Desktop Optimizations (Days 10-12)
- [ ] Set up SQLite local cache
- [ ] Implement hybrid search (local + remote)
- [ ] Add offline support
- [ ] Optimize window performance

### Phase 5: Extension Optimizations (Days 13-14)
- [ ] Set up chrome.storage persistence
- [ ] Implement background sync
- [ ] Create compact popup UI
- [ ] Test and optimize bundle size

---

## 📚 Research Topics

### TanStack Query
- [ ] Query lifecycle and states
- [ ] Optimistic updates pattern
- [ ] Infinite queries for pagination
- [ ] Query invalidation strategies
- [ ] Suspense mode

### Performance
- [ ] React.memo and useMemo usage
- [ ] Virtual lists for large datasets
- [ ] Bundle splitting
- [ ] Service Worker caching

### Desktop (Electron)
- [ ] better-sqlite3 for local storage
- [ ] IPC communication patterns
- [ ] Window state management

### Browser Extension
- [ ] Manifest V3 service workers
- [ ] chrome.storage API
- [ ] chrome.alarms for periodic tasks
- [ ] Content Security Policy

---

## 🔗 Resources

- [TanStack Query Docs](https://tanstack.com/query/latest)
- [TanStack Query DevTools](https://tanstack.com/query/latest/docs/react/devtools)
- [React Query Patterns](https://tkdodo.eu/blog/practical-react-query)
- [Electron + React](https://www.electronjs.org/docs/latest/tutorial/quick-start)
- [Chrome Extension Docs](https://developer.chrome.com/docs/extensions/)

---

*Last Updated: February 2026*
