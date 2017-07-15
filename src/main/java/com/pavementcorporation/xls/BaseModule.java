package com.pavementcorporation.xls;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.IOException;

public class BaseModule extends AbstractModule {

   private Exporter.Env env;

   public BaseModule(Exporter.Env env) {
      this.env = env;
   }

   @Override
   protected void configure() {
   }

   @Provides
   SqlSessionProvider initSqlSessionProvider() throws IOException {
      return new SqlSessionProvider(env);
   }
}
