package cn.byteo.springaidemo.chat.entity;

import cn.byteo.springaidemo.chat.enums.ChatMessageRole;
import cn.byteo.springaidemo.common.entity.BaseEntity;
import cn.byteo.springaidemo.common.handler.PostgreSqlJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * 会话内消息，对应表 {@code chat_message}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Integer seq;

    private ChatMessageRole role;

    private String textContent;

    @TableField(value = "extra_metadata", typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Map<String, Object> extraMetadata;
}
