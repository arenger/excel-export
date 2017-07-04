package com.pavementcorporation.xls.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class IdObj {
   final int id;

   public IdObj(int id) {
      this.id = id;
   }

   public int getId() {
      return id;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (obj instanceof IdObj) {
         IdObj other = (IdObj)obj;
         return this.id == other.id;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return id;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
