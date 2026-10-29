package com.campus.events.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper {

    @Select("""
            SELECT id, username, password_hash, display_name, enabled, created_at
            FROM admin_account
            WHERE username = #{username}
            """)
    AdminAccount findByUsername(String username);

    @Select("SELECT COUNT(*) FROM admin_account")
    int count();

    @Insert("""
            INSERT INTO admin_account (id, username, password_hash, display_name, enabled, created_at)
            VALUES (#{id}, #{username}, #{passwordHash}, #{displayName}, #{enabled}, #{createdAt})
            """)
    int insert(AdminAccount account);
}
