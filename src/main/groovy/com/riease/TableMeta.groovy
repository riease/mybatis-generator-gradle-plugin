package com.riease

/**
 * TableMeta
 * 用來存放資料表相關資訊，例如：
 * - 資料表名稱
 * - 所有欄位名稱、型別
 * - 主鍵、外鍵
 * - 備註等
 */
class TableMeta {
    /** 資料表名稱 */
    String tableName
    /** 欄位資訊列表 */
    List<ColumnMeta> columns = []
    /** 主鍵欄位名稱列表 */
    List<String> primaryKeys = []
    /** 外鍵資訊列表 */
    List<ForeignKeyMeta> foreignKeys = []
    /** 資料表備註 */
    String comment
}

/**
 * 欄位資訊物件
 */
class ColumnMeta {
    /** 欄位名稱 */
    String name
    /** 欄位型別 */
    String type
    /** 欄位長度 */
    Integer length
    /** 欄位是否允許為空 */
    boolean nullable = true
    /** 是否為主鍵 */
    boolean isPrimaryKey = false
    /** 是否為外鍵 */
    boolean isForeignKey = false
    /** 欄位備註 */
    String comment
}

/**
 * 外鍵資訊物件
 */
class ForeignKeyMeta {
    /** 外鍵欄位名稱 */
    String columnName
    /** 參照的資料表名稱 */
    String referenceTable
    /** 參照的欄位名稱 */
    String referenceColumn
}
