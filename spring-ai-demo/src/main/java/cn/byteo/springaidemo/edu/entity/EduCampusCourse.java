package cn.byteo.springaidemo.edu.entity;

import cn.byteo.springaidemo.common.entity.BaseEntity;
import cn.byteo.springaidemo.common.enums.OfferStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 校区开设课程关联，对应表 {@code edu_campus_course}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("edu_campus_course")
public class EduCampusCourse extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long campusId;

    private Long courseId;

    private Integer quotaPerMonth;

    private String remark;

    private OfferStatus status;
}
