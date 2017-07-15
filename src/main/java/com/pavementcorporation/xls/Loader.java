package com.pavementcorporation.xls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.pavementcorporation.xls.dao.ClientDao;
import com.pavementcorporation.xls.dao.ProjectDao;
import com.pavementcorporation.xls.dto.Client;
import com.pavementcorporation.xls.dto.Project;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

@Singleton
public class Loader {
   private static final Logger LOG = LoggerFactory.getLogger(Loader.class);

   private SqlSessionProvider sqlSessionProvider;
   private ClientDao  clientDao;
   private ProjectDao projectDao;

   @Inject
   public Loader(SqlSessionProvider sqlSessionProvider, ClientDao clientDao, ProjectDao projectDao)
      throws IOException, SQLException {
      this.sqlSessionProvider = sqlSessionProvider;
      this.clientDao = clientDao;
      this.projectDao = projectDao;

      try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("db.sql");
           Reader in = new InputStreamReader(is);
           SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
           RunScript.execute(conn, in);
      }
   }

   void run(File xlsFile) throws IOException {
      try (FileInputStream in = new FileInputStream(xlsFile)) {
         XSSFWorkbook wb = new XSSFWorkbook(in);
         loadClients(wb.getSheet("Clients"));
         loadProjects(wb.getSheet("Projects"));
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
      try (SqlSession session = sqlSessionProvider.openSession()) {
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
            clientDao.insert(session, c);
            System.out.println(c);
         }
         session.commit();
      }
   }

   private void loadProjects(XSSFSheet sheet) {
      try (SqlSession session = sqlSessionProvider.openSession()) {
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
            projectDao.insert(session, p);
            System.out.println(p);
         }
         session.commit();
      }
   }

}
