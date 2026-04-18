package cn.byteo.springaidemo.chat.service;

import cn.byteo.springaidemo.chat.entity.ChatFile;
import cn.byteo.springaidemo.chat.enums.ChatFileType;
import cn.byteo.springaidemo.chat.vo.ChatFileListVo;
import cn.byteo.springaidemo.chat.vo.ChatFileUploadVo;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ChatFileService {

    /**
     * @param ensureConversationType 非空时，先保证 {@code chat_conversation} 存在且类型一致（上传早于首条聊天消息的场景）
     */
    ChatFileUploadVo upload(String conversationId, String ensureConversationType,
                            Resource resource, boolean isVectorStore);

    ChatFile getChatFile(String fileId);

    Resource loadResource(ChatFile chatFile);

    /**
     * 根据会话ID和文件类型，列出所有文件
     */
    List<ChatFileListVo> listByConversationId(String conversationId, ChatFileType chatFileType);

    /**
     * @param conversationId 非空时校验文件必须属于该会话，避免误删其它会话引用
     */
    void deleteFile(String fileId, String conversationId);
}
