package com.campus.activity.module.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.module.activity.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 名额控制专用：事务内锁住活动行，防止并发超卖（C岗报名时调用）。
     */
    @Select("SELECT * FROM t_activity WHERE activity_id = #{id} AND deleted = 0 FOR UPDATE")
    Activity selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM t_activity WHERE activity_id = #{id} AND deleted = 0 FOR SHARE")
    Activity selectForShare(@Param("id") Long id);

    @Update("UPDATE t_activity SET status = CASE WHEN end_time <= NOW() THEN 'FINISHED' " +
            "WHEN start_time <= NOW() THEN 'ONGOING' WHEN signup_deadline <= NOW() THEN 'CLOSED' ELSE 'OPEN' END, " +
            "update_time = NOW() WHERE deleted = 0 AND status IN ('OPEN','CLOSED','ONGOING') " +
            "AND status <> CASE WHEN end_time <= NOW() THEN 'FINISHED' WHEN start_time <= NOW() THEN 'ONGOING' " +
            "WHEN signup_deadline <= NOW() THEN 'CLOSED' ELSE 'OPEN' END")
    int refreshStatuses();

    /**
     * 名额增减：与报名/取消在同一事务内完成，保证 signed_count 与实际一致（风险 R6）。
     */
    @Update("UPDATE t_activity SET signed_count = signed_count + #{delta} " +
            "WHERE activity_id = #{id} AND deleted = 0 AND signed_count + #{delta} >= 0")
    int incrSignedCount(@Param("id") Long id, @Param("delta") int delta);
}
