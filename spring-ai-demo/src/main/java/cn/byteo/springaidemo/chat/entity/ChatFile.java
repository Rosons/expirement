package cn.byteo.springaidemo.chat.entity;

import cn.byteo.springaidemo.chat.enums.ChatFileType;
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
 * 聊天上传文件信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "chat_file", autoResultMap = true)
public class ChatFile extends BaseEntity {

    @TableId(value = "file_id", type = IdType.INPUT)
    private String fileId;

    private String conversationId;

    private String originalFilename;

    private String storedFilename;

    private String storagePath;

    private String contentType;

    private ChatFileType fileType;

    private Long fileSize;

    @TableField(value = "metadata", typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Map<String, Object> metadata;
}
