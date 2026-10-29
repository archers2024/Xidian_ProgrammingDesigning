package com.campus.events.attachment;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    @Select("""
            SELECT id, activity_id, original_name, storage_key, content_type, size_bytes, created_at
            FROM attachment
            WHERE activity_id = #{activityId}
            ORDER BY created_at, id
            """)
    List<AttachmentRow> listByActivity(String activityId);

    @Select("""
            SELECT id, activity_id, original_name, storage_key, content_type, size_bytes, created_at
            FROM attachment
            WHERE id = #{id}
            """)
    AttachmentRow findById(String id);

    @Insert("""
            INSERT INTO attachment (id, activity_id, original_name, storage_key, content_type, size_bytes, created_at)
            VALUES (#{id}, #{activityId}, #{originalName}, #{storageKey}, #{contentType}, #{sizeBytes}, #{createdAt})
            """)
    int insert(AttachmentRow row);

    @Delete("DELETE FROM attachment WHERE id = #{id}")
    int deleteById(String id);
}
