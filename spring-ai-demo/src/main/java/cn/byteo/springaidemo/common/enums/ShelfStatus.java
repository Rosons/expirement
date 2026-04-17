package cn.byteo.springaidemo.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用上下架/是否对外展示：0 下架，1 上架。
 */
@Getter
@RequiredArgsConstructor
public enum ShelfStatus {

    OFF(0, "下架"),
    ON(1, "上架");

    @EnumValue
    private final int code;

    /** 中文描述 */
    private final String description;
}
