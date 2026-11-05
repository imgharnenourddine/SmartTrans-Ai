
# ========================================
# Stage 1 - Build React avec Node.js
# ========================================
FROM node:24-alpine AS builder

WORKDIR /app

# Copier les manifestes npm
COPY package.json package-lock.json ./

# Installer les dependances exactes du lockfile
RUN npm ci

# Copier le code source React
COPY . .

# Compiler TypeScript et generer dist/
RUN npm run build

# ========================================
# Stage 2 - Servir React avec Nginx
# ========================================
FROM nginx:1.28-alpine AS runtime

# Supprimer la configuration du site par defaut
RUN rm -f /etc/nginx/conf.d/default.conf

# Template Nginx configurable au demarrage
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Copier uniquement les fichiers generes par Vite
COPY --from=builder /app/dist/ /usr/share/nginx/html/

# Configuration par defaut de notre API locale
ENV API_UPSTREAM=api:8080

# Ne substituer que API_UPSTREAM dans le template
ENV NGINX_ENVSUBST_FILTER=API_UPSTREAM

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

