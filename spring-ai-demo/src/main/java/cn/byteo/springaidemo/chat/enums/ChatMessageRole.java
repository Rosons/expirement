package cn.byteo.springaidemo.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 聊天消息角色，与 {@code chat_message.role} 存储值一致。
 */
@Getter
public enum ChatMessageRole {

    USER("USER", "终端用户发送的内容"),
    ASSISTANT("ASSISTANT", "大模型或助手返回的内容"),
    SYSTEM("SYSTEM", "系统提示词或系统级说明"),
    TOOL("TOOL", "工具调用或工具返回（函数调用等）");

    @EnumValue
    private final String code;

    /** 人类可读说明，可用于文档、管理端展示等（不入库） */
    private final String description;

    ChatMessageRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
