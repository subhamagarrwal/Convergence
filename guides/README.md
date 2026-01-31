# Convergence - Complete Project Guide

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Database Design](#database-design)
4. [Spring Boot Concepts](#spring-boot-concepts)
5. [API Design](#api-design)
6. [Security Implementation](#security-implementation)
7. [Background Processing](#background-processing)
8. [Interview Questions](#interview-questions)

---

## 🎯 Project Overview

### What is Convergence?
Convergence is a **bookmark aggregation platform** that consolidates bookmarks from multiple sources:
- **Browsers:** Chrome, Firefox, Edge
- **Social Platforms:** Reddit (saved posts), YouTube (liked videos), X (bookmarks)
- **Content Platforms:** Medium, GitHub (starred repos), LinkedIn

### Core Features
1. **Multi-Platform Authentication** - OAuth2 integration with Reddit, YouTube, etc.
2. **Bookmark Aggregation** - Fetch and store bookmarks from connected platforms
3. **Unified Search** - Search across all bookmarks regardless of source
4. **Background Sync** - Scheduled jobs to keep bookmarks updated
5. **Session Management** - Handle OAuth token refresh and expiration

### Tech Stack
| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 4.0.2, Java 21 |
| Database | PostgreSQL |
| Caching | Redis |
| Message Queue | RabbitMQ |
| Authentication | JWT + OAuth2 |
| API Documentation | SpringDoc OpenAPI (Swagger) |
| Build Tool | Maven |

---

## 🏗️ System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
│  (Browser Extension / Web App / Mobile App)                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API GATEWAY                              │
│  (Rate Limiting, Authentication, Request Routing)                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT APPLICATION                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Controllers │  │  Services   │  │ Repositories│              │
│  │ (REST API)  │──│ (Business)  │──│ (Data)      │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│         │               │                │                       │
│         │               │                │                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Security  │  │  Scheduler  │  │   Events    │              │
│  │   (JWT)     │  │  (Cron)     │  │ (RabbitMQ)  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
         │                   │                │
         ▼                   ▼                ▼
┌─────────────┐      ┌─────────────┐   ┌─────────────┐
│ PostgreSQL  │      │    Redis    │   │  RabbitMQ   │
│ (Primary DB)│      │  (Cache)    │   │  (Queue)    │
└─────────────┘      └─────────────┘   └─────────────┘
```

### Application Layers

#### 1. Controller Layer (Presentation)
- Handles HTTP requests
- Input validation
- Response formatting
- No business logic

#### 2. Service Layer (Business Logic)
- Core business rules
- Transaction management
- Orchestrates multiple repositories
- Caching decisions

#### 3. Repository Layer (Data Access)
- Database queries
- CRUD operations
- Custom query methods

#### 4. Model Layer (Domain)
- Entity definitions
- Relationships
- Validation constraints

### Request Flow

```
HTTP Request
    │
    ▼
┌─────────────────┐
│ Security Filter │ ← JWT Validation
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Controller    │ ← @RestController
└─────────────────┘
    │
    ▼
┌─────────────────┐
│    Service      │ ← @Service, @Transactional
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Repository    │ ← @Repository
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Database      │
└─────────────────┘
```

---

## 🗄️ Database Design

### Entity Relationship Diagram

```
┌─────────────────┐
│      USER       │
├─────────────────┤
│ id (PK, UUID)   │
│ username        │
│ email           │
│ password        │
│ isRegistered    │
│ role            │
│ status          │
│ createdAt       │
│ updatedAt       │
└────────┬────────┘
         │
         │ 1:N
         ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│   PLATFORM_CONNECTION   │     │        BOOKMARK         │
├─────────────────────────┤     ├─────────────────────────┤
│ id (PK, UUID)           │     │ id (PK, UUID)           │
│ user_id (FK)            │     │ user_id (FK)            │
│ platform (ENUM)         │     │ platform (ENUM)         │
│ platformUsername        │     │ title                   │
│ accessToken             │     │ url                     │
│ refreshToken            │     │ description             │
│ tokenExpiresAt          │     │ contentType (ENUM)      │
│ isActive                │     │ createdAt               │
│ connectedAt             │     │ updatedAt               │
│ lastSyncAt              │     └─────────────────────────┘
└─────────────────────────┘
```

### Entity Details

#### User Entity
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, Auto-generated | Unique identifier |
| username | String | Unique, Not Null | User's display name |
| email | String | Unique, Not Null | User's email |
| password | String | Not Null | BCrypt hashed |
| isRegistered | Boolean | Default: false | True after first login |
| role | Enum | Not Null | USER, ADMIN, SUPER_ADMIN |
| status | Enum | Not Null | ACTIVE, INACTIVE, SUSPENDED |
| createdAt | LocalDateTime | Auto | Creation timestamp |
| updatedAt | LocalDateTime | Auto | Last update timestamp |

#### PlatformConnection Entity
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| user | User | FK, Not Null | Owner of connection |
| platform | Enum | Not Null | REDDIT, YOUTUBE, etc. |
| platformUsername | String | Not Null | Username on that platform |
| accessToken | String | Not Null | OAuth access token |
| refreshToken | String | Nullable | OAuth refresh token |
| tokenExpiresAt | LocalDateTime | Nullable | Token expiration time |
| isActive | Boolean | Default: true | Connection status |
| connectedAt | LocalDateTime | Auto | When connected |
| lastSyncAt | LocalDateTime | Auto | Last bookmark sync |

#### Bookmark Entity
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| user | User | FK, Not Null | Owner of bookmark |
| platform | Enum | Not Null | Source platform |
| title | String | Not Null | Bookmark title |
| url | String | Not Null | Bookmark URL |
| description | String | Nullable | Optional description |
| contentType | Enum | Not Null | VIDEO, ARTICLE, etc. |
| createdAt | LocalDateTime | Auto | Creation timestamp |

### Indexes
```sql
-- Users table
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_status ON users(status);

-- Platform connections table
CREATE INDEX idx_pc_user_id ON platform_connections(user_id);
CREATE INDEX idx_pc_user_platform ON platform_connections(user_id, platform);
CREATE INDEX idx_pc_token_expiry ON platform_connections(token_expires_at);

-- Bookmarks table
CREATE INDEX idx_bookmark_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmark_platform ON bookmarks(user_id, platform);
CREATE INDEX idx_bookmark_url ON bookmarks(url);
```

---

## 🍃 Spring Boot Concepts

### 1. Dependency Injection (DI)

**What:** Spring automatically provides required dependencies to classes.

**How:**
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class UserService {
    private final UserRepository userRepository;  // Injected automatically
    private final PasswordEncoder passwordEncoder;  // Injected automatically
}
```

**Why:** Loose coupling, easier testing, single responsibility.

---

### 2. Spring Data JPA

**What:** Abstraction over database operations.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks class as database table |
| `@Table` | Specifies table name |
| `@Id` | Marks primary key |
| `@GeneratedValue` | Auto-generates ID |
| `@Column` | Configures column properties |
| `@ManyToOne` | Many-to-one relationship |
| `@OneToMany` | One-to-many relationship |

**Query Methods:**
```java
// Spring generates SQL from method name
Optional<User> findByEmail(String email);
List<User> findByStatusAndRole(UserStatus status, UserRole role);
Page<User> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

// Custom JPQL
@Query("SELECT u FROM User u WHERE u.email LIKE %:term%")
List<User> searchByEmail(@Param("term") String term);
```

---

### 3. REST Controllers

**What:** Handle HTTP requests and responses.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@RestController` | Marks class as REST API controller |
| `@RequestMapping` | Base URL mapping |
| `@GetMapping` | Handle GET requests |
| `@PostMapping` | Handle POST requests |
| `@PutMapping` | Handle PUT requests |
| `@DeleteMapping` | Handle DELETE requests |
| `@PathVariable` | Extract URL path variables |
| `@RequestParam` | Extract query parameters |
| `@RequestBody` | Deserialize request body |

**Example:**
```java
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {
    
    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponse> getById(@PathVariable UUID id) { ... }
    
    @GetMapping
    public Page<BookmarkResponse> getAll(
        @RequestParam(required = false) Platform platform,
        Pageable pageable
    ) { ... }
    
    @PostMapping
    public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request) { ... }
}
```

---

### 4. Validation

**What:** Validates incoming request data.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@Valid` | Triggers validation |
| `@NotNull` | Field cannot be null |
| `@NotBlank` | String cannot be empty |
| `@Email` | Valid email format |
| `@Size` | String length constraints |
| `@Min/@Max` | Number range |
| `@Pattern` | Regex validation |

**Example:**
```java
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

---

### 5. Exception Handling

**What:** Centralized error handling across application.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@RestControllerAdvice` | Global exception handler |
| `@ExceptionHandler` | Handles specific exception type |

**Example:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Extract validation errors
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
```

---

### 6. Spring Security

**What:** Authentication and authorization framework.

**Key Concepts:**
| Concept | Description |
|---------|-------------|
| Authentication | Verifying user identity (login) |
| Authorization | Checking user permissions |
| SecurityFilterChain | Chain of security filters |
| UserDetailsService | Loads user from database |

**JWT Flow:**
```
1. User sends login request with credentials
2. Server validates credentials
3. Server generates JWT token
4. Client stores token (localStorage/cookie)
5. Client sends token in Authorization header
6. Server validates token on each request
```

---

### 7. Transaction Management

**What:** Ensures database operations are atomic.

**Key Annotation:** `@Transactional`

**Properties:**
| Property | Description |
|----------|-------------|
| `readOnly` | Optimization for read-only operations |
| `propagation` | How transactions relate to each other |
| `isolation` | Transaction isolation level |
| `rollbackFor` | Exceptions that trigger rollback |

**Example:**
```java
@Service
public class BookmarkService {
    
    @Transactional(readOnly = true)
    public BookmarkResponse getById(UUID id) { ... }
    
    @Transactional
    public BookmarkResponse create(CreateBookmarkRequest request) { ... }
    
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithRelated(UUID id) { ... }
}
```

---

### 8. Caching with Redis

**What:** Store frequently accessed data in memory.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@EnableCaching` | Enables caching support |
| `@Cacheable` | Caches method result |
| `@CacheEvict` | Removes cached data |
| `@CachePut` | Updates cached data |

**Example:**
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public UserResponse getById(UUID id) { ... }
    
    @CacheEvict(value = "users", key = "#id")
    public void deleteById(UUID id) { ... }
}
```

---

### 9. Scheduling

**What:** Run tasks at specified intervals.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@EnableScheduling` | Enables scheduling |
| `@Scheduled` | Marks method as scheduled task |

**Cron Expressions:**
| Expression | Meaning |
|------------|---------|
| `0 0 * * * *` | Every hour |
| `0 0 0 * * *` | Every day at midnight |
| `0 0 */6 * * *` | Every 6 hours |
| `0 */30 * * * *` | Every 30 minutes |

**Example:**
```java
@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "0 0 */6 * * *")  // Every 6 hours
    public void refreshExpiredTokens() { ... }
    
    @Scheduled(fixedRate = 3600000)  // Every hour (in ms)
    public void syncBookmarks() { ... }
}
```

---

### 10. Asynchronous Processing

**What:** Execute tasks in background threads.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@EnableAsync` | Enables async support |
| `@Async` | Marks method as asynchronous |

**Example:**
```java
@Service
public class EmailService {
    
    @Async
    public CompletableFuture<Void> sendWelcomeEmail(String email) {
        // Runs in separate thread
        // Main thread continues immediately
    }
}
```

---

### 11. Message Queues (RabbitMQ)

**What:** Asynchronous communication between services.

**Key Annotations:**
| Annotation | Purpose |
|------------|---------|
| `@RabbitListener` | Listens to queue |
| `RabbitTemplate` | Sends messages |

**Use Cases:**
- Email notifications
- Bookmark sync requests
- Token refresh events

---

### 12. Pagination

**What:** Return data in chunks instead of all at once.

**Key Classes:**
| Class | Purpose |
|-------|---------|
| `Pageable` | Pagination request (page, size, sort) |
| `Page<T>` | Paginated response with metadata |
| `PageRequest` | Create Pageable instance |

**Example:**
```java
// In Controller
@GetMapping
public Page<BookmarkResponse> getAll(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
    return bookmarkService.getAll(pageable);
}

// In Repository
Page<Bookmark> findByUserId(UUID userId, Pageable pageable);
```

---

## 🔌 API Design

### Authentication Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT |
| POST | `/api/auth/refresh` | Refresh JWT token |
| POST | `/api/auth/logout` | Invalidate token |

### User Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user |
| PUT | `/api/users/me` | Update current user |
| DELETE | `/api/users/me` | Delete account |

### Bookmark Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/bookmarks` | Get all bookmarks (paginated) |
| GET | `/api/bookmarks/{id}` | Get bookmark by ID |
| POST | `/api/bookmarks` | Create bookmark |
| PUT | `/api/bookmarks/{id}` | Update bookmark |
| DELETE | `/api/bookmarks/{id}` | Delete bookmark |
| GET | `/api/bookmarks/search` | Search bookmarks |
| GET | `/api/bookmarks/platform/{platform}` | Filter by platform |

### Platform Connection Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/platforms` | Get connected platforms |
| POST | `/api/platforms/connect` | Initiate OAuth connection |
| GET | `/api/platforms/callback` | OAuth callback handler |
| DELETE | `/api/platforms/{platform}` | Disconnect platform |
| POST | `/api/platforms/{platform}/sync` | Trigger manual sync |

### Response Formats

**Success Response:**
```json
{
    "success": true,
    "data": { ... },
    "message": "Operation successful",
    "timestamp": "2026-01-31T10:30:00Z"
}
```

**Error Response:**
```json
{
    "success": false,
    "error": {
        "code": "USER_NOT_FOUND",
        "message": "User with ID xyz not found",
        "details": null
    },
    "timestamp": "2026-01-31T10:30:00Z"
}
```

**Paginated Response:**
```json
{
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
}
```

---

## 🔐 Security Implementation

### JWT Token Structure
```
Header.Payload.Signature

Header: {
    "alg": "HS256",
    "typ": "JWT"
}

Payload: {
    "sub": "user-uuid",
    "email": "user@example.com",
    "role": "USER",
    "iat": 1706700000,
    "exp": 1706786400
}

Signature: HMACSHA256(
    base64UrlEncode(header) + "." + base64UrlEncode(payload),
    secret
)
```

### Security Filter Chain
```
Request → CorsFilter → JwtAuthFilter → UsernamePasswordAuthFilter → Controller
```

### Password Storage
- Never store plain text passwords
- Use BCrypt with cost factor 10+
- Salt is automatically handled by BCrypt

### OAuth2 Flow (for Platform Connections)
```
1. User clicks "Connect Reddit"
2. Redirect to Reddit authorization URL
3. User approves access
4. Reddit redirects to callback with auth code
5. Exchange auth code for access token
6. Store tokens in PlatformConnection
7. Use access token to fetch bookmarks
```

---

## ⏰ Background Processing

### Scheduled Jobs

| Job | Schedule | Purpose |
|-----|----------|---------|
| Token Refresh | Every 6 hours | Refresh expiring OAuth tokens |
| Bookmark Sync | Every 24 hours | Sync bookmarks from all platforms |
| Session Cleanup | Daily at midnight | Remove expired sessions |
| Email Reminder | Every 48 hours | Remind about expired connections |

### Event-Driven Architecture

**Events:**
- `UserRegisteredEvent` → Send welcome email
- `PlatformConnectedEvent` → Trigger initial bookmark sync
- `TokenExpiredEvent` → Notify user to reconnect
- `BookmarkCreatedEvent` → Update search index

**RabbitMQ Queues:**
- `notifications.email` → Email notifications
- `sync.bookmarks` → Bookmark sync requests
- `tokens.refresh` → Token refresh requests

---

## ❓ Interview Questions

### Spring Boot Fundamentals

1. **What is Dependency Injection and why is it important?**
   - DI is a design pattern where objects receive their dependencies from external sources rather than creating them
   - Benefits: Loose coupling, testability, maintainability
   - Spring implements DI through IoC container

2. **Explain the difference between @Component, @Service, @Repository, and @Controller**
   - `@Component`: Generic stereotype for any Spring-managed component
   - `@Service`: Business logic layer, indicates service operations
   - `@Repository`: Data access layer, enables exception translation
   - `@Controller`: Presentation layer, handles HTTP requests

3. **What is the Spring Bean lifecycle?**
   - Instantiation → Populate Properties → BeanNameAware → BeanFactoryAware → Pre-initialization (BeanPostProcessor) → InitializingBean → Custom init-method → Post-initialization → Ready → DisposableBean → Custom destroy-method

4. **What are the different bean scopes in Spring?**
   - `singleton` (default): One instance per Spring container
   - `prototype`: New instance every time requested
   - `request`: One instance per HTTP request
   - `session`: One instance per HTTP session
   - `application`: One instance per ServletContext

5. **What is @Transactional and how does it work?**
   - Marks method/class for transaction management
   - Uses AOP proxies to wrap methods
   - Handles commit/rollback automatically
   - Properties: propagation, isolation, timeout, readOnly, rollbackFor

### Spring Data JPA

6. **How does Spring Data JPA generate queries from method names?**
   - Parses method name using naming conventions
   - `findByEmailAndStatus` → `WHERE email = ? AND status = ?`
   - Supports keywords: And, Or, Between, LessThan, GreaterThan, Like, OrderBy, etc.

7. **When would you use @Query instead of derived queries?**
   - Complex queries with multiple joins
   - Native SQL queries needed
   - Performance optimization
   - When method name becomes too long/unreadable

8. **Explain the difference between JPQL and native SQL in Spring Data JPA**
   - JPQL: Object-oriented query language, uses entity names
   - Native SQL: Raw SQL, uses table names
   - JPQL is database-agnostic, native SQL is database-specific

9. **What is the N+1 query problem and how do you solve it?**
   - Problem: Fetching parent loads children one-by-one
   - Solutions:
     - `@EntityGraph` for eager fetching
     - `JOIN FETCH` in JPQL
     - `@BatchSize` for batch loading

10. **What is optimistic vs pessimistic locking?**
    - Optimistic: `@Version` field, fails on conflict
    - Pessimistic: Database lock, blocks other transactions
    - Use optimistic for high-read, low-conflict scenarios

### REST API Design

11. **What HTTP status codes should be used and when?**
    - 200 OK: Successful GET/PUT
    - 201 Created: Successful POST
    - 204 No Content: Successful DELETE
    - 400 Bad Request: Invalid input
    - 401 Unauthorized: Authentication required
    - 403 Forbidden: Insufficient permissions
    - 404 Not Found: Resource doesn't exist
    - 500 Internal Server Error: Server-side error

12. **How do you handle exceptions globally in Spring Boot?**
    - `@RestControllerAdvice` + `@ExceptionHandler`
    - Maps exceptions to HTTP responses
    - Provides consistent error format

13. **What is the difference between @RequestParam and @PathVariable?**
    - `@PathVariable`: `/users/{id}` → extracts `id` from path
    - `@RequestParam`: `/users?status=active` → extracts query params

14. **How do you implement pagination in Spring Boot?**
    - Accept `Pageable` parameter in controller
    - Return `Page<T>` from repository
    - Use `PageRequest.of(page, size, sort)`

### Security

15. **How does JWT authentication work?**
    - User sends credentials
    - Server validates and returns signed JWT
    - Client includes JWT in Authorization header
    - Server validates JWT signature and claims

16. **What is the difference between authentication and authorization?**
    - Authentication: Verifying identity (who are you?)
    - Authorization: Checking permissions (what can you do?)

17. **How do you secure REST APIs in Spring Boot?**
    - JWT tokens for stateless authentication
    - HTTPS for transport security
    - Input validation
    - Rate limiting
    - CORS configuration
    - Method-level security with `@PreAuthorize`

18. **What is CORS and how do you configure it?**
    - Cross-Origin Resource Sharing
    - Browser security feature
    - Configure allowed origins, methods, headers
    - Use `@CrossOrigin` or `CorsConfigurationSource` bean

### Performance & Scalability

19. **How do you implement caching in Spring Boot?**
    - `@EnableCaching` on main class
    - `@Cacheable`, `@CacheEvict`, `@CachePut` on methods
    - Configure cache manager (Redis, Caffeine, etc.)

20. **What is connection pooling and why is it important?**
    - Reuses database connections
    - Reduces connection overhead
    - HikariCP is default in Spring Boot
    - Configure pool size, timeout, etc.

21. **How do you handle async operations in Spring Boot?**
    - `@EnableAsync` on configuration
    - `@Async` on methods
    - Return `CompletableFuture` for results
    - Configure thread pool executor

22. **What strategies do you use for database optimization?**
    - Proper indexing
    - Query optimization
    - Connection pooling
    - Caching frequently accessed data
    - Pagination for large datasets
    - Lazy loading for relationships

### System Design

23. **How would you design a bookmark aggregation system?**
    - Core entities: User, Bookmark, PlatformConnection
    - OAuth2 for platform authentication
    - Background jobs for sync
    - Message queue for async processing
    - Caching for performance
    - Full-text search for bookmarks

24. **How do you handle token refresh for OAuth connections?**
    - Store refresh token in database
    - Scheduled job checks for expiring tokens
    - Use refresh token to get new access token
    - Update database with new tokens
    - Notify user if refresh fails

25. **What happens if a platform API is down during sync?**
    - Implement retry with exponential backoff
    - Use circuit breaker pattern
    - Queue failed syncs for later
    - Notify user of sync failures
    - Don't delete existing bookmarks

---

## 📚 Learning Resources

### Official Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

### Tutorials
- [Baeldung](https://www.baeldung.com/) - Comprehensive Spring tutorials
- [Spring Guides](https://spring.io/guides) - Official step-by-step guides

### Books
- "Spring in Action" by Craig Walls
- "Spring Boot in Practice" by Somnath Musib

---

## ✅ Project Checklist

### Phase 1: Foundation
- [ ] Project setup with Maven
- [ ] Database configuration (PostgreSQL)
- [ ] Entity models (User, Bookmark, PlatformConnection)
- [ ] Repository interfaces with custom queries
- [ ] Basic CRUD operations

### Phase 2: Authentication
- [ ] User registration
- [ ] Login with JWT
- [ ] Password hashing
- [ ] Token validation filter
- [ ] Protected endpoints

### Phase 3: Core Features
- [ ] Bookmark CRUD operations
- [ ] Platform connection management
- [ ] Search functionality
- [ ] Pagination implementation

### Phase 4: Platform Integration
- [ ] Reddit OAuth integration
- [ ] YouTube API integration
- [ ] Browser extension support
- [ ] Bookmark sync logic

### Phase 5: Background Processing
- [ ] Scheduled token refresh
- [ ] Automated bookmark sync
- [ ] Email notifications
- [ ] RabbitMQ integration

### Phase 6: Production Ready
- [ ] Caching with Redis
- [ ] Error handling
- [ ] Logging
- [ ] API documentation (Swagger)
- [ ] Unit tests
- [ ] Integration tests

---

*Last Updated: January 2026*
