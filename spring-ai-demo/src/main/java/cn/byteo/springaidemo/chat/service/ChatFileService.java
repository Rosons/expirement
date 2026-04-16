package cn.byteo.springaidemo.chat.service;

import cn.byteo.springaidemo.chat.entity.ChatFile;
import cn.byteo.springaidemo.chat.vo.ChatFileListVo;
import cn.byteo.springaidemo.chat.vo.ChatFileUploadVo;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ChatFileService {

    /**
     * @param ensureConversationType 非空时，先保证 {@code chat_conversation} 存在且类型一致（上传早于首条聊天消息的场景）
     */
    ChatFileUploadVo upload(String conversationId, Resource resource, String ensureConversationType);

    default ChatFileUploadVo upload(String conversationId, Resource resource) {
        return upload(conversationId, resource, null);
    }

    ChatFile getChatFile(String fileId);

    Resource loadResource(ChatFile chatFile);

    List<ChatFileListVo> listByConversationId(String conversationId);

    /**
     * @param conversationId 非空时校验文件必须属于该会话，避免误删其它会话引用
     */
    void deleteFile(String fileId, String conversationId);
}
