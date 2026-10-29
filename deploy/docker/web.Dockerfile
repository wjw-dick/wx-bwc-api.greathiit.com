# ============================================================================
# 校园保卫 Web 镜像：Nginx + 管理后台 /admin + H5 门户 /h5
# 构建上下文：仓库根目录
#   docker build -f deploy/docker/web.Dockerfile -t bwc-web:1.0.0 .
# ============================================================================
FROM node:16-alpine AS admin-build
WORKDIR /src
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin-web/ ./
RUN npm run build

FROM node:16-alpine AS portal-build
WORKDIR /src
COPY portal-web/package.json portal-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY portal-web/ ./
RUN npm run build

FROM nginx:1.25-alpine
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
COPY deploy/nginx/docker.conf /etc/nginx/conf.d/default.conf
COPY --from=admin-build /src/dist /usr/share/nginx/html/admin
COPY --from=portal-build /src/dist /usr/share/nginx/html/h5
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1/health || exit 1
