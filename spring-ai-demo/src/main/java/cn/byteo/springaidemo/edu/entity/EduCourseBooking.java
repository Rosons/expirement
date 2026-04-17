package cn.byteo.springaidemo.edu.entity;

import cn.byteo.springaidemo.common.entity.BaseEntity;
import cn.byteo.springaidemo.common.enums.BookingStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 用户课程预约单，对应表 {@code edu_course_booking}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("edu_course_booking")
public class EduCourseBooking extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String bookingNo;

    private String customerName;

    private String customerPhone;

    private Long courseId;

    private Long campusId;

    private BookingStatus status;

    private String remark;
}
