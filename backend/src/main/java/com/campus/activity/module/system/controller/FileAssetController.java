package com.campus.activity.module.system.controller;
import com.campus.activity.module.system.service.FileAssetService;
import com.campus.activity.common.R;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.InputStreamResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
@RestController
public class FileAssetController {
    private final FileAssetService assets;
    public FileAssetController(FileAssetService assets){this.assets=assets;}
    @GetMapping("/file-assets/{id}") public ResponseEntity<InputStreamResource> privateFile(@PathVariable String id)throws IOException{return serve(id,false);}
    @GetMapping("/file-assets/public/{id}") public ResponseEntity<InputStreamResource> publicFile(@PathVariable String id)throws IOException{return serve(id,true);}
    private ResponseEntity<InputStreamResource> serve(String id,boolean publicOnly)throws IOException{var row=assets.metadata(id,publicOnly);String name=String.valueOf(row.get("original_name"));return ResponseEntity.ok().contentType(MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM)).header("Cache-Control","no-store").header("Content-Disposition",(publicOnly&&name.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif)$")?ContentDisposition.inline():ContentDisposition.attachment()).filename(name,StandardCharsets.UTF_8).build().toString()).body(new InputStreamResource(assets.open(String.valueOf(row.get("storage_key")))));}
    @PostMapping("/super/file-assets/cleanup") public R<java.util.Map<String,Integer>> cleanup(){return R.ok(java.util.Map.of("removed",assets.cleanup()));}
}
