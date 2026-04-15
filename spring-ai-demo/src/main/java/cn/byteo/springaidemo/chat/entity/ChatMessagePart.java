package cn.byteo.springaidemo.chat.entity;

import cn.byteo.springaidemo.chat.enums.ChatMessagePartType;
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
 * 消息内容片段，对应表 {@code chat_message_part}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "chat_message_part", autoResultMap = true)
public class ChatMessagePart extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Integer partIndex;

    private ChatMessagePartType partType;

    private String contentText;

    private String mediaUrl;

    private String mimeType;

    @TableField(value = "payload", typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Map<String, Object> payload;
}
