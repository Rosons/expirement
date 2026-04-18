# spring-ai-demo

父工程 **expirement** 下的后端模块，**学习用示例**，与根目录 [README.md](../README.md) 中的定位一致。

## 技术栈

- **Java 17**、**Spring Boot 3.5**
- **Spring AI 1.1.2**（版本由父 POM `spring-ai.version` 管理）
- PostgreSQL、MyBatis-Plus、Flyway

## 模块职责

提供门户所需的 REST / SSE 接口：多会话聊天（含多模态附件）、知识库对话、场景化对话（如哄哄模拟器）、**智能客服（Function Calling / 工具调用）** 及文件上传等；具体包结构以 `src/main/java` 为准。

## 本地运行

1. 安装 **JDK 17**、**Maven 3.9+**，并准备可访问的 **PostgreSQL**，库名与账号与配置一致（见 `src/main/resources/application.yaml`，可通过环境变量覆盖）。
2. 配置大模型相关环境变量（如 `SPRING_AI_OPENAI_API_KEY` 等，与根目录 `.env.example` 对应项含义相同）。
3. 在本目录执行：

```bash
mvn spring-boot:run
```

或在仓库根目录：

```bash
mvn -pl spring-ai-demo -am spring-boot:run
```

默认监听端口 **8080**（可用 `SERVER_PORT` 覆盖）。

## 配置与部署

- 单机配置：`src/main/resources/application.yaml`。
- **Docker** 镜像由**仓库根目录**的 `spring-ai-demo/Dockerfile` 构建，与 `docker-compose.yml` 配合使用；完整步骤见根目录 [DEPLOY.md](../DEPLOY.md)。

## 与前端联调

本地先启动本模块，再启动 `portal-frontend` 的 `npm run dev`；前端开发代理将 API 请求转到 `http://localhost:8080`。
