# portal-frontend

父工程 **expirement** 下的前端子项目：**Vue 3 + TypeScript + Vite**，UI 为 **Element Plus**。与根目录 [README.md](../README.md) 中的定位一致（学习用示例，非正式产品）。

## 技术栈

- Vue 3（`<script setup>`）、Vue Router
- Vite、TypeScript
- Axios、Markdown 与代码高亮等（见 `package.json`）

生产构建产物由 **Nginx** 托管，同源反向代理 API（配置见 `nginx/default.conf`）；与 Docker 编排说明见根目录 [DEPLOY.md](../DEPLOY.md)。

## 功能入口（路由）

门户首页为 `/`，各能力入口包括：`/products/chat`（智能聊天）、`/products/game-chat`（哄哄模拟器）、`/products/knowledge-chat`（知识问答）、`/products/customer-chat`（**智能客服**，侧重 **Function Calling / 工具调用** 演示）。路由定义见 `src/router/index.ts`。

## 本地开发

**依赖**：建议使用 **Node.js 20 LTS**（与 `Dockerfile` 中构建镜像一致）。

```bash
npm ci
npm run dev
```

开发服务器默认 **http://localhost:5173**。需**先启动**后端 `spring-ai-demo`（默认 `http://localhost:8080`），否则聊天相关接口不可用。`vite.config.ts` 中已将 `/v1/ai`、`/v2/ai`、`/game/ai`、`/knowledge/ai`、`/customer/ai`、`/ai/files` 等代理到该地址。

构建与预览：

```bash
npm run build
npm run preview
```

## 环境变量（Vite）

- 本地：可复制 **`portal-frontend/.env.example`** 为 `.env` 并按注释修改。
- **Docker 构建**：`VITE_*` 在镜像 **build** 阶段通过仓库根目录 `docker-compose.yml` 的 `build.args` 传入；变量含义与根目录 **`.env.example`** 中说明一致。

## 完整说明与部署

- 产品功能概览、在线体验链接：[README.md](../README.md)
- 端口、`.env`、打包脚本与运维：[DEPLOY.md](../DEPLOY.md)
