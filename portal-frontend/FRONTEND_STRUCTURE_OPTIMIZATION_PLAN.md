# portal-frontend 前端结构优化方案（草案）

## 1. 背景与目标

当前 `portal-frontend` 的主干分层（`router` / `views` / `components` / `services` / `api`）方向是正确的，但已出现以下可维护性风险：

- 核心组件体量过大，职责混杂；
- 聊天页面骨架和样式重复；
- `api` 与 `services` 边界不够直观；
- 通用类型与业务域类型有耦合。

本方案目标是：在不影响线上行为的前提下，通过分批次、可回滚的方式，提升可维护性与后续开发效率。

## 2. 当前结构主要问题

### 2.1 大文件问题（高优先级）

- `src/components/chat/ChatWorkspace.vue`：约 `964` 行
- `src/components/chat/KnowledgeSessionFilePanel.vue`：约 `771` 行
- `src/components/chat/ChatMessageBubble.vue`：约 `586` 行
- `src/services/chat/chat-service.ts`：约 `394` 行

影响：

- 代码评审成本高；
- 并行开发冲突概率上升；
- 单测和回归测试难度增大。

### 2.2 页面重复问题（中优先级）

`General` / `Knowledge` / `Game` 三个聊天页面存在相似的头部结构和样式，改动时需要多处同步，容易漏改。

### 2.3 分层边界问题（中优先级）

当前 `src/api/index.ts` 再导出了 `services/http/http-client`，会让目录语义变得模糊，不利于新人理解层级边界。

### 2.4 类型归属问题（中优先级）

`ApiResponse<T>` 位于 `src/types/chat.ts`，但它被全局 HTTP 客户端使用。随着非聊天模块增加，类型归属会越来越不清晰。

## 3. 目标目录结构（建议）

```text
portal-frontend/src
├─ app
│  ├─ main.ts
│  ├─ router/
│  │  └─ index.ts
│  └─ providers/
├─ pages
│  ├─ portal/
│  │  └─ PortalHomePage.vue
│  └─ chat/
│     ├─ GeneralChatPage.vue
│     ├─ KnowledgeChatPage.vue
│     ├─ GameChatPage.vue
│     └─ components/
│        └─ ChatPageLayout.vue
├─ features
│  └─ chat
│     ├─ components/
│     │  ├─ ChatWorkspace/
│     │  │  ├─ index.vue
│     │  │  ├─ ConversationSidebar.vue
│     │  │  ├─ MessageList.vue
│     │  │  ├─ Composer.vue
│     │  │  └─ TrailingPanelSlot.vue
│     │  ├─ ChatMessageBubble.vue
│     │  └─ KnowledgeSessionFilePanel/
│     │     ├─ index.vue
│     │     ├─ FileUploadZone.vue
│     │     ├─ FileList.vue
│     │     └─ FilePreview.vue
│     ├─ composables/
│     │  ├─ useConversationState.ts
│     │  ├─ useHistoryPagination.ts
│     │  ├─ useStreamChat.ts
│     │  └─ useScrollControl.ts
│     ├─ services/
│     │  ├─ chat-api.ts
│     │  ├─ chat-stream-parser.ts
│     │  ├─ chat-history-mapper.ts
│     │  └─ workspace-api-presets.ts
│     ├─ model/
│     │  ├─ chat.types.ts
│     │  └─ chat.constants.ts
│     └─ index.ts
├─ shared
│  ├─ api
│  │  ├─ env.ts
│  │  ├─ endpoints/
│  │  │  └─ chat-endpoints.ts
│  │  └─ http/
│  │     └─ api-client.ts
│  ├─ types
│  │  └─ api.ts
│  ├─ ui
│  │  └─ GlobalErrorToast.vue
│  ├─ services
│  │  └─ notifications/
│  │     └─ error-toast-service.ts
│  └─ utils
│     ├─ markdown.ts
│     ├─ error-message.ts
│     └─ format.ts
└─ style.css
```

## 4. 分批迁移计划（可回滚）

### 批次 1：壳层整理（无行为变更）

- 建立 `pages` / `features` / `shared` 结构；
- 先做文件搬迁与导出转发，不改业务逻辑；
- 保持路由路径与运行行为一致。

验收：

- `npm run build` 通过；
- 页面行为一致。

### 批次 2：抽聊天页面公共骨架

- 抽出 `ChatPageLayout.vue`；
- 替换 `General` / `Knowledge` / `Game` 页面中重复的 header / back button / 布局样式；
- 页面内容仍由各自页面传入。

验收：

- 三个页面视觉与交互保持一致；
- 重复样式明显减少。

### 批次 3：拆分 `ChatWorkspace.vue`（核心）

- 按职责拆出 composables：
  - `useConversationState`
  - `useHistoryPagination`
  - `useStreamChat`
  - `useScrollControl`
- 再按 UI 区域拆子组件：
  - 会话侧栏
  - 消息列表
  - 输入区
  - 拓展面板插槽区

验收：

- `ChatWorkspace.vue` 控制在约 `250-350` 行；
- 主流程功能保持不变；
- 可新增至少 1-2 个 composable 单元测试（如分页和流式拼接）。

### 批次 4：边界治理与类型归位

- `ApiResponse<T>` 移至 `shared/types/api.ts`；
- 统一约定：
  - `shared/api/*` 仅放 env / endpoint / http client；
  - 业务编排放 `features/*/services`；
- 拆分 `chat-service.ts` 为 `chat-api` / `chat-stream-parser` / `chat-history-mapper`。

验收：

- import 方向清晰，避免层级反向依赖；
- 新模块接入路径更直观。

## 5. 约束与风险控制

- 每个批次都单独提交，禁止跨批次混改；
- 每批次都执行构建与手工回归；
- 遇到行为风险时优先回滚当前批次，不强行推进；
- 优先“低风险高收益”改造，不做一次性大重写。

## 6. Definition of Done（整体完成标准）

- 核心大文件体量显著下降；
- 聊天页面重复结构被抽象复用；
- API 与 Service 边界清晰；
- 通用类型归位；
- 构建通过，关键路径（通用聊天、知识问答、游戏聊天）回归通过。

## 7. 后续执行建议

建议先实施 **批次 1 + 批次 2**，确认稳定后再进入 **批次 3**。  
其中 `ChatWorkspace` 的拆分属于收益最高但改动面最大的部分，应单独安排时间窗口推进。
