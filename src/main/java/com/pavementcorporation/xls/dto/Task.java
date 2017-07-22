package com.pavementcorporation.xls.dto;

import java.sql.Timestamp;

public class Task extends IdObj {

   public String getCrew() {
      return crew;
   }

   public void setCrew(String crew) {
      this.crew = crew;
   }

   public enum Service {CPV, RPV, INF, SCT, STR, CSL, SUB}

   private final int projectId;
   private Service service;
   private String crew;

   //Java 8's (or JodaTime's) LocalDate is better, but mybatis already has a DateOnlyTypeHandler...
   private Timestamp scheduleDate;

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

   public Timestamp getScheduleDate() {
      return scheduleDate;
   }

   public void setScheduleDate(Timestamp scheduleDate) {
      this.scheduleDate = scheduleDate;
   }

   public int getHours() {
      return hours;
   }

   public void setHours(int hours) {
      this.hours = hours;
   }

}
