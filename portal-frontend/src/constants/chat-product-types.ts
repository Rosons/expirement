/**
 * 与后端 `chat_conversation.type`、流式聊天查询参数 `type` 取值对齐。
 * 新增产品时在此集中维护，避免散落在各页面。
 */
export const CHAT_PRODUCT_TYPE_GENERAL = 'general';

export const CHAT_PRODUCT_TYPE_GAME_CHAT = 'game-chat';

/** 与知识问答控制器会话维度一致 */
export const CHAT_PRODUCT_TYPE_KNOWLEDGE = 'knowledge';

/** 与 {@code CustomerController} 流式对话 `type`、会话落库类型一致 */
export const CHAT_PRODUCT_TYPE_CUSTOMER = 'customer';
