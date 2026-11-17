package com.campus.activity.tools;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
/** Standalone migration process; the runtime account needs only DML. Never auto-baselines an existing schema. */
public final class MigrationTool {
    private static final String[] FILES={"V001__baseline.sql","V002__enterprise_foundation.sql"};
    private static String env(String key,String fallback){return System.getenv().getOrDefault(key,fallback);}
    public static void main(String[] args)throws Exception{
        int baseline=0;for(String arg:args)if(arg.startsWith("--baseline-through="))baseline=Integer.parseInt(arg.substring(19));
        if(baseline<0||baseline>FILES.length)throw new IllegalArgumentException("Invalid baseline");
        String url=env("CAMPUS_MIGRATION_URL",env("SPRING_DATASOURCE_URL","jdbc:mysql://127.0.0.1:3306/campus_activity?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"));
        try(Connection connection=DriverManager.getConnection(url,env("CAMPUS_MIGRATION_USER","campus_migrator"),env("CAMPUS_MIGRATION_PASSWORD",""))){
            try(var lock=connection.createStatement();var rs=lock.executeQuery("SELECT GET_LOCK('campus-schema-migration',30)")){rs.next();if(rs.getInt(1)!=1)throw new IllegalStateException("Migration lock unavailable");}
            try{
                boolean existing=connection.getMetaData().getTables(connection.getCatalog(),null,"t_activity",null).next();
                boolean history=connection.getMetaData().getTables(connection.getCatalog(),null,"t_schema_history",null).next();
                if(existing&&!history&&baseline==0)throw new IllegalStateException("Existing schema requires explicit baseline after verification");
                if(baseline>0&&!existing)throw new IllegalStateException("Cannot baseline an empty schema");
                if(baseline>=1&&!connection.getMetaData().getColumns(connection.getCatalog(),null,"t_activity","approval_status").next())throw new IllegalStateException("Apply legacy platform/review migrations before baseline");
                if(baseline>=2&&!connection.getMetaData().getColumns(connection.getCatalog(),null,"t_activity","version").next())throw new IllegalStateException("Version 2 schema not present");
                try(var statement=connection.createStatement()){statement.execute("CREATE TABLE IF NOT EXISTS t_schema_history(version INT PRIMARY KEY,checksum CHAR(64) NOT NULL,state VARCHAR(20) NOT NULL,applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");}
                for(int i=0;i<FILES.length;i++){
                    var resource=new ClassPathResource("db/migrations/"+FILES[i]);String checksum;
                    try(var input=resource.getInputStream()){checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));}
                    try(var query=connection.prepareStatement("SELECT checksum,state FROM t_schema_history WHERE version=?")){query.setInt(1,i+1);try(var rs=query.executeQuery()){if(rs.next()){if(!checksum.equals(rs.getString(1))||!java.util.Set.of("APPLIED","BASELINED").contains(rs.getString(2)))throw new IllegalStateException("Migration checksum/state mismatch: "+FILES[i]);continue;}}}
                    try(var insert=connection.prepareStatement("INSERT INTO t_schema_history(version,checksum,state) VALUES (?,?,'STARTED')")){insert.setInt(1,i+1);insert.setString(2,checksum);insert.executeUpdate();}
                    try{if(i+1>baseline)ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.support.EncodedResource(resource,StandardCharsets.UTF_8));try(var update=connection.prepareStatement("UPDATE t_schema_history SET state=? WHERE version=?")){update.setString(1,i+1<=baseline?"BASELINED":"APPLIED");update.setInt(2,i+1);update.executeUpdate();}}
                    catch(Exception e){try(var update=connection.prepareStatement("UPDATE t_schema_history SET state='FAILED' WHERE version=?")){update.setInt(1,i+1);update.executeUpdate();}throw e;}
                    System.out.println((i+1<=baseline?"BASELINED ":"APPLIED ")+FILES[i]);
                }
            }finally{try(var statement=connection.createStatement()){statement.execute("SELECT RELEASE_LOCK('campus-schema-migration')");}}
        }
    }
}
