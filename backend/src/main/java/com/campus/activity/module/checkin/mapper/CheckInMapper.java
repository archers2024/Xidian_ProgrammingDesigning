package com.campus.activity.module.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.module.checkin.entity.CheckIn;
import com.campus.activity.module.checkin.vo.CheckinVO;
import com.campus.activity.module.checkin.vo.DailyStatVO;
import com.campus.activity.module.checkin.vo.TypeStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {

    @Select("SELECT k.checkin_id, k.reg_id, r.name, r.phone, r.college_code AS college_code, " +
            "       k.checkin_time, k.method " +
            "FROM t_checkin k JOIN t_registration r ON r.reg_id = k.reg_id " +
            "WHERE k.activity_id = #{activityId} ORDER BY k.checkin_time DESC")
    List<CheckinVO> listByActivity(@Param("activityId") Long activityId);

    /** 按活动类型统计报名与签到（FR-11） */
    @Select("SELECT a.type_code AS type_code, COUNT(DISTINCT r.reg_id) AS signup_count, " +
            "       COUNT(DISTINCT k.checkin_id) AS checkin_count " +
            "FROM t_activity a " +
            "LEFT JOIN t_registration r ON r.activity_id = a.activity_id AND r.deleted = 0 " +
            "LEFT JOIN t_checkin k ON k.reg_id = r.reg_id " +
            "WHERE a.deleted = 0 AND a.status <> 'CANCELED' " +
            "  AND a.create_time >= #{from} AND a.create_time <= #{to} " +
            "GROUP BY a.type_code")
    List<TypeStatVO> statByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 按日报名趋势（FR-11） */
    @Select("SELECT DATE_FORMAT(r.signup_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS cnt " +
            "FROM t_registration r WHERE r.deleted = 0 " +
            "  AND r.signup_time >= #{from} AND r.signup_time <= #{to} " +
            "GROUP BY DATE_FORMAT(r.signup_time, '%Y-%m-%d') ORDER BY stat_date")
    List<DailyStatVO> statDaily(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
