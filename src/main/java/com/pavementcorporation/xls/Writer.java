package com.pavementcorporation.xls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;

@Singleton
public class Writer {
   private static final Logger LOG = LoggerFactory.getLogger(Writer.class);

   private static final String FILENAME = "CustomViews.xlsx";

   private SqlSessionProvider sqlSessionProvider;

   @Inject
   public Writer(SqlSessionProvider sqlSessionProvider) {
      this.sqlSessionProvider = sqlSessionProvider;
   }

   public void run() throws SQLException, IOException {
      LOG.info("creating {}", FILENAME);
      System.out.println("Creating " + FILENAME);

      XSSFWorkbook workbook = new XSSFWorkbook();
      StylesTable stylesTable = workbook.getStylesSource();

      createPlanningSheets(workbook, stylesTable);

      try (FileOutputStream out = new FileOutputStream(FILENAME)) {
         workbook.write(out);
         workbook.close();
      }

      LOG.info("complete");
      System.out.println("Done");
   }

   private void createPlanningSheets(XSSFWorkbook workbook, StylesTable stylesTable) throws SQLException {
      String query = "select p.manager, p.amount, t.project_id, t.service, t.schedule_date, t.hours from task t " +
         "join project p on t.project_id = p.id where schedule_date between ? and ? " +
         "order by p.manager, p.id, t.schedule_date;";

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
      LOG.debug("Adding {}, range: {} - {}", name, start, end);
      ps.setDate(1, new Date(start.toDate().getTime()));
      ps.setDate(2, new Date(end.toDate().getTime()));
      XSSFSheet sheet = workbook.createSheet(name);

      ResultSet rs = ps.executeQuery();
      boolean firstRow = true;
      int rowNum = 0;
      int colTotal = 0;
      while (rs.next()) {
         Row row = sheet.createRow(rowNum++);
         if (firstRow) {
            ResultSetMetaData md = rs.getMetaData();
            colTotal = md.getColumnCount();
            for (int col = 0; col < colTotal; col++) {
               Cell cell = row.createCell(col);
               cell.setCellValue(md.getColumnName(col + 1));
               CellStyle style = new XSSFCellStyle(stylesTable);
               style.setBorderBottom(BorderStyle.THICK);
               cell.setCellStyle(style);
            }
            firstRow = false;
            continue;
         }
         for (int col = 0; col < colTotal; col++) {
            Cell cell = row.createCell(col);
            cell.setCellValue(rs.getString(col + 1));
         }
      }
   }
}
