package com.pavementcorporation.xls.dto;

public class Project extends IdObj {
   private final int clientId;

   private String  name;
   private String  amount;
   private Boolean invoiced;
   private String  manager;
   private String  accountManager;

   public Project(int id, int clientId) {
      super(id);
      this.clientId = clientId;
   }

   public int getClientId() {
      return clientId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getAmount() {
      return amount;
   }

   public void setAmount(String amount) {
      this.amount = amount;
   }

   public Boolean getInvoiced() {
      return invoiced;
   }

   public void setInvoiced(Boolean invoiced) {
      this.invoiced = invoiced;
   }

   public String getManager() {
      return manager;
   }

   public void setManager(String manager) {
      this.manager = manager;
   }

   public String getAccountManager() {
      return accountManager;
   }

   public void setAccountManager(String accountManager) {
      this.accountManager = accountManager;
   }
}
