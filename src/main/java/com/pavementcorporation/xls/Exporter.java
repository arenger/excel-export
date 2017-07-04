package com.pavementcorporation.xls;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Exporter {
   private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);

   private final File xlsFile;

   public Exporter(String[] args) {
      if (args.length != 1) {
         throw new RuntimeException("Expected 1 argument; Path to xls file.");
      }
      xlsFile = new File(args[0]);
      if (!xlsFile.canRead()) {
         throw new RuntimeException("Can't read " + xlsFile.getAbsolutePath());
      }
   }

   private void run() throws IOException {
      System.out.println("run");
      Workbook wb = new Workbook(xlsFile);
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
