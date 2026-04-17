package cn.byteo.springaidemo.edu.entity;

import cn.byteo.springaidemo.common.entity.BaseEntity;
import cn.byteo.springaidemo.common.handler.PostgreSqlJsonbStringListTypeHandler;
import cn.byteo.springaidemo.common.enums.ShelfStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 培训课程，对应表 {@code edu_course}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "edu_course", autoResultMap = true)
public class EduCourse extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String courseCode;

    private String name;

    private String description;

    private Integer minEducationLevel;

    private Long priceCent;

    private Integer durationHours;

    private String category;

    @TableField(value = "interest_tags", typeHandler = PostgreSqlJsonbStringListTypeHandler.class)
    private List<String> interestTags;

    private ShelfStatus status;
}
