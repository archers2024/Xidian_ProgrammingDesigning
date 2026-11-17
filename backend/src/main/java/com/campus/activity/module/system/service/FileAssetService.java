package com.campus.activity.module.system.service;
import com.campus.activity.common.BusinessException;
import com.campus.activity.security.*;
import com.campus.activity.module.system.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.io.*;
@Service
public class FileAssetService {
    private final FileStorage storage;private final JdbcTemplate jdbc;private final OrganizationAccess access;private final org.springframework.transaction.support.TransactionTemplate cleanupTransaction;
    @Value("${campus.file.quota-bytes:536870912}") private long quota;
    public FileAssetService(FileStorage storage,JdbcTemplate jdbc,OrganizationAccess access,org.springframework.transaction.PlatformTransactionManager manager){this.storage=storage;this.jdbc=jdbc;this.access=access;cleanupTransaction=new org.springframework.transaction.support.TransactionTemplate(manager);cleanupTransaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);}
    @Transactional public Map<String,String> upload(org.springframework.web.multipart.MultipartFile file,String ext,String visibility,Long requestedOrg)throws IOException {
        var admin=access.current();Long orgId=access.resolveForCreate(requestedOrg);
        if(!Set.of("PUBLIC","PRIVATE").contains(visibility))throw new BusinessException("附件访问范围不正确");
        com.campus.activity.util.InputRules.text(file.getOriginalFilename(),200,true,"文件名");
        jdbc.queryForObject("SELECT org_id FROM t_organization WHERE org_id=? FOR UPDATE",Long.class,orgId);
        Long used=jdbc.queryForObject("SELECT COALESCE(SUM(file_size),0) FROM t_file_asset WHERE org_id=?",Long.class,orgId);
        if(used+file.getSize()>quota)throw new BusinessException("组织文件空间已满，请清理未使用文件");
        String id=UUID.randomUUID().toString(),key="assets/"+id+"."+ext;
        try(var input=file.getInputStream()){storage.put(key,input,file.getSize());}
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCompletion(int status){if(status==STATUS_ROLLED_BACK)try{storage.delete(key);}catch(IOException e){org.slf4j.LoggerFactory.getLogger(FileAssetService.class).warn("清理回滚上传文件失败: {}",id,e);}}});
        try{jdbc.update("INSERT INTO t_file_asset(asset_id,org_id,owner_id,storage_key,original_name,visibility,file_size) VALUES (?,?,?,?,?,?,?)",id,orgId,admin.getAdminId(),key,file.getOriginalFilename(),visibility,file.getSize());}
        catch(RuntimeException e){storage.delete(key);throw e;}
        String url="/api/v1/file-assets/"+("PUBLIC".equals(visibility)?"public/":"")+id;
        return Map.of("url",url,"fileName",file.getOriginalFilename(),"fileSize",String.valueOf(file.getSize()),"visibility",visibility);
    }
    public Map<String,Object> metadata(String id,boolean publicOnly){var rows=jdbc.queryForList("SELECT * FROM t_file_asset WHERE asset_id=?",id);if(rows.isEmpty())throw new BusinessException(40003,"文件不存在");var row=rows.get(0);if("DELETING".equals(row.get("state")))throw new BusinessException(40003,"文件不存在");if(publicOnly){if(!"PUBLIC".equals(row.get("visibility")))throw new BusinessException(40003,"文件不存在");}else access.requireScope(((Number)row.get("org_id")).longValue());return row;}
    public InputStream open(String key)throws IOException{return storage.open(key);}
    public void link(String url,Long orgId){
        if(url==null||!url.startsWith("/api/v1/file-assets/"))return;
        String id=url.substring(url.lastIndexOf('/')+1);var rows=jdbc.queryForList("SELECT org_id,visibility,state FROM t_file_asset WHERE asset_id=? FOR UPDATE",id);
        if(rows.isEmpty())throw new BusinessException("附件已失效，请重新上传");var row=rows.get(0);if("DELETING".equals(row.get("state")))throw new BusinessException("附件正在清理，请重新上传");
        if(!"PUBLIC".equals(row.get("visibility"))&&!orgId.equals(((Number)row.get("org_id")).longValue()))throw new BusinessException(40002,"不能关联其他组织的私有附件");
        jdbc.update("UPDATE t_file_asset SET state='LINKED' WHERE asset_id=?",id);
    }
    private static final String URL="CONCAT('/api/v1/file-assets/',IF(f.visibility='PUBLIC','public/',''),f.asset_id)";
    private static final String UNREFERENCED="NOT EXISTS(SELECT 1 FROM t_activity a WHERE a.cover_url="+URL+") AND NOT EXISTS(SELECT 1 FROM t_attachment a WHERE a.file_url="+URL+") AND NOT EXISTS(SELECT 1 FROM t_activity_review r WHERE JSON_SEARCH(IF(JSON_VALID(r.content_snapshot),r.content_snapshot,'{}'),'one',"+URL+") IS NOT NULL)";
    /** Commit DELETING before touching storage; a failed object deletion remains retryable and cannot be linked. */
    public int cleanup(){
        access.requireSuper();
        var candidates=cleanupTransaction.execute(status->{
            var rows=jdbc.queryForList("SELECT f.* FROM t_file_asset f WHERE f.create_time<DATE_SUB(NOW(),INTERVAL 2 DAY) AND (f.state='DELETING' OR ("+UNREFERENCED+")) ORDER BY f.create_time LIMIT 100 FOR UPDATE SKIP LOCKED");
            var claimed=new java.util.ArrayList<Map<String,Object>>();
            for(var row:rows){String id=String.valueOf(row.get("asset_id"));
                Long unreferenced=jdbc.queryForObject("SELECT COUNT(*) FROM t_file_asset f WHERE f.asset_id=? AND ("+UNREFERENCED+")",Long.class,id);
                if(unreferenced!=null&&unreferenced==1){jdbc.update("UPDATE t_file_asset SET state='DELETING' WHERE asset_id=?",id);claimed.add(row);}
            }return claimed;
        });
        int removed=0;
        if(candidates!=null)for(var row:candidates){try{storage.delete(String.valueOf(row.get("storage_key")));jdbc.update("DELETE FROM t_file_asset WHERE asset_id=? AND state='DELETING'",row.get("asset_id"));removed++;}catch(IOException e){org.slf4j.LoggerFactory.getLogger(FileAssetService.class).warn("文件清理暂未完成，将在下次请求重试",e);}}
        return removed;
    }
}
