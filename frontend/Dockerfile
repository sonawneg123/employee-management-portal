# ──────────────────────────────────────────────────────────────────────────────
#  Frontend Dockerfile — Employee Management Portal
#  Multi-stage build: Node.js build → Nginx static serving
#  Runtime: nginx:1.27-alpine (nginx user, non-root)
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Dependency cache ─────────────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
# ci performs a clean install matched exactly to package-lock.json
RUN npm ci

# ── Stage 2: Build ────────────────────────────────────────────────────────────
FROM deps AS builder
WORKDIR /app
# Reuse the already-installed node_modules from the deps stage
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove the default Nginx configuration
RUN rm /etc/nginx/conf.d/default.conf

# Copy production Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx master process binds to port 80.
# The nginx image already runs the master process as root but workers as
# the 'nginx' user — this is the standard hardened Nginx setup.
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
