package com.campus.activity.module.signup.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.vo.RegistrationVO;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.ResultHandler;
import java.util.List;
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
    String SQL="""
        SELECT r.reg_id,r.name,r.phone,c.college_name,r.signup_time,k.checkin_time,k.method AS checkin_method
        FROM t_registration r LEFT JOIN t_college c ON c.college_code=r.college_code
        LEFT JOIN t_checkin k ON k.reg_id=r.reg_id
        WHERE r.activity_id=#{activityId} AND r.deleted=0 AND r.status='VALID'
        ORDER BY r.reg_id DESC
        """;
    @Select(SQL+" LIMIT 100") List<RegistrationVO> listWithCheckin(@Param("activityId") Long id);
    @Select(SQL) IPage<RegistrationVO> pageWithCheckin(Page<RegistrationVO> page,@Param("activityId") Long id);
    @Select(SQL) @Options(fetchSize=Integer.MIN_VALUE,resultSetType=org.apache.ibatis.mapping.ResultSetType.FORWARD_ONLY)
    @ResultType(RegistrationVO.class)
    void stream(@Param("activityId") Long id,ResultHandler<RegistrationVO> handler);
}
