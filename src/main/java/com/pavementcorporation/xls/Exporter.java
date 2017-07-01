package com.pavementcorporation.xls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter {
   private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);

   public Exporter(String[] args) {

   }

   private void run() {
      System.out.println("run");
   }

   public static void main(String[] args) {
      try {
         Exporter exporter = new Exporter(args);
         exporter.run();
      } catch (Exception e) {
         LOG.error("boom", e);
      }
   }

}
