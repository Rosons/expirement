package cn.byteo.springaidemo.edu.vo;

import cn.byteo.springaidemo.common.enums.EnableStatus;
import lombok.Data;

/**
 * 线下授课校区，对应表 {@code edu_campus}。
 */
@Data
public class EduCampusVO {

    private Long id;

    private String campusCode;

    private String name;

    private String province;

    private String city;

    private String district;

    private String addressDetail;

    private String contactPhone;

    private EnableStatus status;
}
