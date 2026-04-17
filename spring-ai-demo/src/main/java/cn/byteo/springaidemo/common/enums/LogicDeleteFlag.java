package cn.byteo.springaidemo.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 逻辑删除标记，与库表 {@code deleted} 列 0/1 一致。
 */
@Getter
@RequiredArgsConstructor
public enum LogicDeleteFlag {

    NOT_DELETED(0, "未删除"),
    DELETED(1, "已删除");

    @EnumValue
    private final int code;

    /** 中文描述 */
    private final String description;
}
