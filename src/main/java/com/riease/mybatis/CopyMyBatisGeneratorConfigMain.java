package com.riease.mybatis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * CopyMyBatisGeneratorConfigMain
 * 可用於 Gradle JavaExec 執行，複製 mybatis-generator-config.xml 並處理參數替換與 table append。
 */
public class CopyMyBatisGeneratorConfigMain {

  private final Map<String, DatabaseDriver> databaseDriverMap = Map.of(
    "mysql-connector-java", new DatabaseDriver("mysql-connector-java", "com.mysql.cj.jdbc.Driver", true, false),
    "mariadb-java-client", new DatabaseDriver("mariadb-java-client", "org.mariadb.jdbc.Driver", true, false),
    "postgresql", new DatabaseDriver("postgresql", "org.postgresql.Driver", true, false),
    "ojdbc", new DatabaseDriver("ojdbc8", "oracle.jdbc.OracleDriver", false, true),
    "mssql-jdbc", new DatabaseDriver("mssql-jdbc", "com.microsoft.sqlserver.jdbc.SQLServerDriver", true, false),
    "sqlite-jdbc", new DatabaseDriver("sqlite-jdbc", "org.sqlite.JDBC", false, false)
  );

  private CopyMyBatisParameter parameter;

  public static void main(String[] args) throws Exception {
    CopyMyBatisGeneratorConfigMain main = new CopyMyBatisGeneratorConfigMain();
    main.loadParameter();
    main.run();
  }

  private void run() {
    File targetFile = new File(this.parameter.getProjectDir(), "build-tools/mybatis/mybatis-generator-config.xml");
    // 檢查目標目錄是否存在，若不存在則建立
    if (!targetFile.getParentFile().exists()) {
      boolean isDone = targetFile.getParentFile().mkdirs();
      if (!isDone) {
        System.err.println("無法建立目標目錄: " + targetFile.getParentFile().getAbsolutePath());
        return;
      }
    }

    // 檢查目標檔案是否存在，當不存在則複製配置檔案 或是 當已存在則根據參數決定是否覆寫
    boolean needToCopy = !targetFile.exists() || this.parameter.isOverwrite();
    if (needToCopy) {
      // 複製檔案到目標位置
      copyConfigFile(targetFile);
    }

    try {
      // 使用 JSoup 讀取目標檔案，修改配置內容
      Document doc = Jsoup.parse(targetFile, "UTF-8", "", Parser.xmlParser());
      // 設定輸出格式為 XML
      doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

      // 修改資料庫連線設定
      processForJdbc(doc);

      // 修改目標包名
      processForTargetPackage(doc);

      // 修改資料表設定
      processForTables(doc);

      // 回寫到 目標檔案
      try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
        writer.write(doc.outerHtml());
      }
    } catch (IOException e) {
      System.err.println("修改 mybatis-generator-config.xml 時發生錯誤: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private void processForJdbc(Document doc) {
    Element jdbcElement = doc.selectFirst("jdbcConnection");
    if (Objects.isNull(jdbcElement)) {
      System.err.println("找不到 jdbcConnection 元素，請檢查 mybatis-generator-config.xml 的結構。");
      return;
    }

    // 替換 jdbcConnection 的屬性 if parameter exist
    if (StringUtils.isNotBlank(parameter.getDriverClass())) {
      jdbcElement.attr("driverClass", parameter.getDriverClass());
    }
    if (StringUtils.isNotBlank(parameter.getConnectionURL())) {
      jdbcElement.attr("connectionURL", parameter.getConnectionURL());
    }

    // 注意：這邊不會置換 username 和 password，因為這些資訊可能包含敏感資料，
  }

  private void processForTargetPackage(Document doc) {
    Element javaModelElement = doc.selectFirst("javaModelGenerator");
    if (Objects.isNull(javaModelElement)) {
      System.err.println("找不到 javaModelGenerator 元素，請檢查 mybatis-generator-config.xml 的結構。");
      return;
    }
    // 替換 javaModelGenerator 的 targetPackage 屬性
    if (StringUtils.isNotBlank(parameter.getTargetPackage())) {
      javaModelElement.attr("targetPackage", parameter.getTargetPackage() + ".dao.entity");
    }

    Element javaClientElement = doc.selectFirst("javaClientGenerator");
    if (Objects.isNull(javaClientElement)) {
      System.err.println("找不到 javaClientGenerator 元素，請檢查 mybatis-generator-config.xml 的結構。");
      return;
    }
    // 替換 javaClientGenerator 的 targetPackage 屬性
    if (StringUtils.isNotBlank(parameter.getTargetPackage())) {
      javaClientElement.attr("targetPackage", parameter.getTargetPackage() + ".dao.mapper");
    }
  }

  private void processForTables(Document doc) {
    if (!parameter.isEnableReadDatabase()) {
      System.out.println("參數 enableReadDatabase 為 false，將不讀取資料庫表格資訊。");
      return;
    }

    List<TableMeta> tableMetaList = queryTableMeta();
    if (tableMetaList.isEmpty()) {
      System.out.println("沒有找到任何資料表");
      return;
    }

    if(!parameter.isAppendTables()) {
      System.out.println("不會追加資料表到 mybatis-generator-config.xml 中。");
      return;
    }

    // 找到首的 context 元素
    Element contextElement = doc.selectFirst("context");
    assert contextElement != null;

    // 找到已存在於 context 中的 table 元素
    Elements tableElements = contextElement.select("table");
    List<String> existingTables = tableElements.eachAttr("tableName");

    // 把 tableMetaList 換為 XML 元素
    for (TableMeta tableMeta : tableMetaList) {
      // 檢查該 table 是否需要處理
      if(existingTables.contains(tableMeta.getTableName())) {
        System.out.println("跳過已存在的表格: " + tableMeta.getTableName());
        continue;
      }
      System.out.println("處理表格: " + tableMeta.getTableName());
      StringBuilder xmlBuilder = new StringBuilder();
      if (StringUtils.isNotBlank(tableMeta.getComment())) {
        xmlBuilder.append(System.lineSeparator())
          .append("\t\t<!-- ").append(tableMeta.getComment()).append(" -->\n\t\t");
      }
      xmlBuilder.append("<table tableName=\"")
        .append(tableMeta.getTableName()).append("\">")
        .append(System.lineSeparator());
      // TODO 處理欄位資訊

      // table 結尾
      xmlBuilder.append("\t\t</table>")
        .append(System.lineSeparator());
      contextElement.append(xmlBuilder.toString());
    }
  }

  private List<TableMeta> queryTableMeta() {
    // from databaseDriverMap 取得相對定的 DatabaseDriver
    // 尋找方式為比對 driverClass 是否相同
    DatabaseDriver driver = databaseDriverMap.values().stream()
      .filter(d -> StringUtils.equals(d.getDriverClass(), parameter.getDriverClass()))
      .findFirst().orElse(null);
    if (Objects.isNull(driver)) {
      System.err.println("找不到對應的 DatabaseDriver，請檢查 driverClass 是否正確。");
      return Collections.emptyList();
    }

    // 最後回傳的 tableMetaList
    List<TableMeta> tableMetaList = new ArrayList<>();

    // 讀取資料庫表格資訊，並添加到 XML 中
    // 這裡可以使用 JDBC 或其他方式來讀取資料庫
    try (Connection conn = DriverManager.getConnection(parameter.getConnectionURL(), parameter.getUsername(), parameter.getUsername())) {
      DatabaseMetaData meta = conn.getMetaData();

      // 根據 driver 的設定來決定是否使用 catalog 和 schema
      String catalog = null;
      if (driver.isUseCatalog()) {
        catalog = parseSchemaFromUrl(parameter.getConnectionURL());
      }
      String schema = null;
      if (driver.isUseSchema()) {
        schema = parseSchemaFromUrl(parameter.getConnectionURL());
      }

      // 查詢所有 TABLE
      try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[] {"TABLE"})) {
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");
          String remarks = rs.getString("REMARKS");
          System.out.println("Table: " + tableName + ", Comment: " + remarks);

          TableMeta tableMeta = new TableMeta();
          tableMeta.setTableName(tableName);
          tableMeta.setComment(remarks);
          tableMeta.setColumns(new ArrayList<>());
          tableMeta.setPrimaryKeys(new ArrayList<>());
          tableMeta.setForeignKeys(new ArrayList<>());
          tableMetaList.add(tableMeta);

          // 查詢欄位資訊
          try (ResultSet columns = meta.getColumns(catalog, schema, tableName, "%")) {
            while (columns.next()) {
              String columnName = columns.getString("COLUMN_NAME");
              String typeName = columns.getString("TYPE_NAME");
              String isNullable = columns.getString("IS_NULLABLE");
              String columnRemarks = columns.getString("REMARKS");
              System.out.println("Column: " + columnName + " " + typeName + " " + isNullable + " " + columnRemarks);
              ColumnMeta columnMeta = new ColumnMeta();
              columnMeta.setName(columnName);
              columnMeta.setType(typeName);
              columnMeta.setNullable("YES".equalsIgnoreCase(isNullable));
              columnMeta.setComment(columnRemarks);
              tableMeta.getColumns().add(columnMeta);
            }
          }

          // 查詢主鍵
          try (ResultSet pk = meta.getPrimaryKeys(catalog, schema, tableName)) {
            System.out.print("  Primary Keys:");
            boolean hasPk = false;
            while (pk.next()) {
              hasPk = true;
              String pkName = pk.getString("COLUMN_NAME");
              System.out.print(" " + pkName);
              tableMeta.getPrimaryKeys().add(pkName);
            }
            if (!hasPk) {
              System.out.print(" 無主鍵");
            }
            System.out.println();
          }

          // 查詢外鍵
          try (ResultSet fk = meta.getImportedKeys(catalog, schema, tableName)) {
            System.out.println("  Foreign Keys:");
            boolean hasFk = false;
            while (fk.next()) {
              hasFk = true;
              String fkColumnName = fk.getString("FKCOLUMN_NAME");
              String pkTableName = fk.getString("PKTABLE_NAME");
              String pkColumnName = fk.getString("PKCOLUMN_NAME");
              System.out.println("    " + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
              ForeignKeyMeta foreignKeyMeta = new ForeignKeyMeta();
              foreignKeyMeta.setColumnName(fkColumnName);
              foreignKeyMeta.setReferenceTable(pkTableName);
              foreignKeyMeta.setReferenceColumn(pkColumnName);
              tableMeta.getForeignKeys().add(foreignKeyMeta);
            }
            if (!hasFk) {
              System.out.println("無外鍵");
            }
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("讀取資料庫表格資訊時發生錯誤: " + e.getMessage());
      throw new RuntimeException(e);
    }

    return tableMetaList;
  }

  private void copyConfigFile(File targetFile) {
    // 讀取資源檔案（假設在 resources/config/mybatis-generator-config.xml）
    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("config/mybatis-generator-config.xml")) {
      if (in == null) {
        throw new FileNotFoundException("找不到 mybatis-generator-config.xml 檔案，請確保它存在於 resources/config 目錄下。");
      }
      Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      System.err.println("複製 mybatis-generator-config.xml 時發生錯誤: " + e.getMessage());
      throw new RuntimeException(e);
    }

    // 當複製檔案情況下，需要重新讀取所有的資料庫
    this.parameter.setAppendTables(true);
  }

  private void loadParameter() {
    // 讀取參數或環境變數
    Properties props = System.getProperties();
    String overwrite = getProp("mybatis.generator.copy.overwrite", "false");
    String appendTables = getProp("mybatis.generator.append.tables", "true");

    this.parameter = new CopyMyBatisParameter();
    this.parameter.setProjectDir(getProp("user.dir", ""));
    this.parameter.setOverwrite(StringUtils.equals(overwrite, "true"));
    this.parameter.setAppendTables(StringUtils.equals(appendTables, "true"));
    this.parameter.setTargetPackage(getProp("mybatis.generator.target.package", ""));
    this.parameter.setDriverClass(getProp("mybatis.generator.db.driverClass", null));
    this.parameter.setConnectionURL(getProp("mybatis.generator.db.connectionURL", null));
    this.parameter.setUsername(getProp("mybatis.generator.db.username", ""));
    this.parameter.setPassword(getProp("mybatis.generator.db.password", ""));
    this.parameter.checkDatabaseConfig();

  }

  private static String getProp(String key, String def) {
    String v = System.getProperty(key);
    if (StringUtils.isBlank(v)) {
      v = System.getenv(key);
    }
    return StringUtils.defaultIfBlank(v, def);
  }


  public static String parseSchemaFromUrl(String url) {
    if (url == null) {
      return null;
    }

    // MySQL/MariaDB: jdbc:mysql://host:port/dbname
    String mysqlRegex = "jdbc:(mysql|mariadb)://[^/]+/([^?;]+)";
    java.util.regex.Matcher mysqlMatcher = java.util.regex.Pattern.compile(mysqlRegex).matcher(url);
    if (mysqlMatcher.find()) {
      return mysqlMatcher.group(2);
    }

    // PostgreSQL: jdbc:postgresql://host:port/dbname
    String pgRegex = "jdbc:postgresql://[^/]+/([^?;]+)";
    java.util.regex.Matcher pgMatcher = java.util.regex.Pattern.compile(pgRegex).matcher(url);
    if (pgMatcher.find()) {
      return pgMatcher.group(1);
    }

    // Oracle: jdbc:oracle:thin:@host:port:sid 或 jdbc:oracle:thin:@//host:port/service
    String oracleRegex = "jdbc:oracle:thin:@(?:\\/\\/)?[^:/]+(?::\\d+)?[:/]([^?;/]+)";
    java.util.regex.Matcher oracleMatcher = java.util.regex.Pattern.compile(oracleRegex).matcher(url);
    if (oracleMatcher.find()) {
      return oracleMatcher.group(1);
    }

    // SQL Server: jdbc:sqlserver://host:port;databaseName=dbname
    String mssqlRegex = "jdbc:sqlserver://[^;]+;databaseName=([^;?]+)";
    java.util.regex.Matcher mssqlMatcher = java.util.regex.Pattern.compile(mssqlRegex).matcher(url);
    if (mssqlMatcher.find()) {
      return mssqlMatcher.group(1);
    }

    // SQLite: jdbc:sqlite:/path/to/dbfile 或 jdbc:sqlite::memory:
    String sqliteRegex = "jdbc:sqlite:(.+)";
    java.util.regex.Matcher sqliteMatcher = java.util.regex.Pattern.compile(sqliteRegex).matcher(url);
    if (sqliteMatcher.find()) {
      return sqliteMatcher.group(1);
    }

    return null;
  }
}
