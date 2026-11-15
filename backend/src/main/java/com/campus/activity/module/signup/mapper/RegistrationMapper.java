package com.campus.activity.module.signup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.vo.RegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    /**
     * 名单查询：左连接签到表，输出已签到 / 未签到（FR-10）。
     * 说明：签到表 t_checkin 属 D 岗，此处只读，不写入。
     */
    @Select("SELECT r.reg_id, r.name, r.phone, c.college_name AS college_name, r.signup_time, " +
            "       k.checkin_time, k.method AS checkin_method " +
            "FROM t_registration r " +
            "LEFT JOIN t_college c ON c.college_code = r.college_code " +
            "LEFT JOIN t_checkin k ON k.reg_id = r.reg_id " +
            "WHERE r.activity_id = #{activityId} AND r.deleted = 0 AND r.status = 'VALID' " +
            "ORDER BY r.signup_time DESC")
    List<RegistrationVO> listWithCheckin(@Param("activityId") Long activityId);
}
