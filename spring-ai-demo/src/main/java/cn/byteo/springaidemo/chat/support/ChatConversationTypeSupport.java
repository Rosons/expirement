package cn.byteo.springaidemo.chat.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 会话类型归一化工具。
 */
public final class ChatConversationTypeSupport {

    private ChatConversationTypeSupport() {
    }

    public static String normalize(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }
}
