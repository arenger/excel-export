package com.pavementcorporation.xls;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Exporter {
   private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);

   public enum Env {TEST, PROD}

   private final Env env;
   private final File xlsFile;
   private final Injector guice;
   private final boolean listAndQuit;

   public Exporter(String[] args) {
      if ((args.length < 1) || (args.length > 2)){
         throw new RuntimeException("Usage: Exporter -l | Exporter path-to-xlsx [env]");
      }
      if ("-l".equals(args[0])) {
         listAndQuit = true;
         xlsFile = null;
         env = Env.PROD;
      } else {
         listAndQuit = false;
         xlsFile = new File(args[0]);
         if (!xlsFile.canRead()) {
            throw new RuntimeException("Can't read " + xlsFile.getAbsolutePath());
         }
         if (args.length > 1) {
            env = Env.valueOf(args[1].toUpperCase());
         } else {
            env = Env.PROD;
         }
      }
      LOG.info("Env: {}, File: {}", env, xlsFile);
      guice = Guice.createInjector(new BaseModule(env));
   }

   private void run() throws IOException, SQLException {
      LOG.info("Startup, Version 1.3-SNAPSHOT");
      if (listAndQuit) {
         guice.getInstance(CalCopier.class).listCalendards();
      } else {
         guice.getInstance(Loader.class).run(xlsFile);
         guice.getInstance(Writer.class).run();
         guice.getInstance(CalCopier.class).run();
      }
      LOG.info("Complete");
      System.out.println("Complete");
   }

   public static void main(String[] args) {
      try {
         Exporter exporter = new Exporter(args);
         exporter.run();
      } catch (Exception e) {
         System.err.println((StringUtils.isNoneEmpty(e.getMessage()) ?
            e.getMessage() : "Error") + " - See log for more details");
         LOG.error("boom", e);
      }
   }

}
