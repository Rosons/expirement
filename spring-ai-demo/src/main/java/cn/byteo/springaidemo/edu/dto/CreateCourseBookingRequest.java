package cn.byteo.springaidemo.edu.dto;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file CreateCourseBookingRequest
 * @since 2026/4/17 17:01
 */
@Data
public class CreateCourseBookingRequest {

    @ToolParam(description = "课程ID")
    private Long courseId;

    @ToolParam(description = "校区ID")
    private Long campusId;

    @ToolParam(description = "客户姓名")
    private String customerName;

    @ToolParam(description = "客户联系电话")
    private String customerPhone;

    @ToolParam(description = "备注信息", required = false)
    private String remark;
}
