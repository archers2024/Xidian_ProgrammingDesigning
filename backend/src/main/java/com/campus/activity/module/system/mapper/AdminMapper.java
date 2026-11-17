package com.campus.activity.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.module.system.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    @org.apache.ibatis.annotations.Select("SELECT a.*,o.name AS org_name FROM t_admin a LEFT JOIN t_organization o ON o.org_id=a.org_id WHERE (#{keyword}='' OR a.username LIKE CONCAT('%',#{keyword},'%') OR a.real_name LIKE CONCAT('%',#{keyword},'%')) ORDER BY a.admin_id DESC")
    com.baomidou.mybatisplus.core.metadata.IPage<Admin> pageWithOrg(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admin> page,@org.apache.ibatis.annotations.Param("keyword")String keyword);

}
