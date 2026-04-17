package cn.byteo.springaidemo.edu.service.impl;

import cn.byteo.springaidemo.edu.entity.EduCourse;
import cn.byteo.springaidemo.edu.mapper.EduCourseMapper;
import cn.byteo.springaidemo.edu.service.IEduCourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EduCourseServiceImpl extends ServiceImpl<EduCourseMapper, EduCourse> implements IEduCourseService {
}
