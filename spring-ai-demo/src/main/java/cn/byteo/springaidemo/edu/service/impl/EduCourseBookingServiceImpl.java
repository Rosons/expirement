package cn.byteo.springaidemo.edu.service.impl;

import cn.byteo.springaidemo.edu.entity.EduCourseBooking;
import cn.byteo.springaidemo.edu.mapper.EduCourseBookingMapper;
import cn.byteo.springaidemo.edu.service.IEduCourseBookingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EduCourseBookingServiceImpl extends ServiceImpl<EduCourseBookingMapper, EduCourseBooking>
        implements IEduCourseBookingService {
}
