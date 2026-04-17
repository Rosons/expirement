package cn.byteo.springaidemo.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用预约/订单类生命周期状态（数据库存字符串码）。
 */
@Getter
@RequiredArgsConstructor
public enum BookingStatus {

    PENDING("pending", "待确认"),
    CONFIRMED("confirmed", "已确认"),
    CANCELLED("cancelled", "已取消"),
    COMPLETED("completed", "已完成");

    @EnumValue
    private final String code;

    /** 中文描述 */
    private final String description;
}
