package cn.byteo.springaidemo.chat.entity;

import cn.byteo.springaidemo.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * 聊天会话，对应表 {@code chat_conversation}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "chat_conversation", autoResultMap = true)
public class ChatConversation extends BaseEntity {

    @TableId(value = "conversation_id", type = IdType.INPUT)
    private String conversationId;

    private String title;

    private String userId;

    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}
