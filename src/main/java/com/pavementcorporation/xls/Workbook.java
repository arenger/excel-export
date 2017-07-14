package com.pavementcorporation.xls;

import com.pavementcorporation.xls.dto.Client;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
         XSSFWorkbook wb = new XSSFWorkbook(in);
         loadClients(wb.getSheet("Clients"));
      }
   }

   private String cellStringNvl(Cell c) {
      if (c == null) {
         return null;
      }
      if (c.getCellTypeEnum() == CellType.NUMERIC) {
         return String.valueOf((int)c.getNumericCellValue());
      } else {
         return c.getStringCellValue();
      }
   }

   private void loadClients(XSSFSheet sheet) {
      Iterator<Row> i = sheet.rowIterator();
      boolean firstRow = true;
      while (i.hasNext()) {
         Row r = i.next();
         if (firstRow) {
            firstRow = false;
            continue;
         }
         if ((r.getCell(0) == null) || (r.getCell(1) == null)) {
            continue;
         }
         Client c = new Client((int)r.getCell(0).getNumericCellValue());
         c.setName(cellStringNvl(r.getCell(1)));
         c.setAddress(cellStringNvl(r.getCell(2)));
         c.setCity(cellStringNvl(r.getCell(3)));
         c.setState(cellStringNvl(r.getCell(4)));
         c.setZip(cellStringNvl(r.getCell(5)));
         clients.put(c.getId(), c);
         System.out.println(c);
      }
   }
}
