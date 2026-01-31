# Convergence Docker Guide - Production-Ready Setup

## 📋 Table of Contents
1. [Prerequisites - What to Study First](#prerequisites---what-to-study-first)
2. [Docker Fundamentals](#docker-fundamentals)
3. [Project-Specific Docker Setup](#project-specific-docker-setup)
4. [Development vs Production](#development-vs-production)
5. [Web Version Docker Setup](#web-version-docker-setup)
6. [Extension Version Docker Setup](#extension-version-docker-setup)
7. [Docker Compose Orchestration](#docker-compose-orchestration)
8. [Best Practices Checklist](#best-practices-checklist)
9. [Common Mistakes to Avoid](#common-mistakes-to-avoid)
10. [Implementation Roadmap](#implementation-roadmap)

---

## 📚 Prerequisites - What to Study First

### Before You Touch Docker, Understand:

```
┌─────────────────────────────────────────────────────────────┐
│                 STUDY ORDER (1-2 weeks)                      │
│                                                              │
│  Week 1: Fundamentals                                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 1. What is containerization? (vs VMs)               │    │
│  │ 2. Docker architecture (daemon, client, registry)   │    │
│  │ 3. Images vs Containers vs Volumes                  │    │
│  │ 4. Dockerfile syntax & instructions                 │    │
│  │ 5. Docker CLI commands                              │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  Week 2: Production Concepts                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 6. Multi-stage builds                               │    │
│  │ 7. Docker Compose                                   │    │
│  │ 8. Networking (bridge, host, overlay)               │    │
│  │ 9. Volume management & persistence                  │    │
│  │ 10. Security best practices                         │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Study Resources

| Topic | Resource | Time |
|-------|----------|------|
| Docker Basics | [Docker Official Getting Started](https://docs.docker.com/get-started/) | 2-3 hours |
| Dockerfile | [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/) | 1-2 hours |
| Multi-stage Builds | [Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/) | 1 hour |
| Docker Compose | [Compose Specification](https://docs.docker.com/compose/compose-file/) | 2 hours |
| Security | [Docker Security](https://docs.docker.com/engine/security/) | 1-2 hours |

---

## 🐳 Docker Fundamentals

### Core Concepts You MUST Understand

#### 1. Images vs Containers

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  IMAGE (Blueprint)              CONTAINER (Running Instance) │
│  ┌─────────────────┐           ┌─────────────────┐          │
│  │ Read-only       │           │ Read-write      │          │
│  │ Layered         │  ──────▶  │ Ephemeral       │          │
│  │ Shareable       │  docker   │ Isolated        │          │
│  │ Versioned       │   run     │ Has process     │          │
│  └─────────────────┘           └─────────────────┘          │
│                                                              │
│  Like a "Class"                 Like an "Object/Instance"   │
└─────────────────────────────────────────────────────────────┘
```

#### 2. Dockerfile Instructions

```dockerfile
# ============ MOST IMPORTANT INSTRUCTIONS ============

# Base image - ALWAYS start here
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy files (order matters for caching!)
COPY pom.xml .
COPY src ./src

# Run commands during build
RUN mvn clean package -DskipTests

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m"

# Expose port (documentation only)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run command when container starts
CMD ["java", "-jar", "app.jar"]

# Alternative: ENTRYPOINT (harder to override)
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 3. Layer Caching (CRITICAL for build speed)

```dockerfile
# ❌ BAD - Copies everything, cache invalidated on ANY change
FROM maven:3.9-eclipse-temurin-21
COPY . .
RUN mvn package

# ✅ GOOD - Dependencies cached separately from code
FROM maven:3.9-eclipse-temurin-21
COPY pom.xml .
RUN mvn dependency:go-offline    # Cached until pom.xml changes
COPY src ./src
RUN mvn package -DskipTests      # Only rebuilds when src changes
```

**Rule:** Copy things that change LESS frequently FIRST.

```
Order of Dockerfile instructions:
1. Base image (rarely changes)
2. System dependencies (rarely changes)
3. Application dependencies (changes sometimes)
4. Application code (changes frequently)
```

#### 4. Multi-Stage Builds (MUST USE)

```dockerfile
# ============ WHY MULTI-STAGE? ============
#
# Single stage: 800MB+ image (includes Maven, source code, etc.)
# Multi-stage:  200MB image (only JRE + JAR)

# Stage 1: BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: RUN (production image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

---

## 🎯 Project-Specific Docker Setup

### Convergence Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CONVERGENCE STACK                         │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    NGINX (Reverse Proxy)             │    │
│  │                    Port 80/443                       │    │
│  └─────────────────────┬───────────────────────────────┘    │
│                        │                                     │
│         ┌──────────────┼──────────────┐                     │
│         │              │              │                     │
│         ▼              ▼              ▼                     │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐              │
│  │ Frontend  │  │  Backend  │  │  Backend  │              │
│  │  (React)  │  │  (Spring) │  │  (Spring) │              │
│  │  Port 80  │  │  :8080    │  │  :8081    │  (Replicas)  │
│  └───────────┘  └─────┬─────┘  └─────┬─────┘              │
│                       │              │                     │
│         ┌─────────────┴──────────────┘                     │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    DATA LAYER                        │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │   │
│  │  │PostgreSQL│  │  Redis   │  │ RabbitMQ │          │   │
│  │  │  :5432   │  │  :6379   │  │  :5672   │          │   │
│  │  └──────────┘  └──────────┘  └──────────┘          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
convergence/
├── docker/
│   ├── backend/
│   │   ├── Dockerfile              # Production Dockerfile
│   │   └── Dockerfile.dev          # Development Dockerfile
│   ├── frontend/
│   │   ├── Dockerfile
│   │   └── Dockerfile.dev
│   ├── nginx/
│   │   ├── Dockerfile
│   │   └── nginx.conf
│   └── scripts/
│       ├── init-db.sh              # Database initialization
│       └── wait-for-it.sh          # Service readiness check
│
├── docker-compose.yml              # Production compose
├── docker-compose.dev.yml          # Development compose
├── docker-compose.override.yml     # Local overrides (gitignored)
│
├── .dockerignore                   # Files to exclude from build
├── .env.example                    # Environment template
└── .env                            # Actual env vars (gitignored)
```

---

## 🔧 Development vs Production

### Key Differences

| Aspect | Development | Production |
|--------|-------------|------------|
| **Hot Reload** | ✅ Yes (volume mounts) | ❌ No |
| **Debug Ports** | ✅ Exposed | ❌ Closed |
| **Source Maps** | ✅ Included | ❌ Excluded |
| **Image Size** | Larger OK | Minimal |
| **Build Time** | Fast iteration | Optimized layers |
| **Secrets** | .env files | Secret managers |
| **Logging** | Verbose | Structured JSON |
| **Health Checks** | Optional | Required |

### Development Dockerfile

```dockerfile
# docker/backend/Dockerfile.dev
FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Install development tools
RUN apt-get update && apt-get install -y \
    curl \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Copy only pom.xml first (dependency caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Source code mounted as volume (not copied)
# See docker-compose.dev.yml

# Debug port
EXPOSE 8080 5005

# Development command with hot reload
CMD ["mvn", "spring-boot:run", \
     "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]
```

### Production Dockerfile

```dockerfile
# docker/backend/Dockerfile
# ============================================================
# PRODUCTION DOCKERFILE - CONVERGENCE BACKEND
# ============================================================
# Best Practices Applied:
# 1. Multi-stage build (smaller image)
# 2. Non-root user (security)
# 3. Layer caching optimization
# 4. Health check included
# 5. Minimal base image (Alpine)
# ============================================================

# -------------------- STAGE 1: BUILD --------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy dependency descriptor FIRST (better caching)
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests -B && \
    # Extract layered JAR for better caching
    java -Djarmode=layertools -jar target/*.jar extract

# -------------------- STAGE 2: PRODUCTION --------------------
FROM eclipse-temurin:21-jre-alpine

# Labels for image metadata
LABEL maintainer="subham@example.com"
LABEL version="1.0.0"
LABEL description="Convergence Backend API"

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy layered JAR (better caching on updates)
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Expose port (documentation)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

---

## 🌐 Web Version Docker Setup

### Frontend Dockerfile (React)

```dockerfile
# docker/frontend/Dockerfile
# ============================================================
# PRODUCTION DOCKERFILE - CONVERGENCE FRONTEND
# ============================================================

# -------------------- STAGE 1: BUILD --------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first (dependency caching)
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci --only=production=false

# Copy source code
COPY . .

# Build arguments for environment
ARG VITE_API_URL
ARG VITE_APP_VERSION

# Set environment variables for build
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_APP_VERSION=$VITE_APP_VERSION

# Build the application
RUN npm run build

# -------------------- STAGE 2: PRODUCTION --------------------
FROM nginx:alpine

# Copy custom nginx config
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Add non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup && \
    chown -R appuser:appgroup /usr/share/nginx/html && \
    chown -R appuser:appgroup /var/cache/nginx && \
    chown -R appuser:appgroup /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R appuser:appgroup /var/run/nginx.pid

USER appuser

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### Nginx Configuration

```nginx
# docker/nginx/nginx.conf
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Logging format
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    # Performance settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript 
               application/rss+xml application/atom+xml image/svg+xml;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    server {
        listen 80;
        server_name localhost;
        root /usr/share/nginx/html;
        index index.html;

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }

        # API proxy to backend
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }

        # SPA routing - serve index.html for all routes
        location / {
            try_files $uri $uri/ /index.html;
        }

        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

### Docker Compose - Web Version

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ==================== DATABASE ====================
  postgres:
    image: postgres:16-alpine
    container_name: convergence-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-convergence}
      POSTGRES_USER: ${POSTGRES_USER:-convergence}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Database password required}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/scripts/init-db.sh:/docker-entrypoint-initdb.d/init-db.sh:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-convergence} -d ${POSTGRES_DB:-convergence}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend-network

  # ==================== CACHE ====================
  redis:
    image: redis:7-alpine
    container_name: convergence-cache
    restart: unless-stopped
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend-network

  # ==================== MESSAGE QUEUE ====================
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: convergence-queue
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-convergence}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?RabbitMQ password required}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - backend-network

  # ==================== BACKEND ====================
  backend:
    build:
      context: .
      dockerfile: docker/backend/Dockerfile
    container_name: convergence-api
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-convergence}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-convergence}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-convergence}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?JWT secret required}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 3
    networks:
      - backend-network
      - frontend-network

  # ==================== FRONTEND ====================
  frontend:
    build:
      context: ./frontend
      dockerfile: ../docker/frontend/Dockerfile
      args:
        VITE_API_URL: /api
        VITE_APP_VERSION: ${APP_VERSION:-1.0.0}
    container_name: convergence-web
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    networks:
      - frontend-network
    ports:
      - "80:80"
      - "443:443"

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  rabbitmq_data:
    driver: local

networks:
  backend-network:
    driver: bridge
  frontend-network:
    driver: bridge
```

### Development Docker Compose

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: convergence-db-dev
    environment:
      POSTGRES_DB: convergence_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    ports:
      - "5432:5432"
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
    networks:
      - dev-network

  redis:
    image: redis:7-alpine
    container_name: convergence-cache-dev
    ports:
      - "6379:6379"
    networks:
      - dev-network

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: convergence-queue-dev
    environment:
      RABBITMQ_DEFAULT_USER: dev
      RABBITMQ_DEFAULT_PASS: dev123
    ports:
      - "5672:5672"
      - "15672:15672"  # Management UI
    networks:
      - dev-network

  backend:
    build:
      context: .
      dockerfile: docker/backend/Dockerfile.dev
    container_name: convergence-api-dev
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/convergence_dev
      SPRING_DATASOURCE_USERNAME: dev
      SPRING_DATASOURCE_PASSWORD: dev123
      SPRING_DATA_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: dev
      SPRING_RABBITMQ_PASSWORD: dev123
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    volumes:
      # Mount source code for hot reload
      - ./src:/app/src:ro
      - ./pom.xml:/app/pom.xml:ro
      # Cache Maven dependencies
      - maven_cache:/root/.m2
    depends_on:
      - postgres
      - redis
      - rabbitmq
    networks:
      - dev-network

  frontend:
    build:
      context: ./frontend
      dockerfile: ../docker/frontend/Dockerfile.dev
    container_name: convergence-web-dev
    ports:
      - "5173:5173"  # Vite dev server
    volumes:
      - ./frontend/src:/app/src:ro
      - ./frontend/public:/app/public:ro
    environment:
      VITE_API_URL: http://localhost:8080/api
    depends_on:
      - backend
    networks:
      - dev-network

volumes:
  postgres_dev_data:
  maven_cache:

networks:
  dev-network:
    driver: bridge
```

---

## 🔌 Extension Version Docker Setup

### Why Extension Needs Different Setup

```
┌─────────────────────────────────────────────────────────────┐
│                 EXTENSION vs WEB VERSION                     │
│                                                              │
│  WEB VERSION:                                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Frontend + Backend + Database + Redis + RabbitMQ    │    │
│  │ All in Docker Compose                               │    │
│  │ Nginx serves frontend + proxies API                 │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  EXTENSION VERSION:                                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Backend + Database + Redis + RabbitMQ               │    │
│  │ In Docker Compose                                   │    │
│  │                                                     │    │
│  │ Extension runs in BROWSER (not Docker!)             │    │
│  │ Extension calls Backend API directly                │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Extension Backend Docker Compose

```yaml
# docker-compose.extension.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: convergence-ext-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-convergence}
      POSTGRES_USER: ${POSTGRES_USER:-convergence}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Database password required}
    volumes:
      - postgres_ext_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-convergence}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ext-network

  redis:
    image: redis:7-alpine
    container_name: convergence-ext-cache
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis_ext_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ext-network

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: convergence-ext-queue
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-convergence}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?RabbitMQ password required}
    volumes:
      - rabbitmq_ext_data:/var/lib/rabbitmq
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - ext-network

  backend:
    build:
      context: .
      dockerfile: docker/backend/Dockerfile
    container_name: convergence-ext-api
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: extension
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-convergence}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-convergence}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-convergence}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?JWT secret required}
      # CORS for browser extension
      CORS_ALLOWED_ORIGINS: chrome-extension://*,moz-extension://*,https://your-domain.com
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 3
    networks:
      - ext-network

volumes:
  postgres_ext_data:
  redis_ext_data:
  rabbitmq_ext_data:

networks:
  ext-network:
    driver: bridge
```

### Extension-Specific CORS Configuration

```yaml
# application-extension.yml (Spring profile for extension)
spring:
  profiles:
    active: extension

# CORS configuration for browser extension
cors:
  allowed-origins:
    - chrome-extension://*
    - moz-extension://*
    - https://your-domain.com
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
  allowed-headers:
    - Authorization
    - Content-Type
    - X-Requested-With
  allow-credentials: true
  max-age: 3600
```

### Extension Build Pipeline

```yaml
# docker-compose.extension-build.yml
# For building the extension (not running it)
version: '3.8'

services:
  extension-builder:
    image: node:20-alpine
    container_name: convergence-ext-builder
    working_dir: /app
    volumes:
      - ./extension:/app
      - extension_node_modules:/app/node_modules
      - ./extension-dist:/app/dist
    command: |
      sh -c "
        npm ci &&
        npm run build:chrome &&
        npm run build:firefox &&
        echo 'Extension built successfully!'
      "
    environment:
      VITE_API_URL: ${API_URL:-http://localhost:8080/api}

volumes:
  extension_node_modules:
```

---

## 📦 Docker Compose Orchestration

### Environment Variables

```bash
# .env.example
# ==================== DATABASE ====================
POSTGRES_DB=convergence
POSTGRES_USER=convergence
POSTGRES_PASSWORD=your_secure_password_here

# ==================== CACHE ====================
REDIS_PASSWORD=your_redis_password

# ==================== MESSAGE QUEUE ====================
RABBITMQ_USER=convergence
RABBITMQ_PASSWORD=your_rabbitmq_password

# ==================== APPLICATION ====================
JWT_SECRET=your_256_bit_secret_key_here_at_least_32_chars
JWT_EXPIRATION=86400000

# ==================== MONITORING ====================
GRAFANA_PASSWORD=admin123

# ==================== BUILD ====================
APP_VERSION=1.0.0
```

### Makefile for Common Commands

```makefile
# Makefile
.PHONY: help dev prod build test clean logs

# Default target
help:
	@echo "Available commands:"
	@echo "  make dev        - Start development environment"
	@echo "  make prod       - Start production environment"
	@echo "  make build      - Build all Docker images"
	@echo "  make test       - Run tests in Docker"
	@echo "  make clean      - Remove all containers and volumes"
	@echo "  make logs       - View logs from all services"
	@echo "  make db-shell   - Open PostgreSQL shell"
	@echo "  make redis-cli  - Open Redis CLI"

# Development
dev:
	docker-compose -f docker-compose.dev.yml up --build

dev-d:
	docker-compose -f docker-compose.dev.yml up --build -d

dev-down:
	docker-compose -f docker-compose.dev.yml down

# Production
prod:
	docker-compose -f docker-compose.yml up --build -d

prod-down:
	docker-compose -f docker-compose.yml down

# Extension backend
ext:
	docker-compose -f docker-compose.extension.yml up --build -d

ext-down:
	docker-compose -f docker-compose.extension.yml down

# Build
build:
	docker-compose -f docker-compose.yml build --no-cache

build-backend:
	docker build -t convergence-api:latest -f docker/backend/Dockerfile .

build-frontend:
	docker build -t convergence-web:latest -f docker/frontend/Dockerfile ./frontend

# Testing
test:
	docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit

# Logs
logs:
	docker-compose logs -f

logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

# Shell access
db-shell:
	docker-compose exec postgres psql -U convergence -d convergence

redis-cli:
	docker-compose exec redis redis-cli

backend-shell:
	docker-compose exec backend sh

# Cleanup
clean:
	docker-compose -f docker-compose.yml down -v --remove-orphans
	docker-compose -f docker-compose.dev.yml down -v --remove-orphans
	docker system prune -f

clean-all:
	docker-compose -f docker-compose.yml down -v --remove-orphans
	docker-compose -f docker-compose.dev.yml down -v --remove-orphans
	docker system prune -af --volumes
```

---

## ✅ Best Practices Checklist

### Dockerfile Best Practices

```
□ Use specific version tags (not :latest)
   ✅ FROM eclipse-temurin:21-jre-alpine
   ❌ FROM eclipse-temurin:latest

□ Use multi-stage builds
   ✅ Build stage + Production stage
   ❌ Single stage with build tools in production

□ Order instructions for cache efficiency
   ✅ COPY pom.xml first, then mvn install, then COPY src
   ❌ COPY . . (invalidates cache on any change)

□ Use .dockerignore
   ✅ Exclude .git, node_modules, target, logs
   ❌ Copy everything including unnecessary files

□ Run as non-root user
   ✅ USER appuser
   ❌ Running as root

□ Use HEALTHCHECK
   ✅ HEALTHCHECK CMD wget --spider http://localhost:8080/health
   ❌ No health check

□ Set resource limits
   ✅ deploy.resources.limits.memory: 512M
   ❌ Unlimited resources

□ Don't store secrets in image
   ✅ Use environment variables or secret managers
   ❌ COPY credentials.json /app/
```

### Docker Compose Best Practices

```
□ Use depends_on with condition
   ✅ depends_on: postgres: condition: service_healthy
   ❌ depends_on: - postgres (doesn't wait for ready)

□ Define networks explicitly
   ✅ networks: backend-network, frontend-network
   ❌ Using default network for everything

□ Use named volumes
   ✅ volumes: postgres_data:
   ❌ Anonymous volumes

□ Set restart policies
   ✅ restart: unless-stopped
   ❌ No restart policy (containers don't restart)

□ Use env_file for secrets
   ✅ env_file: .env
   ❌ Hardcoding passwords in compose file

□ Health checks on all services
   ✅ healthcheck for postgres, redis, backend
   ❌ No health checks
```

### .dockerignore

```gitignore
# .dockerignore

# Git
.git
.gitignore

# IDE
.idea
.vscode
*.iml

# Build outputs
target/
dist/
build/

# Dependencies (will be installed in container)
node_modules/

# Logs
*.log
logs/

# Environment files (use Docker secrets instead)
.env
.env.local
.env.*.local

# Test files
*.test.js
*.spec.js
__tests__/
coverage/

# Documentation
*.md
docs/

# Docker files (meta)
Dockerfile*
docker-compose*
.docker/

# OS files
.DS_Store
Thumbs.db
```

---

## ❌ Common Mistakes to Avoid

### 1. Using :latest Tag

```dockerfile
# ❌ BAD - :latest can change unexpectedly
FROM node:latest

# ✅ GOOD - Specific version for reproducibility
FROM node:20.11-alpine
```

### 2. Not Using .dockerignore

```
# Without .dockerignore:
COPY . .  →  Copies node_modules (500MB+), .git (huge), etc.

# With .dockerignore:
COPY . .  →  Copies only needed files (few MB)
```

### 3. Installing Dev Dependencies in Production

```dockerfile
# ❌ BAD - Includes devDependencies
RUN npm install

# ✅ GOOD - Production only
RUN npm ci --only=production
```

### 4. Running as Root

```dockerfile
# ❌ BAD - Running as root (security risk)
FROM node:20-alpine
COPY . .
CMD ["node", "app.js"]

# ✅ GOOD - Non-root user
FROM node:20-alpine
RUN adduser -D appuser
USER appuser
COPY --chown=appuser . .
CMD ["node", "app.js"]
```

### 5. No Health Checks

```yaml
# ❌ BAD - No way to know if service is actually ready
backend:
  image: myapp
  depends_on:
    - postgres

# ✅ GOOD - Waits for postgres to be actually ready
backend:
  image: myapp
  depends_on:
    postgres:
      condition: service_healthy
```

### 6. Hardcoding Secrets

```dockerfile
# ❌ TERRIBLE - Secrets in Dockerfile (visible in image history!)
ENV DATABASE_PASSWORD=mysecretpassword

# ✅ GOOD - Use environment variables at runtime
ENV DATABASE_PASSWORD=${DB_PASSWORD}
```

### 7. Not Cleaning Up in Same Layer

```dockerfile
# ❌ BAD - Cache files remain in image
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get clean

# ✅ GOOD - Clean up in same layer
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
```

---

## 📅 Implementation Roadmap

### Phase 1: Learn Fundamentals (Week 1)

```
Day 1-2: Docker Basics
□ Complete Docker Getting Started tutorial
□ Understand images, containers, volumes
□ Practice basic commands (run, build, ps, logs)

Day 3-4: Dockerfile
□ Write your first Dockerfile
□ Learn multi-stage builds
□ Understand layer caching

Day 5-7: Docker Compose
□ Learn compose file syntax
□ Set up multi-container app
□ Understand networking and volumes
```

### Phase 2: Development Setup (Week 2)

```
Day 1-2: Create Development Dockerfiles
□ Backend Dockerfile.dev
□ Frontend Dockerfile.dev
□ Test hot reload works

Day 3-4: Docker Compose Dev
□ Create docker-compose.dev.yml
□ Set up volume mounts
□ Configure debug ports

Day 5-7: Test & Iterate
□ Verify all services communicate
□ Test database persistence
□ Document any issues
```

### Phase 3: Production Setup (Week 3)

```
Day 1-2: Production Dockerfiles
□ Optimize backend Dockerfile
□ Optimize frontend Dockerfile
□ Minimize image sizes

Day 3-4: Docker Compose Production
□ Create docker-compose.yml
□ Configure health checks
□ Set up proper networking

Day 5-7: Security & Performance
□ Add non-root users
□ Configure resource limits
□ Test failure scenarios
```

### Phase 4: Extension Setup (Week 4)

```
Day 1-2: Extension Backend
□ Create docker-compose.extension.yml
□ Configure CORS for extension
□ Test with extension locally

Day 3-4: CI/CD Pipeline
□ Create GitHub Actions workflow
□ Automate image building
□ Push to container registry

Day 5-7: Documentation
□ Write deployment guide
□ Create troubleshooting guide
□ Document all commands
```

---

## 📚 Study Resources

### Official Documentation
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Specification](https://docs.docker.com/compose/compose-file/)
- [Dockerfile Reference](https://docs.docker.com/engine/reference/builder/)

### Best Practices
- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Node.js Docker Guide](https://nodejs.org/en/docs/guides/nodejs-docker-webapp/)

### Security
- [Docker Security](https://docs.docker.com/engine/security/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)

### Tools
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Dive - Image Analysis](https://github.com/wagoodman/dive)
- [Hadolint - Dockerfile Linter](https://github.com/hadolint/hadolint)

---

*Last Updated: February 2026*
