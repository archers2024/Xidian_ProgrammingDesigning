package com.campus.activity.module.system.controller;
@org.springframework.web.bind.annotation.RestController
public class IdentityController {
 private final com.campus.activity.security.IdentityVerifier identity;
 public IdentityController(com.campus.activity.security.IdentityVerifier identity){this.identity=identity;}
 @org.springframework.web.bind.annotation.GetMapping("/identity/config")
 public com.campus.activity.common.R<java.util.Map<String,Object>> config(){return com.campus.activity.common.R.ok(identity.config());}
}
