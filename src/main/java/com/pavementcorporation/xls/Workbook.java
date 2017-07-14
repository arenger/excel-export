package com.pavementcorporation.xls;

import com.pavementcorporation.xls.dto.Client;
import com.pavementcorporation.xls.dto.Project;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Workbook {
   private static final Logger LOG = LoggerFactory.getLogger(Workbook.class);

   private final Map<Integer, Client>  clients = new HashMap<>();
   private final Map<Integer, Project> projects = new HashMap<>();

   public Workbook(File xlsFile) throws IOException {
      try (FileInputStream in = new FileInputStream(xlsFile)) {
         XSSFWorkbook wb = new XSSFWorkbook(in);
         FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
         loadClients(wb.getSheet("Clients"));
         loadProjects(wb.getSheet("Projects"), evaluator);
      }
   }

   private String cellString(Cell c) {
      if (c == null) {
         return null;
      }
      String s;
      switch (c.getCellTypeEnum()) {
         case NUMERIC:
            s = String.valueOf((int)c.getNumericCellValue());
            break;
         case STRING:
            s = c.getStringCellValue();
            break;
         default:
            s = null;
            break;
      }
      return s;
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
         c.setName(cellString(r.getCell(1)));
         c.setAddress(cellString(r.getCell(2)));
         c.setCity(cellString(r.getCell(3)));
         c.setState(cellString(r.getCell(4)));
         c.setZip(cellString(r.getCell(5)));
         clients.put(c.getId(), c);
         System.out.println(c);
      }
   }

   private void loadProjects(XSSFSheet sheet, FormulaEvaluator evaluator) {
      Iterator<Row> i = sheet.rowIterator();
      boolean firstRow = true;
      while (i.hasNext()) {
         Row r = i.next();
         if (firstRow) {
            firstRow = false;
            continue;
         }
         if ((r.getCell(0) == null) || (r.getCell(2) == null)) {
            continue;
         }
         Project p = new Project(
            (int)r.getCell(2).getNumericCellValue(),
            (int)r.getCell(0).getNumericCellValue()
         );
         p.setName(cellString(r.getCell(3)));
         p.setAmount(cellString(r.getCell(4)));
         String yn = cellString(r.getCell(5));
         p.setInvoiced(yn != null && "Y".equalsIgnoreCase(yn));
         p.setManager(cellString(r.getCell(6)));
         p.setAccountManager(cellString(r.getCell(7)));
         p.setStartDate(cellString(r.getCell(9)));
         LOG.debug("startDate type: {}, formula: {} result: {}", r.getCell(9).getCellTypeEnum(),
            r.getCell(9).getCellFormula(), evaluator.evaluate(r.getCell(9)).formatAsString());
         p.setEndDate(cellString(r.getCell(10)));
         projects.put(p.getId(), p);
         System.out.println(p);
      }
   }
}
