package com.campus.activity.module.system.storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalFileStorageTest {
 @TempDir Path folder;
 @Test void writesReadsAndDeletesOnlyWithinStorageRoot()throws Exception{var storage=new LocalFileStorage(folder.toString());storage.put("assets/test.pdf",new ByteArrayInputStream(new byte[]{1,2,3}),3);try(var input=storage.open("assets/test.pdf")){assertArrayEquals(new byte[]{1,2,3},input.readAllBytes());}storage.delete("assets/test.pdf");assertThrows(IOException.class,()->storage.open("assets/test.pdf"));assertThrows(IOException.class,()->storage.put("../escape.pdf",new ByteArrayInputStream(new byte[0]),0));}
}
