# Imagen del frontend. Multi-etapa: la imagen final es nginx con los estáticos, sin
# Node ni node_modules.

# --- Etapa de construcción ---
FROM node:22-alpine AS build
WORKDIR /build

# npm ci antes de copiar el código, por la misma razón que en el backend: un cambio
# en un .tsx no invalida la capa de instalación.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Etapa de ejecución ---
FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
