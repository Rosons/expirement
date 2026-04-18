# 在仓库根目录构建：docker compose build frontend
# 构建期注入 Vite 变量（与 src/api/env.ts 一致）

FROM node:20-alpine AS builder
WORKDIR /app

COPY portal-frontend/package.json portal-frontend/package-lock.json ./
RUN npm ci

COPY portal-frontend/ ./

ARG VITE_API_BASE=
ARG VITE_CHAT_API_VERSION=v2
ARG VITE_CHAT_HISTORY_PAGE=1
ARG VITE_CHAT_HISTORY_PAGE_SIZE=10

ENV VITE_API_BASE=$VITE_API_BASE \
    VITE_CHAT_API_VERSION=$VITE_CHAT_API_VERSION \
    VITE_CHAT_HISTORY_PAGE=$VITE_CHAT_HISTORY_PAGE \
    VITE_CHAT_HISTORY_PAGE_SIZE=$VITE_CHAT_HISTORY_PAGE_SIZE

RUN npm run build

FROM nginx:1.27-alpine
COPY portal-frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
