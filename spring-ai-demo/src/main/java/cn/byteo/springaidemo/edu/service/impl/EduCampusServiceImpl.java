package cn.byteo.springaidemo.edu.service.impl;

import cn.byteo.springaidemo.edu.entity.EduCampus;
import cn.byteo.springaidemo.edu.mapper.EduCampusMapper;
import cn.byteo.springaidemo.edu.service.IEduCampusService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EduCampusServiceImpl extends ServiceImpl<EduCampusMapper, EduCampus> implements IEduCampusService {
}
