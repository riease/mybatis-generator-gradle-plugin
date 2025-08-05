package com.riease.mybatis;

public class ColumnMeta {

  /** 欄位名稱 */
  String name;
  /** 欄位型別 */
  String type;
  /** 欄位長度 */
  Integer length;
  /** 欄位是否允許為空 */
  boolean nullable = true;
  /** 是否為主鍵 */
  boolean isPrimaryKey = false;
  /** 是否為外鍵 */
  boolean isForeignKey = false;
  /** 欄位備註 */
  String comment;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getLength() {
    return length;
  }

  public void setLength(Integer length) {
    this.length = length;
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public boolean isPrimaryKey() {
    return isPrimaryKey;
  }

  public void setPrimaryKey(boolean primaryKey) {
    isPrimaryKey = primaryKey;
  }

  public boolean isForeignKey() {
    return isForeignKey;
  }

  public void setForeignKey(boolean foreignKey) {
    isForeignKey = foreignKey;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
