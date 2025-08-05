package com.riease.mybatis;

import org.apache.commons.lang3.StringUtils;

public class CopyMyBatisParameter {

  private String projectDir;
  private boolean overwrite;
  private boolean appendTables;
  private String driverClass;
  private String connectionURL;
  private String targetPackage;
  private String username;
  private String password;
  private boolean enableReadDatabase = false;

  public String getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(String projectDir) {
    this.projectDir = projectDir;
  }

  public boolean isOverwrite() {
    return overwrite;
  }

  public void setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
  }

  public boolean isAppendTables() {
    return appendTables;
  }

  public void setAppendTables(boolean appendTables) {
    this.appendTables = appendTables;
  }

  public String getDriverClass() {
    return driverClass;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  public String getConnectionURL() {
    return connectionURL;
  }

  public void setConnectionURL(String connectionURL) {
    this.connectionURL = connectionURL;
  }

  public String getTargetPackage() {
    return targetPackage;
  }

  public void setTargetPackage(String targetPackage) {
    this.targetPackage = targetPackage;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isEnableReadDatabase() {
    return enableReadDatabase;
  }

  public void setEnableReadDatabase(boolean enableReadDatabase) {
    this.enableReadDatabase = enableReadDatabase;
  }

  public void checkDatabaseConfig() {
    boolean result = true;
    if (StringUtils.isAnyBlank(driverClass, connectionURL, username, password)) {
      result = false;
    }
    this.setEnableReadDatabase(result);
  }
}
