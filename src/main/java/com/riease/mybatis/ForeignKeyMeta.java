package com.riease.mybatis;

public class ForeignKeyMeta {
  /** 外鍵欄位名稱 */
  String columnName;
  /** 參照的資料表名稱 */
  String referenceTable;
  /** 參照的欄位名稱 */
  String referenceColumn;

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getReferenceTable() {
    return referenceTable;
  }

  public void setReferenceTable(String referenceTable) {
    this.referenceTable = referenceTable;
  }

  public String getReferenceColumn() {
    return referenceColumn;
  }

  public void setReferenceColumn(String referenceColumn) {
    this.referenceColumn = referenceColumn;
  }
}
