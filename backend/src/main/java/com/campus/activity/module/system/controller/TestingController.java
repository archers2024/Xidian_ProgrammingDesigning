package com.campus.activity.module.system.controller;
@org.springframework.web.bind.annotation.RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="campus.testing.enabled",havingValue="true")
public class TestingController {
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 public TestingController(org.springframework.jdbc.core.JdbcTemplate jdbc){this.jdbc=jdbc;}
 @org.springframework.web.bind.annotation.GetMapping("/testing/identity")
 public com.campus.activity.common.R<java.util.Map<String,Object>> identity(){String schema=jdbc.queryForObject("SELECT DATABASE()",String.class);return com.campus.activity.common.R.ok(java.util.Map.of("testing",schema!=null&&schema.startsWith("campus_test_"),"schema",schema));}
}
