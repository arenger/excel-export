package com.pavementcorporation.xls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;

@Singleton
public class Writer {
   private static final Logger LOG = LoggerFactory.getLogger(Writer.class);

   private SqlSessionProvider sqlSessionProvider;

   @Inject
   public Writer(SqlSessionProvider sqlSessionProvider) {
      this.sqlSessionProvider = sqlSessionProvider;
   }

   public void run() throws SQLException, IOException {

      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

      String workbookName = String.format("Project Dashboard %s.xlsx", formatter.print(LocalDate.now()));
      LOG.info("creating {}", workbookName);
      System.out.println("Creating " + workbookName);

      XSSFWorkbook workbook = new XSSFWorkbook();
      StylesTable stylesTable = workbook.getStylesSource();

      createPlanningSheets(workbook, stylesTable);
      createProjInProgress(workbook, stylesTable);
      createProjectBilling(workbook, stylesTable);

      try (FileOutputStream out = new FileOutputStream(workbookName)) {
         workbook.write(out);
         workbook.close();
      }

      LOG.info("complete");
      System.out.println("Done");
   }

   private void createProjectBilling(XSSFWorkbook workbook, StylesTable stylesTable) throws SQLException {
      String query =
         "select p.manager \"Manager\", p.name \"Project\", p.id \"ID\", p.amount \"Amount\", " +
            "sum(t.hours) \"Hours\", min(t.schedule_date) \"Start Date\", max(schedule_date) \"End Date\" " +
            "from project p join task t on p.id = t.project_id where invoiced = false " +
            "group by p.id having max(schedule_date) between ? and ? order by amount desc";
      LOG.debug("query: {}", query);
      try (SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
         PreparedStatement ps = conn.prepareStatement(query);

         LocalDate start = LocalDate.now();
         start = start.minusDays(start.getDayOfMonth() - 1);
         LocalDate end = start.plusMonths(1);
         end = end.minusDays(end.getDayOfMonth());

         DateTimeFormatter formatter = DateTimeFormat.forPattern("MMMM");
         final String title = String.format("%s Billing", formatter.print(LocalDate.now()));
         System.out.printf("Adding %s\n", title);

         LOG.debug("Adding {}, range: {} - {}", title, start, end);
         XSSFSheet sheet = workbook.createSheet(title);

         ps.setDate(1, new Date(start.toDate().getTime()));
         ps.setDate(2, new Date(end.toDate().getTime()));
         ResultSet rs = ps.executeQuery();
         boolean firstRow = true;
         int rowNum = 0;
         int colTotal = 0;
         while (rs.next()) {
            if (firstRow) {
               colTotal = addHeaderRow(rs, sheet.createRow(rowNum++), stylesTable);
               firstRow = false;
            }
            Row row = sheet.createRow(rowNum++);
            for (int col = 0; col < colTotal; col++) {
               Cell cell = row.createCell(col);
               CellStyle style = new XSSFCellStyle(stylesTable);
               cell.setCellStyle(style);
               buildProjProgCell(col, cell, rs);
            }
         }
         for (int col = 0; col < colTotal; col++) {
            sheet.autoSizeColumn(col);
         }
      }
   }

   private void createProjInProgress(XSSFWorkbook workbook, StylesTable stylesTable) throws SQLException {
      String query =
         "select p.manager \"Manager\", p.name \"Project\", p.id \"ID\", p.amount \"Amount\", " +
            "sum(t.hours) \"Hours\", min(t.schedule_date) \"Start Date\", max(schedule_date) \"End Date\" " +
            "from project p join task t on p.id = t.project_id where invoiced = false " +
            "group by p.id having min(schedule_date) < current_date order by min(schedule_date) asc, p.manager";
      LOG.debug("query: {}", query);
      try (SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
         PreparedStatement ps = conn.prepareStatement(query);
         final String title = "Projects In-Progress";
         System.out.printf("Adding %s\n", title);

         LOG.debug("Adding {}", title);
         XSSFSheet sheet = workbook.createSheet(title);

         ResultSet rs = ps.executeQuery();
         boolean firstRow = true;
         int rowNum = 0;
         int colTotal = 0;
         while (rs.next()) {
            if (firstRow) {
               colTotal = addHeaderRow(rs, sheet.createRow(rowNum++), stylesTable);
               firstRow = false;
            }
            Row row = sheet.createRow(rowNum++);
            for (int col = 0; col < colTotal; col++) {
               Cell cell = row.createCell(col);
               CellStyle style = new XSSFCellStyle(stylesTable);
               cell.setCellStyle(style);
               buildProjProgCell(col, cell, rs);
            }
         }
         for (int col = 0; col < colTotal; col++) {
            sheet.autoSizeColumn(col);
         }
      }
   }

   private void buildProjProgCell(int col, Cell cell, ResultSet rs) throws SQLException {
      CellStyle style = cell.getCellStyle();
      switch (col) {
         case 2:
         case 4:
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(rs.getInt(col + 1));
            style.setDataFormat((short)1);
            break;

         case 3:
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(rs.getInt(col + 1));
            style.setDataFormat((short)5);
            break;

         default:
            cell.setCellType(CellType.STRING);
            cell.setCellValue(rs.getString(col + 1));
            break;
      }
   }

   private int addHeaderRow(ResultSet rs, Row row, StylesTable stylesTable) throws SQLException {
      ResultSetMetaData md = rs.getMetaData();
      int colTotal = md.getColumnCount();
      for (int col = 0; col < colTotal; col++) {
         Cell cell = row.createCell(col);
         cell.setCellValue(md.getColumnLabel(col + 1));
         CellStyle style = new XSSFCellStyle(stylesTable);
         style.setBorderBottom(BorderStyle.DOUBLE);
         cell.setCellStyle(style);
      }
      return colTotal;
   }

   private void createPlanningSheets(XSSFWorkbook workbook, StylesTable stylesTable) throws SQLException {
      String query = "select p.manager \"Manager\", p.name \"Project\", p.id \"ID\", t.service \"Service\", " +
         "t.schedule_date \"Schedule Date\", t.hours \"Hours\", p.amount \"Amount\" from task t " +
         "join project p on t.project_id = p.id where schedule_date between ? and ? " +
         "order by p.manager, t.schedule_date, t.id";

      try (SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
         PreparedStatement ps = conn.prepareStatement(query);
         LocalDate start = LocalDate.now();
         start = start.minusDays(start.getDayOfWeek());
         LocalDate end = start.plusDays(6);

         addPlanningSheet(ps, workbook, stylesTable, start, end, "Plan-ThisWeek");
         start = start.plusDays(7);
         end   = end.plusDays(7);
         addPlanningSheet(ps, workbook, stylesTable, start, end, "Plan-NextWeek");
         start = start.plusDays(7);
         end   = end.plusDays(7);
         addPlanningSheet(ps, workbook, stylesTable, start, end, "Plan-3WksOut");
      }
   }

   private void addPlanningSheet(PreparedStatement ps, XSSFWorkbook workbook, StylesTable stylesTable,
                                 LocalDate start, LocalDate end, String name) throws SQLException {
      System.out.printf("Adding %s\n", name);
      LOG.debug("Adding {}, range: {} - {}", name, start, end);
      ps.setDate(1, new Date(start.toDate().getTime()));
      ps.setDate(2, new Date(end.toDate().getTime()));
      XSSFSheet sheet = workbook.createSheet(name);

      ColorWheel cw = new ColorWheel();
      ResultSet rs = ps.executeQuery();
      boolean firstRow = true;
      int rowNum = 0;
      int colTotal = 0;
      String prevMgr = "";
      while (rs.next()) {
         if (firstRow) {
            colTotal = addHeaderRow(rs, sheet.createRow(rowNum++), stylesTable);
            firstRow = false;
         }
         Row row = sheet.createRow(rowNum++);
         if (!prevMgr.equals(rs.getString(1))) {
            prevMgr = rs.getString(1);
            cw.next();
         }
         for (int col = 0; col < colTotal; col++) {
            Cell cell = row.createCell(col);
            CellStyle style = new XSSFCellStyle(stylesTable);
            style.setFillForegroundColor(cw.getColor());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
            buildPlanCell(col, cell, rs);
         }
      }
      for (int col = 0; col < colTotal; col++) {
         sheet.autoSizeColumn(col);
      }
   }

   private void buildPlanCell(int col, Cell cell, ResultSet rs) throws SQLException {
      CellStyle style = cell.getCellStyle();
      switch (col) {
         case 2:
         case 5:
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(rs.getInt(col + 1));
            style.setDataFormat((short)1);
            break;

         case 6:
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(rs.getInt(col + 1));
            style.setDataFormat((short)5);
            break;

         case 4:
            cell.setCellType(CellType.STRING);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("E, dd MMM");
            LocalDate ld = new LocalDate(rs.getDate(col+ 1).getTime());
            cell.setCellValue(formatter.print(ld));
            break;

         default:
            cell.setCellType(CellType.STRING);
            cell.setCellValue(rs.getString(col + 1));
            break;
      }
   }

   private class ColorWheel {
      private short[] colors = new short[]{
         IndexedColors.LIGHT_BLUE.getIndex(),
         IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(),
         IndexedColors.LIGHT_GREEN.getIndex(),
         IndexedColors.LIGHT_ORANGE.getIndex(),
         IndexedColors.LIGHT_TURQUOISE.getIndex(),
         IndexedColors.LIGHT_YELLOW.getIndex()
      };
      private short current = 0;

      public short getColor() {
         return colors[current % colors.length];
      }

      public void next() {
         current++;
      }
   }
}
