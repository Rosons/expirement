package cn.byteo.springaidemo.edu.vo;

import cn.byteo.springaidemo.common.enums.ShelfStatus;
import lombok.Data;

import java.util.List;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file CourseVO
 * @since 2026/4/17 16:34
 */
@Data
public class EduCourseVO {
    private Long id;

    private String courseCode;

    private String name;

    private String description;

    private Integer minEducationLevel;

    private Long priceCent;

    private Integer durationHours;

    private String category;

    private List<String> interestTags;

    private ShelfStatus status;
}
