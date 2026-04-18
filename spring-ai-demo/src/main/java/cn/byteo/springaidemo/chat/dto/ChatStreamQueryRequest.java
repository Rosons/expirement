package cn.byteo.springaidemo.chat.dto;

import cn.byteo.springaidemo.chat.support.ChatConversationTypeSupport;
import cn.byteo.springaidemo.exception.BusinessException;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * v1 / v2 共用的流式对话查询入参（GET Query 绑定）。
 */
@Data
public class ChatStreamQueryRequest {

    private String chatId;

    private String message;

    private String type;

    private List<MultipartFile> files;

    public String requireType() {
        String normalizedType = ChatConversationTypeSupport.normalize(type);
        if (!StringUtils.hasText(normalizedType)) {
            throw new BusinessException("type 不能为空");
        }
        return normalizedType;
    }
}
