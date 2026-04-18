package cn.byteo.springaidemo.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file FileType
 * @since 2026/4/16
 */
@Getter
public enum ChatFileType {

    KNOWLEDGE("knowledge", "知识库文件"),
    NORMAL("normal", "普通文件");

    @EnumValue
    private final String code;

    /** 人类可读说明，可用于文档、管理端展示等（不入库） */
    private final String description;

    ChatFileType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
