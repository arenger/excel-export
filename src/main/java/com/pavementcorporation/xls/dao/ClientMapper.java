package com.pavementcorporation.xls.dao;

import com.pavementcorporation.xls.dto.Client;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

interface ClientMapper {
   @Results(id="client", value = {
      @Result(property = "id", column = "id"),
      @Result(property = "name", column = "name"),
      @Result(property = "address", column = "address"),
      @Result(property = "city", column = "city"),
      @Result(property = "state", column = "state"),
      @Result(property = "zip", column = "zip")
   })
   @Select("select id, name, address, city, state, zip from client where id = #{id}")
   Client getById(int id);

   @Insert("insert into client (id, name, address, city, state, zip) " +
           "values (#{id}, #{name}, #{address}, #{city}, #{state}, #{zip})")
   int insert(Client client);
}
