package com.riease.mybatis;

import java.util.List;

public class TableMeta {

  /** 資料表名稱 */
  String tableName;
  /** 欄位資訊列表 */
  List<ColumnMeta> columns;
  /** 主鍵欄位名稱列表 */
  List<String> primaryKeys;
  /** 外鍵資訊列表 */
  List<ForeignKeyMeta> foreignKeys;
  /** 資料表備註 */
  String comment;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public List<ColumnMeta> getColumns() {
    return columns;
  }

  public void setColumns(List<ColumnMeta> columns) {
    this.columns = columns;
  }

  public List<String> getPrimaryKeys() {
    return primaryKeys;
  }

  public void setPrimaryKeys(List<String> primaryKeys) {
    this.primaryKeys = primaryKeys;
  }

  public List<ForeignKeyMeta> getForeignKeys() {
    return foreignKeys;
  }

  public void setForeignKeys(List<ForeignKeyMeta> foreignKeys) {
    this.foreignKeys = foreignKeys;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}


