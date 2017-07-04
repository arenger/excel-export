package com.pavementcorporation.xls;

import com.pavementcorporation.xls.dto.Client;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Workbook {
   private final Map<Integer, Client> clients = new HashMap<>();

   public Workbook(File xlsFile) throws IOException {
      try (FileInputStream in = new FileInputStream(xlsFile)) {
         HSSFWorkbook wb = new HSSFWorkbook(in);
         HSSFSheet sheet = wb.getSheet("Clients");
         Iterator<Row> i = sheet.rowIterator();
         while (i.hasNext()) {
            Row r = i.next();
            Client c = new Client((int)r.getCell(0).getNumericCellValue());
            c.setName(r.getCell(1).getStringCellValue());
            c.setAddress(r.getCell(2).getStringCellValue());
            c.setCity(r.getCell(3).getStringCellValue());
            c.setState(r.getCell(4).getStringCellValue());
            c.setZip(r.getCell(5).getStringCellValue());
            clients.put(c.getId(), c);
            System.out.println(c);
         }
      }
   }
}
