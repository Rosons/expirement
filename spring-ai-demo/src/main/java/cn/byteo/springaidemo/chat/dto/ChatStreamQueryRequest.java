package cn.byteo.springaidemo.chat.dto;

import lombok.Data;

/**
 * v1 / v2 共用的流式对话查询入参（GET Query 绑定）。
 */
@Data
public class ChatStreamQueryRequest {

    private String chatId;

    private String message;
}
