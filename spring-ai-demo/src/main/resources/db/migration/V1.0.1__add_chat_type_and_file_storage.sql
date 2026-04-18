-- 会话类型隔离 + 聊天文件上传存储

-- ---------------------------------------------------------------------------
-- 会话维度新增 type（用于区分不同产品）
-- ---------------------------------------------------------------------------
ALTER TABLE chat_conversation
    ADD COLUMN IF NOT EXISTS type VARCHAR(64);

UPDATE chat_conversation
SET type = 'general'
WHERE type IS NULL
   OR BTRIM(type) = '';

ALTER TABLE chat_conversation
    ALTER COLUMN type SET DEFAULT 'general';

ALTER TABLE chat_conversation
    ALTER COLUMN type SET NOT NULL;

COMMENT ON COLUMN chat_conversation.type IS '会话类型/产品编码，例如 general、game-chat';

CREATE INDEX IF NOT EXISTS idx_chat_conversation_type_updated
    ON chat_conversation (type, updated_at DESC);

-- ---------------------------------------------------------------------------
-- 聊天上传文件表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_file (
    file_id            VARCHAR(64) PRIMARY KEY,
    conversation_id    VARCHAR(255) NOT NULL,
    original_filename  VARCHAR(512) NOT NULL,
    stored_filename    VARCHAR(256) NOT NULL,
    storage_path       VARCHAR(1024) NOT NULL,
    file_type          VARCHAR(255) NOT NULL,
    content_type       VARCHAR(255),
    file_size          BIGINT       NOT NULL,
    metadata           JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted            SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE chat_file IS '聊天会话上传文件信息';
COMMENT ON COLUMN chat_file.file_id IS '文件业务主键';
COMMENT ON COLUMN chat_file.conversation_id IS '关联会话ID（逻辑关联，不建外键）';
COMMENT ON COLUMN chat_file.original_filename IS '用户上传时原始文件名';
COMMENT ON COLUMN chat_file.stored_filename IS '落盘后的文件名';
COMMENT ON COLUMN chat_file.storage_path IS '相对存储根目录路径';
COMMENT ON COLUMN chat_file.file_type IS '上传文件类型 knowledge-知识库文件、normal-普通文件';
COMMENT ON COLUMN chat_file.content_type IS '文件 MIME 类型';
COMMENT ON COLUMN chat_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN chat_file.metadata IS '扩展元数据（预留向量化状态等）';
COMMENT ON COLUMN chat_file.created_at IS '创建时间，插入时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_file.updated_at IS '最后更新时间，插入与更新时由 MyBatis-Plus 填充';
COMMENT ON COLUMN chat_file.deleted IS '逻辑删除：0 未删除，1 已删除';

-- 文件查询按会话维度即可
CREATE INDEX IF NOT EXISTS idx_chat_file_conversation_created
    ON chat_file (conversation_id, created_at DESC);
