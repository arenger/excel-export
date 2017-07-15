package com.pavementcorporation.xls;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Exporter {
   private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);

   public enum Env {TEST, PROD}

   private final Env env;

   private final File xlsFile;
   private final Injector guice;

   public Exporter(String[] args) {
      if ((args.length < 1) || (args.length > 2)){
         throw new RuntimeException("Usage: Exporter path-to-xlsx [env]");
      }
      xlsFile = new File(args[0]);
      if (!xlsFile.canRead()) {
         throw new RuntimeException("Can't read " + xlsFile.getAbsolutePath());
      }
      if (args.length > 1) {
         env = Env.valueOf(args[1].toUpperCase());
      } else {
         env = Env.PROD;
      }
      LOG.info("Env: {}, File: {}", env, xlsFile);
      guice = Guice.createInjector(new BaseModule(env));
   }

   private void run() throws IOException {
      System.out.println("run");
      guice.getInstance(Loader.class).run(xlsFile);
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
