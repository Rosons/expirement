package cn.byteo.springaidemo.edu.dto;

import lombok.Data;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file CreateCourseBookingRequest
 * @since 2026/4/17 17:01
 */
@Data
public class CreateCourseBookingRequest {

    private Long courseId;

    private Long campusId;

    private String customerName;

    private String customerPhone;

     private String remark;
}
