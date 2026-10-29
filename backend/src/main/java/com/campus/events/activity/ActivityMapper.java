package com.campus.events.activity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface ActivityMapper {

    ActivityRow findById(@Param("id") String id);

    ActivityRow lockById(@Param("id") String id);

    List<ActivityRow> search(ActivityQuery query);

    long countSearch(ActivityQuery query);

    StatusCountRow countStatus(@Param("now") OffsetDateTime now);

    List<ActivityRow> recent(@Param("now") OffsetDateTime now);

    @Update("""
            INSERT INTO activity (
                id, type_id, name, description, location, start_at, end_at, registration_deadline,
                capacity, publication_state, published_at, live_url, cover_storage_key, cover_content_type,
                created_by, version, created_at, updated_at, deleted_at
            ) VALUES (
                #{id}, #{typeId}, #{name}, #{description}, #{location}, #{startAt}, #{endAt}, #{registrationDeadline},
                #{capacity}, #{publicationState}, #{publishedAt}, #{liveUrl}, #{coverStorageKey}, #{coverContentType},
                #{createdBy}, #{version}, #{createdAt}, #{updatedAt}, #{deletedAt}
            )
            """)
    int insert(ActivityRow row);

    @Update("""
            UPDATE activity
            SET type_id = #{typeId},
                name = #{name},
                description = #{description},
                location = #{location},
                start_at = #{startAt},
                end_at = #{endAt},
                registration_deadline = #{registrationDeadline},
                capacity = #{capacity},
                live_url = #{liveUrl},
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted_at IS NULL
            """)
    int updateContent(ActivityRow row);

    @Update("""
            UPDATE activity
            SET publication_state = 'PUBLISHED',
                published_at = COALESCE(published_at, #{publishedAt}),
                updated_at = #{publishedAt},
                version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted_at IS NULL AND publication_state = 'DRAFT'
            """)
    int publishIfDraft(@Param("id") String id, @Param("publishedAt") OffsetDateTime publishedAt, @Param("version") int version);

    @Update("""
            UPDATE activity
            SET deleted_at = #{deletedAt},
                updated_at = #{deletedAt},
                version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted_at IS NULL
            """)
    int markDeleted(@Param("id") String id, @Param("deletedAt") OffsetDateTime deletedAt, @Param("version") int version);

    @Update("""
            UPDATE activity
            SET cover_storage_key = #{key},
                cover_content_type = #{contentType},
                updated_at = #{updatedAt}
            WHERE id = #{id} AND deleted_at IS NULL
            """)
    int updateCover(
            @Param("id") String id,
            @Param("key") String key,
            @Param("contentType") String contentType,
            @Param("updatedAt") OffsetDateTime updatedAt);
}
