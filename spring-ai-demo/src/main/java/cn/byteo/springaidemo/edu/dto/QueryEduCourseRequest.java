package cn.byteo.springaidemo.edu.dto;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file QueryEduCourseRequest
 * @since 2026/4/17
 */
@Data
public class QueryEduCourseRequest {

    @ToolParam(description = "报名最低学历等级：0～4 一位数（0 不限；1 高中及以上；2 大专及以上；3 本科及以上；4 硕士及以上）")
    private Integer educationLevel;

    @ToolParam(description = "兴趣方向，其值必须属于queryCourseInterestList工具中查询到的值", required = false)
    private List<String> interestDirections;
}
