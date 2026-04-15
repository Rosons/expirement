-- =============================================================================
-- 聊天持久化：会话 + 消息 + 可选消息片段（多模态 / 一条消息多段内容）
-- conversation_id 与 ChatController 的 chatId、Spring AI ChatMemory.CONVERSATION_ID 一致
--
-- 枚举约定（由 Java 校验与写入，库内为 VARCHAR，不在此做 CHECK）：
--   chat_message.role
--     -> cn.byteo.springaidemo.chat.enums.ChatMessageRole
--     建议取值：USER, ASSISTANT, SYSTEM, TOOL
--   chat_message_part.part_type
--     -> cn.byteo.springaidemo.chat.enums.ChatMessagePartType
--     建议取值：TEXT, IMAGE_URL, AUDIO_URL, VIDEO_URL, FILE_REF, INLINE_DATA,
--              TOOL_CALL, TOOL_RESULT, OTHER
--
-- 公共字段 created_at、updated_at、deleted：由应用通过 MyBatis-Plus 填充与逻辑删除维护，无数据库触发器
-- deleted：0 未删除，1 已删除（与实体 @TableLogic 及全局配置一致）
-- 不在库级创建外键，关联关系由应用保证（可与逻辑删除策略配合）
-- 可重复执行：CREATE TABLE / CREATE INDEX 均带 IF NOT EXISTS
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 会话表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversation (
    conversation_id VARCHAR(255) PRIMARY KEY,
    title             VARCHAR(512),
    user_id           VARCHAR(128),
    metadata          JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE chat_conversation IS '聊天会话';
COMMENT ON COLUMN chat_conversation.conversation_id IS '业务会话主键，对应接口 chatId / CONVERSATION_ID';
COMMENT ON COLUMN chat_conversation.title IS '会话标题，可空';
COMMENT ON COLUMN chat_conversation.user_id IS '归属用户标识，可空，多租户/登录后补充';
COMMENT ON COLUMN chat_conversation.metadata IS '会话级扩展 JSON：标签、模型名等';
COMMENT ON COLUMN chat_conversation.created_at IS '创建时间，插入时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_conversation.updated_at IS '最后更新时间，插入与更新时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_conversation.deleted IS '逻辑删除：0 未删除，1 已删除';

-- ---------------------------------------------------------------------------
-- 消息表（一条逻辑消息一行，会话内顺序由 seq 唯一且递增）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message (
    id                BIGSERIAL PRIMARY KEY,
    conversation_id   VARCHAR(255) NOT NULL,
    seq               INTEGER      NOT NULL,
    role              VARCHAR(32)  NOT NULL,
    text_content      TEXT,
    extra_metadata    JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_chat_message_conv_seq UNIQUE (conversation_id, seq)
);

COMMENT ON TABLE chat_message IS '会话内消息主表';
COMMENT ON COLUMN chat_message.id IS '自增主键';
COMMENT ON COLUMN chat_message.conversation_id IS '所属会话，对应 chat_conversation.conversation_id（库级无外键）';
COMMENT ON COLUMN chat_message.seq IS '会话内序号，从 1 递增，保证展示与拉取顺序';
COMMENT ON COLUMN chat_message.role IS '消息角色，取值见文件头枚举说明（ChatMessageRole）';
COMMENT ON COLUMN chat_message.text_content IS '主文本；纯文本消息通常只填本列';
COMMENT ON COLUMN chat_message.extra_metadata IS '消息级扩展 JSON：tool_call_id、name、finish_reason 等';
COMMENT ON COLUMN chat_message.created_at IS '创建时间，插入时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_message.updated_at IS '最后更新时间，插入与更新时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_message.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE INDEX IF NOT EXISTS idx_chat_message_conv_seq
    ON chat_message (conversation_id, seq);

CREATE INDEX IF NOT EXISTS idx_chat_message_conv_created
    ON chat_message (conversation_id, created_at);

-- ---------------------------------------------------------------------------
-- 消息片段表（一条消息可含多段：文本、图片 URL、文件引用等）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message_part (
    id           BIGSERIAL PRIMARY KEY,
    message_id   BIGINT       NOT NULL,
    part_index   INTEGER      NOT NULL,
    part_type    VARCHAR(32)  NOT NULL,
    content_text TEXT,
    media_url    TEXT,
    mime_type    VARCHAR(128),
    payload      JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_chat_message_part_msg_idx UNIQUE (message_id, part_index)
);

COMMENT ON TABLE chat_message_part IS '单条消息内的多段内容';
COMMENT ON COLUMN chat_message_part.id IS '自增主键';
COMMENT ON COLUMN chat_message_part.message_id IS '所属消息，对应 chat_message.id（库级无外键）';
COMMENT ON COLUMN chat_message_part.part_index IS '片段顺序，从 0 递增';
COMMENT ON COLUMN chat_message_part.part_type IS '片段类型，取值见文件头枚举说明（ChatMessagePartType）';
COMMENT ON COLUMN chat_message_part.content_text IS '该片段纯文本';
COMMENT ON COLUMN chat_message_part.media_url IS '媒体或文件的可访问 URL / 内部资源定位';
COMMENT ON COLUMN chat_message_part.mime_type IS 'MIME 类型，如 image/png、application/pdf';
COMMENT ON COLUMN chat_message_part.payload IS '结构化扩展 JSON；大文件仅存元数据或存储 key';
COMMENT ON COLUMN chat_message_part.created_at IS '创建时间，插入时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_message_part.updated_at IS '最后更新时间，插入与更新时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_message_part.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE INDEX IF NOT EXISTS idx_chat_message_part_message
    ON chat_message_part (message_id, part_index);
