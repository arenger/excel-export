package com.pavementcorporation.xls;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Singleton
public class CalCopier {
   private static final Logger LOG = LoggerFactory.getLogger(Writer.class);
   private static final String APPLICATION_NAME = "ExcelExport";

   private final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".excel-export");

   private HttpTransport httpTransport;
   private FileDataStoreFactory dataStoreFactory;
   private SqlSessionProvider sqlSessionProvider;
   private final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
   private final List<String> scopes = Arrays.asList(CalendarScopes.CALENDAR);
   private final Map<String, String> crewMap; //key is crew name, value is calendar id hash

   @Inject
   public CalCopier(SqlSessionProvider sqlSessionProvider, Loader loader) throws GeneralSecurityException, IOException {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      this.sqlSessionProvider = sqlSessionProvider;
      crewMap = loader.getCalIdMap();
   }

   private Credential authorize() throws IOException {
      InputStream in = CalCopier.class.getClassLoader().getResourceAsStream("client_secret.json");
      GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

      // Build flow and trigger user authorization request.
      GoogleAuthorizationCodeFlow flow =
         new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes)
            .setDataStoreFactory(dataStoreFactory)
            .setAccessType("offline")
            .build();
      Credential credential = new AuthorizationCodeInstalledApp(
         flow, new LocalServerReceiver()).authorize("user");
      LOG.info("Credentials saved to {}", DATA_STORE_DIR.getAbsolutePath());
      return credential;
   }

   private Calendar getCalendarService() throws IOException {
      Credential credential = authorize();
      return new com.google.api.services.calendar.Calendar.Builder(
         httpTransport, jsonFactory, credential)
         .setApplicationName(APPLICATION_NAME)
         .build();
   }

   void listCalendards() throws IOException {
      com.google.api.services.calendar.Calendar service = getCalendarService();
      List<CalendarListEntry> list = service.calendarList().list().execute().getItems();
      for (CalendarListEntry e : list) {
         String s = String.format("%30s %s", e.getSummary(), e.getId());
         System.out.println(s);
         LOG.info(s);
      }
   }

   private class BatchHelper {
      private int batchCount;
      private BatchRequest batch;
      private String prevHash;
      private JsonBatchCallback<Event> handler;
      private Calendar service;

      public BatchHelper(Calendar service) {
         this.service = service;
      }

      public void next(String idHash, Event e, String crew) throws IOException {
         if (!idHash.equals(prevHash)) {
            flush();
            LOG.info("Creating events for {}", crew);
            System.out.println(" - " + crew);
            prevHash = idHash;
         }
         LOG.debug("{} to receive {}", idHash, e);
         service.events().insert(idFromHash(idHash), e).queue(batch, handler);
         batchCount++;
      }

      public void flush() throws IOException {
         if (batchCount > 0) {
            batch.execute();
         }
         handler = new JsonBatchCallback<Event>() {
            @Override
            public void onSuccess(Event event, HttpHeaders responseHeaders) throws IOException {
            }

            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
               LOG.error("Populate failed for {}", prevHash);
               System.err.printf("Populate failed for %s\n", prevHash);
            }
         };
         batch = service.batch();
         batchCount = 0;
      }
   }

   private void createFutureEvents(Calendar service) throws SQLException, IOException {
      String query =
         "select t.crew, t.schedule_date, t.hours, t.project_id, p.name project_name, p.manager, " +
            "c.name client_name, c.address, c.city, c.state, c.zip\n" +
            "from task t join project p on t.project_id = p.id join client c on p.client_id = c.id\n" +
            "where t.schedule_date >= current_date\n" +
            "order by t.crew, t.schedule_date";
      LOG.debug("query: {}", query);
      System.out.println("Creating events");
      try (SqlSession session = sqlSessionProvider.openSession();
           Connection conn = session.getConnection()) {
         PreparedStatement ps = conn.prepareStatement(query);
         ResultSet rs = ps.executeQuery();
         BatchHelper helper = new BatchHelper(service);
         while (rs.next()) {
            String crew = rs.getString("crew");
            final String idHash = crewMap.get(crew);
            if (StringUtils.isEmpty(idHash)) {
               LOG.warn("No hash for '{}'", crew);
               System.err.printf("Warning: Skipping event b/c unknown crew: \"%s\"\n", crew);
               continue;
            }
            Event e = new Event();
            e.setSummary(String.format("%s: %s", rs.getString("project_id"), rs.getString("project_name")));
            e.setDescription(String.format("Client: %s\nManager: %s",
               rs.getString("client_name"), rs.getString("manager")));

            long startMillis = rs.getTimestamp("schedule_date").getTime();
            e.setStart(eventDateTimeFromMillis(startMillis));
            int hours = rs.getInt("hours");
            if (hours == 0) {
               hours = 1;
            }
            e.setEnd(eventDateTimeFromMillis(startMillis + 3600000 * hours));
            e.setLocation(String.format("%s, %s %s %s",
               rs.getString("address"), rs.getString("city"), rs.getString("state"), rs.getString("zip")));
            helper.next(idHash, e, crew);
         }
         helper.flush();
      }
   }

   private String idFromHash(String hash) {
      return String.format("%s@group.calendar.google.com", hash);
   }

   private EventDateTime eventDateTimeFromMillis(long millis) {
      EventDateTime dt = new EventDateTime();
      dt.setDateTime(new DateTime(millis));
      return dt;
   }

   private void clearFutureEvents(Calendar service) throws IOException {
      System.out.println("Clearing future events");
      long now = System.currentTimeMillis();
      for (Map.Entry<String, String> entry : crewMap.entrySet()) {
         System.out.printf(" - %s\n", entry.getKey());
         final String idHash = entry.getValue();
         JsonBatchCallback<Void> handler = new JsonBatchCallback<Void>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
               LOG.error("Clear failed for {}", idHash);
               System.out.printf("Clear failed for %s\n", idHash);
            }

            @Override
            public void onSuccess(Void aVoid, HttpHeaders responseHeaders) throws IOException {
               LOG.info("Clear succeeded for {}", idHash);
            }
         };
         BatchRequest batch = service.batch();
         Events events = service.events().list(idFromHash(idHash))
            .setTimeMin(new DateTime(now - (now % 86400000)))
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute();
         List<Event> items = events.getItems();
         int i = 0;
         for (Event event : items) {
            service.events().delete(idFromHash(idHash), event.getId()).queue(batch, handler);
            i++;
         }
         if (i > 0) {
            batch.execute();
         }
      }
   }

   public void run() throws SQLException, IOException {
      Calendar service = getCalendarService();
      clearFutureEvents(service);
      createFutureEvents(service);
   }

}