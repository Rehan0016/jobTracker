# ===================================================================
# Multi-stage Dockerfile for JobTracker React Frontend
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build stage
# -------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package descriptors and install dependencies
COPY package*.json ./
RUN npm install

# Copy application source and build production static bundle
COPY . .
RUN npm run build

# -------------------------------------------------------------------
# Stage 2: Production Nginx runtime
# -------------------------------------------------------------------
FROM nginx:alpine AS runtime

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration with SPA fallback and API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
