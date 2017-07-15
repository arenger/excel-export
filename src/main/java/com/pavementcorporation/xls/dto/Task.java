package com.pavementcorporation.xls.dto;

import java.util.Date;

public class Task extends IdObj {

   public enum Service {CPV, RPV, INF, SCT, STR, CSL, SUB}

   private final int projectId;
   private Service service;

   //Java 8's (or JodaTime's) LocalDate is better, but mybatis already has a DateOnlyTypeHandler...
   private Date scheduleDate;

   private int hours;

   public Task(int id, int projectId) {
      super(id);
      this.projectId = projectId;
   }

   public int getProjectId() {
      return projectId;
   }

   public Service getService() {
      return service;
   }

   public void setService(Service service) {
      this.service = service;
   }

   public Date getScheduleDate() {
      return scheduleDate;
   }

   public void setScheduleDate(Date scheduleDate) {
      this.scheduleDate = scheduleDate;
   }

   public int getHours() {
      return hours;
   }

   public void setHours(int hours) {
      this.hours = hours;
   }

}
