# ========================================
# Stage 1 : Build Vue.js
# ========================================
FROM node:20-alpine AS ui-builder

WORKDIR /app

# Installer les dépendances en premier (cache Docker)
COPY package*.json ./
RUN npm install

# Copier le reste du code
COPY . .

# Build en mode production
RUN npm run build

# ========================================
# Stage 2 : Serveur Nginx
# ========================================
FROM nginx:alpine

# Supprimer conf défaut
RUN rm -rf /etc/nginx/conf.d/default.conf

# Copier les fichiers statiques générés
COPY --from=ui-builder /app/dist /usr/share/nginx/html

# Copier notre configuration proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
