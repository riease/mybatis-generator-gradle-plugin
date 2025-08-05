package com.riease.mybatis;

public class DatabaseDriver {

  private String artifactId;
  private String driverClass;
  private boolean useCatalog;
  private boolean useSchema;

  public DatabaseDriver() {
  }

  public DatabaseDriver(String artifactId, String driverClass, boolean useCatalog, boolean useSchema) {
    this.artifactId = artifactId;
    this.driverClass = driverClass;
    this.useCatalog = useCatalog;
    this.useSchema = useSchema;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getDriverClass() {
    return driverClass;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  public boolean isUseCatalog() {
    return useCatalog;
  }

  public void setUseCatalog(boolean useCatalog) {
    this.useCatalog = useCatalog;
  }

  public boolean isUseSchema() {
    return useSchema;
  }

  public void setUseSchema(boolean useSchema) {
    this.useSchema = useSchema;
  }
}
