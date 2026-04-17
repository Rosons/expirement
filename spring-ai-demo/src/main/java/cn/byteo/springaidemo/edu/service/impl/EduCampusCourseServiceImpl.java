package cn.byteo.springaidemo.edu.service.impl;

import cn.byteo.springaidemo.edu.entity.EduCampusCourse;
import cn.byteo.springaidemo.edu.mapper.EduCampusCourseMapper;
import cn.byteo.springaidemo.edu.service.IEduCampusCourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EduCampusCourseServiceImpl extends ServiceImpl<EduCampusCourseMapper, EduCampusCourse>
        implements IEduCampusCourseService {
}
