package com.campus.events.dictionary;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictionaryMapper {

    @Select("""
            SELECT id, name FROM activity_type WHERE enabled = TRUE ORDER BY sort_order, name
            """)
    List<DictionaryItem> listTypes();

    @Select("""
            SELECT id, name FROM college WHERE enabled = TRUE ORDER BY sort_order, name
            """)
    List<DictionaryItem> listColleges();

    @Select("SELECT COUNT(*) FROM activity_type WHERE id = #{id} AND enabled = TRUE")
    int countType(String id);

    @Select("SELECT COUNT(*) FROM college WHERE id = #{id} AND enabled = TRUE")
    int countCollege(String id);

    @Insert("""
            INSERT INTO activity_type (id, name, sort_order, enabled)
            VALUES (#{id}, #{name}, #{sortOrder}, #{enabled})
            """)
    int insertType(DictionaryItem item);

    @Insert("""
            INSERT INTO college (id, name, sort_order, enabled)
            VALUES (#{id}, #{name}, #{sortOrder}, #{enabled})
            """)
    int insertCollege(DictionaryItem item);
}
