package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.dto.Task;
import org.apache.ibatis.annotations.Insert;

interface TaskMapper {
   @Insert("insert into task (id, project_id, service, crew, schedule_date, hours) " +
           "values (#{id}, #{projectId}, #{service}, #{crew}, #{scheduleDate}, #{hours})")
   int insert(Task task);
}
