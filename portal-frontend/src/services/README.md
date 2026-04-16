# Services 目录约定

`services` 目录聚焦“业务服务与数据访问编排”，不承载页面反馈 UI 逻辑。

## 当前分层

- `services/chat`：聊天相关业务服务（会话、消息流、文件）
- `services/http`：HTTP 客户端封装（请求/响应解包、统一错误拦截）
- `src/feedback`：页面反馈能力（确认弹窗、错误提示、全局消息）

## 导入约定

- 页面与组件优先从 `services/chat/index.ts` 导入（`from '../../services/chat'`）
- 避免直接引用 `chat-history-service.ts`、`chat-stream-service.ts`、`chat-file-service.ts` 等具体文件，降低重构耦合

## 约束

- 新增弹窗、消息提示、Toast 等 UI 反馈能力时，放到 `src/feedback`
- `services` 内模块可依赖 `services/http`，但避免反向依赖页面组件
- 对外优先暴露稳定函数接口，避免页面直接感知底层传输细节
