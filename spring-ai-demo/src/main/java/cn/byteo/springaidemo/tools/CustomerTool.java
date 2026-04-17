package cn.byteo.springaidemo.tools;

import cn.byteo.springaidemo.edu.dto.CreateCourseBookingRequest;
import cn.byteo.springaidemo.edu.dto.QueryEduCourseRequest;
import cn.byteo.springaidemo.edu.entity.EduCampus;
import cn.byteo.springaidemo.edu.entity.EduCampusCourse;
import cn.byteo.springaidemo.edu.entity.EduCourse;
import cn.byteo.springaidemo.edu.entity.EduCourseBooking;
import cn.byteo.springaidemo.edu.service.impl.EduCampusCourseServiceImpl;
import cn.byteo.springaidemo.edu.service.impl.EduCampusServiceImpl;
import cn.byteo.springaidemo.edu.service.impl.EduCourseBookingServiceImpl;
import cn.byteo.springaidemo.edu.service.impl.EduCourseServiceImpl;
import cn.byteo.springaidemo.edu.vo.EduCampusVO;
import cn.byteo.springaidemo.edu.vo.EduCourseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>智能客服产品相关的工具</p>
 *
 * @author Roson
 * @file CustomerTool
 * @since 2026/4/17 15:53
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTool {

    /** 课程服务 */
    private final EduCourseServiceImpl eduCourseService;

    /** 校区服务 */
    private final EduCampusServiceImpl eduCampusService;

    /** 校区课程服务 */
    private final EduCampusCourseServiceImpl eduCampusCourseService;

    /** 课程预约服务 */
    private final EduCourseBookingServiceImpl eduCourseBookingService;


    @Tool(name = "queryCourseInterestList",
            description = "查询已有课程所属的全部兴趣列表")
    List<String> queryCourseInterestList() {
        return eduCourseService.lambdaQuery()
                .select(EduCourse::getInterestTags)
                .list()
                .stream()
                .flatMap(course -> course.getInterestTags().stream())
                .distinct()
                .toList();
    }


    /**
     * 按照学历信息和兴趣方向查询课程列表
     */
    @Tool(name = "queryCourseListByEducationLevel",
            description = "根据学历信息和兴趣方向，查询满足条件的课程列表")
    List<EduCourseVO> queryCourseListByEducationLevel(@ToolParam(description = "查询课程条件")
                                                      QueryEduCourseRequest queryEduCourseRequest) {
        // 用户学历等级
        Integer educationLevel = queryEduCourseRequest.getEducationLevel();
        // 用户感兴趣方向
        List<String> interestDirections = queryEduCourseRequest.getInterestDirections();
        if (!CollectionUtils.isEmpty(interestDirections)) {
            log.info("查询课程列表，学历等级：{}，兴趣方向：{}", educationLevel, interestDirections);
        }
        return eduCourseService.lambdaQuery()
                .le(EduCourse::getMinEducationLevel, educationLevel)
                .list()
                .stream()
                .filter(course -> {
                    if (CollectionUtils.isEmpty(interestDirections)) {
                        return true;
                    }
                    // 课程兴趣方向
                    List<String> courseInterestTags = course.getInterestTags();
                    // 判断课程兴趣方向与用户兴趣方向是否有交集
                    return courseInterestTags.stream().anyMatch(interestDirections::contains);
                })
                .map(CustomerTool::toEduCourseVO)
                .toList();
    }

    /**
     * 根据课程ID列表，查询提供该课程的校区列表
     */
    @Tool(name = "queryCampusListMapByCourseIds",
            description = "根据课程ID列表，查询提供该课程的校区信息集合，返回每一个课程ID对应的校区信息列表")
    Map<Long, List<EduCampusVO>> queryCampusListMapByCourseIds(@ToolParam(description = "课程ID列表")
                                                               List<Long> courseIds) {
        Map<Long, List<Long>> courseIdToCampusIds = eduCampusCourseService.lambdaQuery()
                .in(EduCampusCourse::getCourseId, courseIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        EduCampusCourse::getCourseId,
                        Collectors.mapping(EduCampusCourse::getCampusId, Collectors.toList())
                ));
        // 查询校区信息
        List<Long> campusIds = courseIdToCampusIds
                .values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, EduCampusVO> eduCampusVOMap = eduCampusService.lambdaQuery()
                .in(EduCampus::getId, campusIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        EduCampus::getId,
                        CustomerTool::toEduCampusVO
                ));
        // 组装结果
        return courseIdToCampusIds.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(eduCampusVOMap::get)
                                .toList()
                ));
    }

    /**
     * 根据提交的预约信息列表，新增预约单
     */
    @Tool(name = "createCourseBooking",
            description = "根据提交的预约信息列表，生成预约单，并返回预约单号")
    String createCourseBooking(@ToolParam(description = "预约信息列表")
                               List<CreateCourseBookingRequest> bookingRequests) {
        // 生成预约单号
        String bookingNo = "BK" + System.currentTimeMillis();
        for (CreateCourseBookingRequest request : bookingRequests) {
            EduCourseBooking booking = new EduCourseBooking();
            booking.setCourseId(request.getCourseId());
            booking.setCampusId(request.getCampusId());
            booking.setCustomerName(request.getCustomerName());
            booking.setCustomerPhone(request.getCustomerPhone());
            booking.setRemark(request.getRemark());
            booking.setBookingNo(bookingNo);
            eduCourseBookingService.save(booking);
        }
        return bookingNo;
    }

    private static EduCampusVO toEduCampusVO(EduCampus campus) {
        EduCampusVO campusVO = new EduCampusVO();
        campusVO.setId(campus.getId());
        campusVO.setCampusCode(campus.getCampusCode());
        campusVO.setName(campus.getName());
        campusVO.setProvince(campus.getProvince());
        campusVO.setCity(campus.getCity());
        campusVO.setDistrict(campus.getDistrict());
        campusVO.setAddressDetail(campus.getAddressDetail());
        campusVO.setContactPhone(campus.getContactPhone());
        campusVO.setStatus(campus.getStatus());
        return campusVO;
    }

    private static EduCourseVO toEduCourseVO(EduCourse course) {
        EduCourseVO courseVO = new EduCourseVO();
        courseVO.setId(course.getId());
        courseVO.setCourseCode(course.getCourseCode());
        courseVO.setName(course.getName());
        courseVO.setDescription(course.getDescription());
        courseVO.setMinEducationLevel(course.getMinEducationLevel());
        courseVO.setPriceCent(course.getPriceCent());
        courseVO.setDurationHours(course.getDurationHours());
        courseVO.setCategory(course.getCategory());
        courseVO.setInterestTags(course.getInterestTags());
        courseVO.setStatus(course.getStatus());
        return courseVO;
    }
}
