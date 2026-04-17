package cn.byteo.springaidemo.edu.entity;

import cn.byteo.springaidemo.common.entity.BaseEntity;
import cn.byteo.springaidemo.common.enums.EnableStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 线下授课校区，对应表 {@code edu_campus}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("edu_campus")
public class EduCampus extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String campusCode;

    private String name;

    private String province;

    private String city;

    private String district;

    private String addressDetail;

    private String contactPhone;

    private EnableStatus status;
}
