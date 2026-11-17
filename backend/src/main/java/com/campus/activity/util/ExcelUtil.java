package com.campus.activity.util;
import com.campus.activity.common.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
/** Bounded row window and bounded concurrent exports; temporary files are always disposed. */
public final class ExcelUtil {
    private static final Semaphore EXPORTS=new Semaphore(2,true);
    private ExcelUtil(){}
    public static void export(HttpServletResponse response,String name,List<String> headers,List<List<String>> rows)throws IOException {exportStream(response,name,headers,sink->rows.forEach(sink));}
    public static void exportStream(HttpServletResponse response,String name,List<String> headers,Consumer<Consumer<List<String>>> source)throws IOException {
        if(!EXPORTS.tryAcquire())throw new BusinessException(40029,"导出任务较多，请稍后重试");
        SXSSFWorkbook book=null;
        try {
            book=new SXSSFWorkbook(100);book.setCompressTempFiles(true);var sheet=book.createSheet("名单");var head=sheet.createRow(0);
            for(int i=0;i<headers.size();i++){head.createCell(i).setCellValue(headers.get(i));sheet.setColumnWidth(i,24*256);}
            int[] n={1};source.accept(values->{var row=sheet.createRow(n[0]++);for(int i=0;i<values.size();i++)row.createCell(i).setCellValue(values.get(i)==null?"":values.get(i));});
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Cache-Control","no-store");response.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+URLEncoder.encode(name,StandardCharsets.UTF_8)+".xlsx");book.write(response.getOutputStream());
        } finally {if(book!=null){try{book.close();}finally{book.dispose();}}EXPORTS.release();}
    }
}
