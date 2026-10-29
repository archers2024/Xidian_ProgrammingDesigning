package com.campus.events.registration;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegistrationMapper {

    @Select("""
            SELECT id, activity_id, participant_name, phone, college_id, created_at
            FROM registration
            WHERE activity_id = #{activityId} AND phone = #{phone}
            """)
    RegistrationRow findByActivityAndPhone(@Param("activityId") String activityId, @Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM registration WHERE activity_id = #{activityId}")
    int countByActivity(String activityId);

    @Select("SELECT COUNT(*) FROM registration")
    long countAll();

    @Insert("""
            INSERT INTO registration (id, activity_id, participant_name, phone, college_id, created_at)
            VALUES (#{id}, #{activityId}, #{participantName}, #{phone}, #{collegeId}, #{createdAt})
            """)
    int insert(RegistrationRow row);

    List<RegistrationSummaryRow> listByActivity(@Param("activityId") String activityId, @Param("limit") int limit, @Param("offset") int offset);

    long countByActivityId(@Param("activityId") String activityId);
}
