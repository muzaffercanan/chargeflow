FROM node:20-alpine AS build

WORKDIR /app

COPY panel/package.json panel/package-lock.json ./
RUN npm ci

COPY panel/ ./
RUN npm run build

FROM nginx:1.27-alpine AS runtime

COPY panel/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=12 \
  CMD wget --quiet --output-document=/dev/null http://127.0.0.1:8080/health || exit 1
