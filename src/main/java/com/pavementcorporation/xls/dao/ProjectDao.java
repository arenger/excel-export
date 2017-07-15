package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.dto.Project;
import org.apache.ibatis.session.SqlSession;

import javax.inject.Singleton;

@Singleton
public class ProjectDao {

   public void insert(SqlSession session, Project r) {
      ProjectMapper mapper = session.getMapper(ProjectMapper.class);
      mapper.insert(r);
   }
}
