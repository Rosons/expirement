package cn.byteo.springaidemo.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用「暂停/有效」类业务开关：0 暂停，1 有效。
 */
@Getter
@RequiredArgsConstructor
public enum OfferStatus {

    SUSPENDED(0, "暂停开设"),
    ACTIVE(1, "有效");

    @EnumValue
    private final int code;

    /** 中文描述 */
    private final String description;
}
