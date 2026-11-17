# ============================================================
# Stage 1: Build React Production Bundle with Vite
# ============================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies first for Docker layer caching
COPY package*.json ./
RUN npm ci

# Copy source code and assets
COPY . .

# Run production build
RUN npm run build

# ============================================================
# Stage 2: Production Nginx Server
# ============================================================
FROM nginx:alpine

LABEL maintainer="Cognivault AI Team"
LABEL description="Production Nginx container for Cognivault AI React Frontend"

# Remove default nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy built artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
