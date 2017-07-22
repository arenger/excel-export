package com.pavementcorporation.xls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.pavementcorporation.xls.dao.ClientDao;
import com.pavementcorporation.xls.dao.ProjectDao;
import com.pavementcorporation.xls.dao.TaskDao;
import com.pavementcorporation.xls.dto.Client;
import com.pavementcorporation.xls.dto.Project;
import com.pavementcorporation.xls.dto.Task;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.h2.tools.RunScript;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Singleton
public class Loader {
   private static final Logger LOG = LoggerFactory.getLogger(Loader.class);

   private SqlSessionProvider sqlSessionProvider;
   private ClientDao  clientDao;
   private ProjectDao projectDao;
   private TaskDao    taskDao;

   private Map<String, Task.Service> crewMap;

   @Inject
   public Loader(SqlSessionProvider sqlSessionProvider, ClientDao clientDao, ProjectDao projectDao, TaskDao taskDao)
      throws IOException, SQLException {
      this.sqlSessionProvider = sqlSessionProvider;
      this.clientDao = clientDao;
      this.projectDao = projectDao;
      this.taskDao = taskDao;

      try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("db.sql");
           Reader in = new InputStreamReader(is);
           SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
           RunScript.execute(conn, in);
      }
   }

   void run(File xlsFile) throws IOException {
      System.out.println("Opening " + xlsFile);
      LOG.info("Opening {}", xlsFile);
      try (FileInputStream in = new FileInputStream(xlsFile)) {
         XSSFWorkbook wb = new XSSFWorkbook(in);
         loadCrewMap(wb.getSheet("Config"));
         loadClients(wb.getSheet("Clients"));
         loadProjects(wb.getSheet("Projects"));
         loadTasks(wb.getSheet("Scheduling"));
      }
   }

   private void loadCrewMap(XSSFSheet sheet) {
      crewMap = new HashMap<>();
      Iterator<Row> i = sheet.rowIterator();
      boolean crewSection = false;
      while (i.hasNext()) {
         Row r = i.next();
         if (!crewSection && r.getCell(0) != null && r.getCell(0).getStringCellValue().equals("Crews:")) {
            crewSection = true;
            continue;
         }
         if (crewSection) {
            if (r.getCell(0) == null || (r.getCell(0).getCellTypeEnum() == CellType.BLANK)) {
               break;
            }
            LOG.debug("mapping {} => {}", r.getCell(0).getStringCellValue(), r.getCell(1).getStringCellValue());
            crewMap.put(r.getCell(0).getStringCellValue(), Task.Service.valueOf(r.getCell(1).getStringCellValue()));
         }
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
      LOG.info("loading clients");
      System.out.println("Loading clients");
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
         }
         session.commit();
      }
   }

   private void loadProjects(XSSFSheet sheet) {
      LOG.info("loading projects");
      System.out.println("Loading projects");
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
         }
         session.commit();
      }
   }

   private void loadTasks(XSSFSheet sheet) {
      LOG.info("loading tasks");
      System.out.println("Loading tasks");
      try (SqlSession session = sqlSessionProvider.openSession()) {
         Iterator<Row> i = sheet.rowIterator();
         boolean firstRow = true;
         while (i.hasNext()) {
            Row r = i.next();
            if (firstRow) {
               firstRow = false;
               continue;
            }
            if ((r.getCell(0) == null) ||
                (r.getCell(0).getCellTypeEnum() != CellType.NUMERIC) ||
                (r.getCell(5) == null)) {
               continue;
            }
            Task t = new Task(r.getRowNum(), (int)r.getCell(0).getNumericCellValue());
            t.setCrew(r.getCell(5).getStringCellValue());
            t.setService(crewMap.get(r.getCell(5).getStringCellValue()));
            if (r.getCell(4) != null) {
               LocalDateTime ld = new LocalDateTime(1970, 1, 1, 0, 0);
               ld = ld.plusDays((int)r.getCell(4).getNumericCellValue() - 25569);
               if (r.getCell(7) != null) {
                  ld = ld.plusMinutes((int)(1440 * r.getCell(7).getNumericCellValue()));
               }
               t.setScheduleDate(new Timestamp(ld.toDate().getTime()));
            }
            if (r.getCell(6) != null) {
               t.setHours((int)r.getCell(6).getNumericCellValue());
            }
            taskDao.insert(session, t);
         }
         session.commit();
      }
   }

}
