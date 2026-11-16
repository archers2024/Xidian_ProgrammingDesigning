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

    /**
     * 按活动类型统计报名与签到（FR-11）。
     * 入参是调用方筛出的「有效活动」集合，保证分类统计与总量口径完全一致。
     */
    @Select("<script>SELECT a.type_code AS type_code, COUNT(DISTINCT r.reg_id) AS signup_count, " +
            "       COUNT(DISTINCT k.checkin_id) AS checkin_count " +
            "FROM t_activity a " +
            "LEFT JOIN t_registration r ON r.activity_id = a.activity_id AND r.deleted = 0 " +
            "LEFT JOIN t_checkin k ON k.reg_id = r.reg_id " +
            "WHERE a.activity_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY a.type_code</script>")
    List<TypeStatVO> statByType(@Param("ids") List<Long> ids);

    /** 按日报名趋势（FR-11）。同样限定在有效活动集合内，避免趋势与汇总对不上。 */
    @Select("<script>SELECT DATE_FORMAT(r.signup_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS cnt " +
            "FROM t_registration r WHERE r.deleted = 0 " +
            "  AND r.activity_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "  AND r.signup_time &gt;= #{from} AND r.signup_time &lt;= #{to} " +
            "GROUP BY DATE_FORMAT(r.signup_time, '%Y-%m-%d') ORDER BY stat_date</script>")
    List<DailyStatVO> statDaily(@Param("ids") List<Long> ids,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
