package com.campus.activity.util;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ExcelUtilTest {
 @Test void streamingExportKeepsEveryRowAndTreatsFormulaLikeTextAsText()throws Exception{
  var response=new MockHttpServletResponse();ExcelUtil.exportStream(response,"签到名单",List.of("姓名","电话"),sink->{for(int i=0;i<5000;i++)sink.accept(List.of("=test"+i,"138****5678"));});
  try(var book=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))){assertEquals(5000,book.getSheetAt(0).getLastRowNum());assertEquals("=test4999",book.getSheetAt(0).getRow(5000).getCell(0).getStringCellValue());assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,book.getSheetAt(0).getRow(1).getCell(0).getCellType());}
  assertEquals("no-store",response.getHeader("Cache-Control"));
 }
}
