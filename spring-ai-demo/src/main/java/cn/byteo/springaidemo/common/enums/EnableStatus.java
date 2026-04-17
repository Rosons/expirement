package cn.byteo.springaidemo.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用启用/停用：0 停用，1 启用（业务场景可理解为「停业/营业」等）。
 */
@Getter
@RequiredArgsConstructor
public enum EnableStatus {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    @EnumValue
    private final int code;

    /** 中文描述 */
    private final String description;
}
