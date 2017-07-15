package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.SqlSessionProvider;
import com.pavementcorporation.xls.dto.Client;
import org.apache.ibatis.session.SqlSession;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClientDao {
   private final SqlSessionProvider sqlSessionProvider;

   @Inject
   public ClientDao(SqlSessionProvider sqlSessionProvider) {
      this.sqlSessionProvider = sqlSessionProvider;
   }

   public Client getById(int id) {
      Client r = null;
      try (SqlSession session = sqlSessionProvider.openSession()) {
         ClientMapper mapper = session.getMapper(ClientMapper.class);
         r = mapper.getById(id);
      }
      return r;
   }

   public void insert(SqlSession session, Client r) {
      ClientMapper mapper = session.getMapper(ClientMapper.class);
      mapper.insert(r);
   }
}
