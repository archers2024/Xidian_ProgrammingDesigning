package com.campus.events.checkin;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CheckinMapper {

    @Select("""
            SELECT id, activity_id, session_mode AS mode, window_start, window_end, token_version, enabled, created_at
            FROM checkin_session
            WHERE id = #{id}
            """)
    CheckinSessionRow findById(String id);

    @Select("""
            SELECT id, activity_id, session_mode AS mode, window_start, window_end, token_version, enabled, created_at
            FROM checkin_session
            WHERE id = #{id}
            FOR UPDATE
            """)
    CheckinSessionRow lockById(String id);

    @Select("""
            SELECT id, activity_id, session_mode AS mode, window_start, window_end, token_version, enabled, created_at
            FROM checkin_session
            WHERE activity_id = #{activityId}
            ORDER BY enabled DESC, created_at DESC
            FETCH FIRST 1 ROW ONLY
            """)
    CheckinSessionRow latest(String activityId);

    @Insert("""
            INSERT INTO checkin_session (
                id, activity_id, session_mode, window_start, window_end, token_version, enabled, created_at
            ) VALUES (
                #{id}, #{activityId}, #{mode}, #{windowStart}, #{windowEnd}, #{tokenVersion}, #{enabled}, #{createdAt}
            )
            """)
    int insert(CheckinSessionRow row);

    @Update("""
            UPDATE checkin_session
            SET enabled = FALSE, token_version = token_version + 1
            WHERE activity_id = #{activityId} AND enabled = TRUE
            """)
    int disableEnabled(String activityId);

    @Update("""
            UPDATE checkin_session
            SET enabled = FALSE, token_version = token_version + 1
            WHERE id = #{id} AND enabled = TRUE
            """)
    int close(String id);

    @Select("""
            SELECT registration_id, session_id, checked_at
            FROM checkin_record
            WHERE registration_id = #{registrationId}
            """)
    CheckinRecordRow findRecord(String registrationId);

    @Insert("""
            INSERT INTO checkin_record (registration_id, session_id, checked_at)
            VALUES (#{registrationId}, #{sessionId}, #{checkedAt})
            """)
    int insertRecord(CheckinRecordRow row);

    @Select("""
            SELECT COUNT(*)
            FROM checkin_record cr
            JOIN registration r ON r.id = cr.registration_id
            WHERE r.activity_id = #{activityId}
            """)
    int countByActivity(String activityId);

    @Select("SELECT COUNT(*) FROM checkin_record")
    long countAll();
}
