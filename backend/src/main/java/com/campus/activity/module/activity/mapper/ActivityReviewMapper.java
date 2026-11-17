package com.campus.activity.module.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.module.activity.entity.ActivityReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityReviewMapper extends BaseMapper<ActivityReview> {
    @Select("SELECT * FROM t_activity_review WHERE review_id=#{id} FOR UPDATE")
    ActivityReview selectForUpdate(@Param("id") Long id);

    @Select("""
            SELECT r.*, COALESCE(r.title_snapshot,a.title) AS activity_title, o.name AS org_name, u.real_name AS submitter_name
            FROM t_activity_review r
            JOIN t_activity a ON a.activity_id=r.activity_id
            JOIN t_organization o ON o.org_id=r.org_id
            LEFT JOIN t_admin u ON u.admin_id=r.submitted_by
            WHERE (r.decision<>'PENDING' OR (a.deleted=0 AND o.status=1))
              AND (#{orgId} IS NULL OR r.org_id=#{orgId})
              AND (#{decision} IS NULL OR #{decision}='' OR r.decision=#{decision})
            ORDER BY r.review_id DESC
            """)
    IPage<ActivityReview> queue(Page<ActivityReview> page, @Param("orgId") Long orgId,
                               @Param("decision") String decision);
}
