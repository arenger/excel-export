package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.dto.Project;
import org.apache.ibatis.annotations.Insert;

interface ProjectMapper {
   @Insert("insert into project (id, client_id, name, amount, invoiced, manager, acct_mgr) " +
           "values (#{id}, #{clientId}, #{name}, #{amount}, #{invoiced}, #{manager}, #{accountManager})")
   int insert(Project project);
}
