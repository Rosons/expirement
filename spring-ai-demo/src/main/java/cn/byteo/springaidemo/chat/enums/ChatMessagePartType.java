package cn.byteo.springaidemo.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 消息片段类型，与 {@code chat_message_part.part_type} 存储值一致。
 */
@Getter
public enum ChatMessagePartType {

    TEXT("TEXT", "纯文本片段"),
    IMAGE_URL("IMAGE_URL", "图片资源地址"),
    AUDIO_URL("AUDIO_URL", "音频资源地址"),
    VIDEO_URL("VIDEO_URL", "视频资源地址"),
    FILE_REF("FILE_REF", "通用文件引用（上传后的 URL 或内部路径）"),
    INLINE_DATA("INLINE_DATA", "内联二进制/小数据元信息（大文件请用对象存储 + FILE_REF）"),
    TOOL_CALL("TOOL_CALL", "模型发起的工具调用描述"),
    TOOL_RESULT("TOOL_RESULT", "工具执行结果"),
    OTHER("OTHER", "其他类型，细节见 payload");

    @EnumValue
    private final String code;

    /** 人类可读说明，可用于文档、管理端展示等（不入库） */
    private final String description;

    ChatMessagePartType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
