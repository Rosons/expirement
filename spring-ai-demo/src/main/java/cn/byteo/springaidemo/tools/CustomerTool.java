package cn.byteo.springaidemo.tools;

import cn.byteo.springaidemo.edu.dto.CreateCourseBookingRequest;
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
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

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
public class CustomerTool {

    /** 课程服务 */
    private final EduCourseServiceImpl eduCourseService;

    /** 校区服务 */
    private final EduCampusServiceImpl eduCampusService;

    /** 校区课程服务 */
    private final EduCampusCourseServiceImpl eduCampusCourseService;

    /** 课程预约服务 */
    private final EduCourseBookingServiceImpl eduCourseBookingService;


    /**
     * 按照学历信息查询课程列表
     */
    @Tool(description = "按照学历信息查询课程列表，参数是学历层次，返回满足条件的课程列表")
    List<EduCourseVO> queryCourseListByEducationLevel(@ToolParam(description = "") Integer educationLevel) {
        return eduCourseService.lambdaQuery()
                .le(EduCourse::getMinEducationLevel, educationLevel)
                .list()
                .stream()
                .map(CustomerTool::toEduCourseVO)
                .toList();
    }

    /**
     * 根据课程ID列表，查询提供该课程的校区列表
     */
    Map<Long, List<EduCampusVO>> queryCampusListByCourseId(List<Long> courseIds) {
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
    String createCourseBooking(List<CreateCourseBookingRequest> bookingRequests) {
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
