package com.campus.activity.module.signup.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


/**
 * 学院字典 t_college（Owner：C岗）
 */

@TableName("t_college")
public class College {

    @TableId
    private String collegeCode;

    private String collegeName;

    private Integer sortOrder;

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
