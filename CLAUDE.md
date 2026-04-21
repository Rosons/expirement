# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个整合 Spring Boot 3 + Spring AI 1.1.2 后端与 Vue 3 + Vite 前端的全栈示例项目，用于学习对话类 AI 应用的常见模式。项目通过统一门户集成了多种 AI 对话能力，包括多模态聊天、哄哄模拟器、知识问答和智能客服（Function Calling）。

**重要说明**：本仓库是**学习与交流用的示例工程**，不是正式对外产品，不承诺功能完整性、稳定性或长期维护。

## 项目结构

### 多模块架构

- **根目录**：Docker Compose 配置、部署脚本、环境变量管理
- **spring-ai-demo/**：Spring Boot 3 后端（Java 17）
- **portal-frontend/**：Vue 3 + TypeScript + Vite 前端

### 后端架构 (spring-ai-demo)

- **包结构**：`cn.byteo.springaidemo`
- **核心模块**：
  - `chat/`：聊天相关的实体、服务、控制器和持久化逻辑
  - `edu/`：智能客服演示模块（Function Calling 示例）
  - `config/`：Spring AI、向量存储、聊天记忆配置
  - `common/`：通用工具类、异常处理、类型处理器

### 前端架构 (portal-frontend)

- **Vue 3 Composition API** + TypeScript
- **核心模块**：
  - `views/`：路由页面组件（门户首页、通用聊天、游戏聊天、知识问答、智能客服）
  - `components/chat/`：可复用的聊天组件
  - `services/chat/`：聊天相关业务逻辑
  - `api/`：API 接口定义和 HTTP 客户端

## 开发环境与工具

### Java 环境要求

- **JDK 17**（Spring Boot 3 要求）
- Maven 构建工具
- **注意**：部分开发环境可能有特定的 JDK 路径配置（参考 `.cursor/rules/` 目录），但通用要求是 JDK 17 即可

### 主要技术栈

- **后端**：Spring Boot 3.2.5, Spring AI 1.1.2, MyBatis-Plus, PostgreSQL, Flyway
- **前端**：Vue 3.5.32, TypeScript, Element Plus, Vite 8.0.4
- **数据库**：PostgreSQL 16-alpine
- **AI 集成**：兼容 OpenAI API 的服务（如阿里云 DashScope 兼容模式）

## 构建与运行

### 使用 Docker Compose（推荐）

```bash
# 1. 复制环境变量文件
cp .env.example .env
# 编辑 .env 文件，设置 SPRING_AI_OPENAI_API_KEY 等必需变量

# 2. 启动所有服务
docker compose up -d --build

# 3. 访问应用
# 前端门户：http://localhost:80
# 后端API：http://localhost:8080
```

### 本地开发（不使用 Docker）

#### 后端启动

```bash
cd spring-ai-demo
mvn spring-boot:run
```

**注意**：需要本地安装 PostgreSQL 并确保配置与 [application.yaml](spring-ai-demo/src/main/resources/application.yaml) 一致。

#### 前端启动

```bash
cd portal-frontend
npm ci  # 或 npm install
npm run dev
```

前端开发服务器通常在 `http://localhost:5173`。

## 环境变量配置

### 必需配置

- `SPRING_AI_OPENAI_API_KEY`：兼容 OpenAI 的 API Key（例如阿里云 DashScope）

### 主要环境变量

关键变量定义在 [.env.example](.env.example)，包括：

- **端口配置**：`FRONTEND_HTTP_PORT`, `BACKEND_HTTP_PORT`
- **数据库配置**：`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- **AI 模型配置**：模型名称、基础 URL、嵌入维度等
- **RAG 参数**：相似度阈值、top-k 数量
- **前端构建参数**：API 基础路径、聊天接口版本

### 环境变量映射

后端环境变量通过 `application.yaml` 中的 `${...}` 语法引用，支持 Spring Boot 的 relaxed binding。

## 数据库管理

### 数据库迁移

项目使用 Flyway 进行数据库版本管理：

- 迁移文件位置：`spring-ai-demo/src/main/resources/db/migration/`
- 自动执行：应用启动时自动运行迁移
- 基线迁移：`flyway.baseline-on-migrate: true`

### 主要数据表

- `chat_conversation`：对话会话
- `chat_message`：聊天消息
- `chat_message_part`：消息部分（支持多模态）
- `chat_file`：聊天相关文件
- `edu_`*：智能客服演示相关的课程、校区、预约表

## API 架构

### 后端 API 端点

- **通用聊天**：`/v1/ai/chat`, `/v2/ai/chat`（支持流式和会话记忆持久化）
- **知识问答**：`/knowledge/chat/stream`（RAG 向量检索）
- **智能客服**：`/customer/chat/stream`（Function Calling 演示）
- **游戏聊天**：`/game/chat/stream`（哄哄模拟器，提示词工程）
- **文件管理**：`/chat/file/`*（文件上传、列表、预览）

### 前端 API 集成

- API 配置：[portal-frontend/src/api/env.ts](portal-frontend/src/api/env.ts)
- HTTP 客户端：[portal-frontend/src/services/http/http-client.ts](portal-frontend/src/services/http/http-client.ts)
- 聊天流式处理：[portal-frontend/src/services/chat/chat-stream-service.ts](portal-frontend/src/services/chat/chat-stream-service.ts)

## Spring AI 集成

### 主要配置

- **聊天模型**：`AlibabaOpenAiChatModel`（自定义聊天模型适配器）
- **向量存储**：`SimpleVectorStore`（本地文件持久化）
- **聊天记忆**：`PersistentChatMemory`（基于数据库的持久化记忆）
- **RAG 增强**：`QuestionAnswerAdvisor`（向量检索增强生成）

### 多模态支持

- **支持的文件类型**：图片、音频、PDF、Office 文档、纯文本
- **文件处理**：分片向量化、相似度检索
- **消息适配器**：`SpringAiMessagesAdapter` 处理 Spring AI 消息格式

## 部署与打包

### 部署包生成

项目提供跨平台部署包生成脚本：

- **Windows**：[scripts/package-docker-deploy.ps1](scripts/package-docker-deploy.ps1)
- **Linux/macOS**：[scripts/package-docker-deploy.sh](scripts/package-docker-deploy.sh)

### 数据持久化

Docker Compose 使用 `./data` 目录持久化：

- `data/postgres/`：PostgreSQL 数据
- `data/chat-files/`：上传的文件
- `data/vector-store/`：向量存储文件

## 开发注意事项

### Java 版本一致性

确保使用 JDK 17，避免因版本不匹配导致的编译问题。项目使用 Spring Boot 3，需要 JDK 17 或更高版本。

### 文件上传限制

- 后端：`spring.servlet.multipart.max-file-size=100MB`
- Nginx：`client_max_body_size 100m`

### 流式输出配置

前端 Nginx 配置已设置 `proxy_buffering off` 确保流式输出正常工作。

### 向量存储配置

RAG 相关参数可在 `.env` 中调整：

- `APP_KNOWLEDGE_CHAT_RAG_SIMILARITY_THRESHOLD`：相似度阈值（默认 0.5）
- `APP_KNOWLEDGE_CHAT_RAG_TOP_K`：检索片段数量（默认 3）

## 故障排除

### 常见问题

1. **Docker Compose 启动失败**：检查 `.env` 文件中的 `SPRING_AI_OPENAI_API_KEY` 是否正确设置
2. **数据库连接失败**：确保 PostgreSQL 容器健康检查通过
3. **文件上传失败**：检查 Nginx 和 Spring Boot 的文件大小限制配置
4. **流式输出异常**：确保 Nginx 代理缓冲已关闭

### 日志查看

```bash
# 查看所有容器日志
docker compose logs -f

# 查看特定容器日志
docker compose logs -f backend
docker compose logs -f frontend
```

## 项目特定约定

### 代码风格

- 后端使用 MyBatis-Plus 进行数据库操作
- 实体类继承 `BaseEntity` 包含公共字段
- 复杂SQL逻辑编写时，必须通过mapper.xml文件实现，禁止使用注解编写SQL
- 使用枚举类定义状态和类型常量
- 异常处理通过 `GlobalExceptionHandler` 统一管理

### 前端架构模式

- Composition API + TypeScript
- 按功能模块组织代码
- 使用 `auto-imports` 自动导入常用组件
- 聊天组件采用可复用的 workspace 设计

### API 版本管理

- 聊天 API 有 v1 和 v2 版本，通过 `VITE_CHAT_API_VERSION` 环境变量控制前端使用哪个版本

