package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.dto.Task;
import org.apache.ibatis.session.SqlSession;

import javax.inject.Singleton;

@Singleton
public class TaskDao {

   public void insert(SqlSession session, Task r) {
      TaskMapper mapper = session.getMapper(TaskMapper.class);
      mapper.insert(r);
   }
}
