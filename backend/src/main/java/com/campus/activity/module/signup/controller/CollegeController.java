package com.campus.activity.module.signup.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.signup.entity.College;
import com.campus.activity.module.signup.mapper.CollegeMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C岗：学院字典（报名表单下拉，对外开放）
 */
@RestController
@RequestMapping("/dict")

public class CollegeController {

    private final CollegeMapper collegeMapper;

    @GetMapping("/college")
    public R<List<College>> list() {
        return R.ok(collegeMapper.selectList(null));
    }

    public CollegeController(CollegeMapper collegeMapper) {
        this.collegeMapper = collegeMapper;
    }
}
