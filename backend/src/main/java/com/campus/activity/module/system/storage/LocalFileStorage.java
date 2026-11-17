package com.campus.activity.module.system.storage;
import java.io.*;
import java.nio.file.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="campus.file.storage",havingValue="local",matchIfMissing=true)
public class LocalFileStorage implements FileStorage {
    private final Path root;
    public LocalFileStorage(@Value("${campus.file.upload-dir}")String path){root=Path.of(path).toAbsolutePath().normalize();}
    private Path path(String key)throws IOException{Path p=root.resolve(key).normalize();if(!p.startsWith(root))throw new IOException("Invalid storage key");return p;}
    public void put(String key,InputStream input,long size)throws IOException {Path p=path(key);Files.createDirectories(p.getParent());Files.copy(input,p);}
    public InputStream open(String key)throws IOException{return Files.newInputStream(path(key));}
    public void delete(String key)throws IOException{Files.deleteIfExists(path(key));}
}
