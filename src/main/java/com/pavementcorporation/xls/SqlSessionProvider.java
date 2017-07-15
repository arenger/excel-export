package com.pavementcorporation.xls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

@Singleton
public class SqlSessionProvider {
   private static final String CONFIG = "mybatis.xml";

   private SqlSessionFactory factory;

   @Inject
   public SqlSessionProvider(Exporter.Env env) throws IOException {
      try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(CONFIG)) {
         factory = new SqlSessionFactoryBuilder().build(in, env.toString().toLowerCase());
      }
   }

   public SqlSession openSession() {
      return factory.openSession();
   }
}
