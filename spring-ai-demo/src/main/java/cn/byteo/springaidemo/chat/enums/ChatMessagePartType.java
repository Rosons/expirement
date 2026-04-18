package cn.byteo.springaidemo.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

import java.util.Locale;

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

    /**
     * 根据 MIME 类型推断适合的消息片段类型（用于附件、媒体等；不含工具调用语义）。
     * <p>
     * 会忽略参数段（如 {@code text/plain; charset=utf-8} 只按 {@code text/plain} 判断）。
     * 分类规则与 {@link cn.byteo.springaidemo.util.FileExtensionMimeTypeUtils} 常见输出对齐：
     * {@code image/*}、{@code audio/*}、{@code video/*}、{@code text/*}，以及类文本的 {@code application/json}、{@code application/xml}、{@code application/yaml}、{@code application/x-ndjson} 等。
     * </p>
     * 无法归类时返回 {@link #OTHER}。
     *
     * @param mimeType MIME 字符串，如 {@code image/png}、{@code application/pdf}；可为 null 或空白
     */
    public static ChatMessagePartType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return OTHER;
        }
        String base = mimeType.trim();
        int semi = base.indexOf(';');
        if (semi >= 0) {
            base = base.substring(0, semi).trim();
        }
        if (base.isEmpty()) {
            return OTHER;
        }
        base = base.toLowerCase(Locale.ROOT);
        int slash = base.indexOf('/');
        if (slash <= 0 || slash == base.length() - 1) {
            return OTHER;
        }
        String primary = base.substring(0, slash);
        return switch (primary) {
            case "image" -> IMAGE_URL;
            case "audio" -> AUDIO_URL;
            case "video" -> VIDEO_URL;
            case "text" -> TEXT;
            case "application" -> mapApplicationSubtype(base.substring(slash + 1));
            default -> OTHER;
        };
    }

    private static ChatMessagePartType mapApplicationSubtype(String subtype) {
        return switch (subtype) {
            case "json",
                 "xml",
                 "javascript",
                 "x-www-form-urlencoded",
                 "graphql",
                 "graphql-response+json",
                 "yaml",
                 "x-yaml",
                 "x-ndjson" -> TEXT;
            default -> {
                if (subtype.endsWith("+json") || subtype.endsWith("+xml")) {
                    yield TEXT;
                }
                yield FILE_REF;
            }
        };
    }

}
