package com.campus.activity.module.system.storage;
import java.io.*;
public interface FileStorage {
    void put(String key,InputStream input,long size)throws IOException;
    InputStream open(String key)throws IOException;
    void delete(String key)throws IOException;
}
