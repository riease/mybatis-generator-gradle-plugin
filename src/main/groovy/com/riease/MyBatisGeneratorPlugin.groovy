package com.riease

import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

import java.sql.DriverManager

/**
 * MyBatisGeneratorPlugin
 *
 * 這是一個自訂的 Gradle Plugin，主要用於整合 MyBatis Generator 相關任務，
 * 包含：
 * 1. Spotless Java 格式化設定（針對 MyBatis 產生的檔案自動格式化）。
 * 2. 複製 mybatis-generator-config.xml 設定檔到專案目錄，並自動替換 JDBC 連線參數。
 * 3. 註冊 mybatisGenerate 任務，根據設定檔自動產生 MyBatis 相關程式碼。
 * 4. 註冊 mybatisGenerateAndFormat 任務，產生程式碼後自動格式化。
 * 5. 自動偵測專案所用 JDBC 驅動類別。
 *
 * 使用方式：
 * - 在 build.gradle 中套用本 Plugin。
 * - 依需求執行 mybatisGenerate 或 mybatisGenerateAndFormat 任務。
 * - JDBC 連線資訊可由專案屬性或環境變數提供。
 */
class MyBatisGeneratorPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    // Configure Spotless for Java files
    project.pluginManager.withPlugin('com.diffplug.spotless') {
      project.afterEvaluate {
        println('after evaluate for Spotless plugin')
        def spotlessExtension = project.extensions.findByName('spotless')
        if (spotlessExtension == null) {
          throw new IllegalStateException("Spotless plugin is not applied correctly.")
        }
        print('check spotlessExtension has java extension: ' + spotlessExtension.toString())
        println('config Spotless for Java files')
        spotlessExtension.java {
          target 'src/**/dao/entity/*.java', 'src/**/dao/mapper/*.java'
          // Use Eclipse formatter for MyBatis files
          def configFile = this.class.classLoader.getResource("config/mybatis-code-formatter.xml")
          if (configFile == null) {
            throw new IllegalStateException("Formatter configuration file not found in plugin resources.")
          }
          // 儲存到所引用的專案
          def targetFile = new File(project.rootDir, "build-tools/spotless/mybatis-code-formatter.xml")
          if (!targetFile.parentFile.exists()) {
            targetFile.parentFile.mkdirs()
          }
          targetFile.withOutputStream { out ->
            configFile.openStream().withStream { inStream ->
              out << inStream
            }
          }
          println "已複製 mybatis-code-formatter 設定檔到: ${targetFile.absolutePath}"

          // 使用 Eclipse formatter 設定檔
          eclipse().configFile(targetFile)
        }
      }
    }

    // 註冊 copyMyBatisGeneratorConfig 任務：目的為複製 mybatis-generator-config.xml 到專案 build-tools 目錄
    project.tasks.register("copyMyBatisGeneratorConfig") {
      group = 'MyBatis'
      description = '複製 mybatis-generator-config.xml 到專案 build-tools 目錄'
      doLast {
        def resource = this.class.classLoader.getResource("config/mybatis-generator-config.xml")
        if (resource == null) {
          throw new IllegalStateException("找不到 mybatis-generator-config.xml 資源")
        }
        def fileContent = resource.openStream().getText('UTF-8')

        // 替代變數 mybatis.generator.db.driverClass 變數 if needed
        def driverClass = System.getenv('mybatis.generator.db.driverClass') ?: project.property('mybatis.generator.db.driverClass')
        // 如果沒有提供 driverClass，則嘗試從專案依賴中自動檢測
        if(!driverClass) {
          driverClass = detectJdbcDriverClass(project)
          println "自動檢測到 JDBC 驅動類別: ${driverClass}"
        }
        if (driverClass) {
          fileContent = fileContent.replaceAll('\\$\\{mybatis\\.generator\\.db\\.driverClass\\}', driverClass as String)
        } else {
          println "未檢測到 JDBC 驅動類別 請確保已添加相關依賴。"
        }

        def connectionURL = null
        // 替代變數 mybatis.generator.db.connectionURL
        if( project.hasProperty('mybatis.generator.db.connectionURL')) {
          connectionURL = project.property('mybatis.generator.db.connectionURL')
          fileContent = fileContent.replaceAll('\\$\\{mybatis\\.generator\\.db\\.connectionURL\\}', connectionURL as String)
        } else {
          println "未提供 mybatis.generator.db.connectionURL，請在專案屬性中設定。"
        }

        // 替代變數 mybatis.generator.db.tables
        fileContent = replaceTableMetaInConfig(project, driverClass as String, connectionURL as String, fileContent)

        // 替代變數 mybatis.generator.target.package 變數
        def targetPackage = project.group
        if(project.hasProperty("mybatis.generator.target.package")) {
          targetPackage = project.property("mybatis.generator.target.package")
        }
        fileContent = fileContent.replaceAll('\\$\\{mybatis\\.generator\\.target\\.package\\}', targetPackage as String)


        def targetFile = new File(project.rootDir, "build-tools/mybatis/mybatis-generator-config.xml")
        if (!targetFile.parentFile.exists()) {
          targetFile.parentFile.mkdirs()
        }
        targetFile.text = fileContent
        println "已複製 mybatis-generator-config.xml 到: ${targetFile.absolutePath}"
      }
    }


    // 註冊 mybatisGenerate 任務：用於根據配置檔生成 MyBatis 相關檔案
    project.tasks.register("mybatisGenerate", JavaExec) {
      group = 'MyBatis'
      description = 'Generates MyBatis artifacts based on the configuration file.'
      main = 'org.mybatis.generator.api.ShellRunner'
      classpath = project.sourceSets.main.runtimeClasspath
      args = ['-configfile', 'build-tools/mybatis/mybatis-generator-config.xml', '-overwrite', '-verbose']
      // 設定資料庫連線帳號密碼，從環境變數取得
      systemProperties = [
        'mybatis.generator.db.username': System.getenv('mybatis.generator.db.username'),
        'mybatis.generator.db.password': System.getenv('mybatis.generator.db.password')
      ]

      // 任務執行結束後提示訊息
      doLast {
        println "MyBatis generated successfully."
      }
    }

    // 註冊 mybatisGenerateAndFormat 任務：此任務將在生成 MyBatis 代碼後自動格式化
    project.tasks.register('myBatisGenerateAndFormat') {
      group = 'MyBatis'
      description = 'Generate MyBatis code and format with Spotless.'
      // 依賴 mybatisGenerate 與 spotlessApply 任務，先生成再格式化
      dependsOn 'mybatisGenerate', 'spotlessApply'
    }
  }

  /**
   * 支援的 JDBC 驅動對應表：artifact 名稱 -> driver class
   */
  static def drivers = [
    'mysql-connector-java'      : 'com.mysql.cj.jdbc.Driver',
    'mariadb-java-client'       : 'org.mariadb.jdbc.Driver',
    'postgresql'                : 'org.postgresql.Driver',
    'ojdbc'                     : 'oracle.jdbc.OracleDriver',
    'mssql-jdbc'                : 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
    'sqlite-jdbc'               : 'org.sqlite.JDBC'
  ]

  /**
   * 定義 JDBC 驅動查詢規則，meta.getTables 參數 catalog, schema 的對應
   * 例如：
   * - MySQL/MariaDB: 使用 catalog
   * - PostgreSQL/Oracle/SQLServer: 不使用 catalog，僅使用 schema
   * - SQLite: 不使用 catalog 或 schema
   */
  static def driverQueryRuleUseCatalog = [
    'com.mysql.cj.jdbc.Driver'                    : [
      'catalog': true,  // 使用 catalog
      'schema' : false  // 不使用 schema
    ],
    'org.mariadb.jdbc.Driver'                     : [
      'catalog': true,  // 使用 catalog
      'schema' : false  // 不使用 schema
    ],
    'org.postgresql.Driver'                       : [
      'catalog': false, // 不使用 catalog
      'schema' : true   // 使用 schema
    ],
    'oracle.jdbc.OracleDriver'                    : [
      'catalog': false, // 不使用 catalog
      'schema' : true   // 使用 schema
    ],
    'com.microsoft.sqlserver.jdbc.SQLServerDriver': [
      'catalog': false, // 不使用 catalog
      'schema' : true   // 使用 schema
    ],
    'org.sqlite.JDBC'                             : [
      'catalog': false, // 不使用 catalog
      'schema' : false  // 不使用 schema
    ]
  ]

  /**
   * 檢測專案中使用的 JDBC 驅動類別
   * @param project 專案實例
   * @return 驅動類別名稱或 null
   */
  static def detectJdbcDriverClass(Project project) {
    def found = null
    // 檢查 implementation 中的所有依賴，尋找匹配的 artifact
    project.configurations.runtimeClasspath.allDependencies.each { dep ->
      drivers.each { artifact, driverClass ->
        if (dep.name.contains(artifact)) {
          found = driverClass
        }
      }
    }
    // 回傳找到的 driver class，若無則回傳 null
    return found
  }

  /**
   * 將指定的 JDBC 驅動類別添加到專案的 classpath 中
   * @param project 專案實例
   * @param driverClass JDBC 驅動類別名稱，例如：org.mariadb.jdbc.Driver
   */
  static def addDriverToClasspath(Project project, String driverClass) {
    // 從 driverClass 中提取 artifact 名稱
    def artifactName = drivers.find { it.value == driverClass }?.key
    if (!artifactName) {
      throw new IllegalArgumentException("Unsupported driver class: ${driverClass}. Please ensure it is in the supported drivers list.")
    }
    println "Adding driver to classpath: ${artifactName}"
    def cLoader = Thread.currentThread().contextClassLoader
    def files = project.configurations.runtimeClasspath.files.findAll { it.name.contains(artifactName) }
    files.each { file ->
      def url = file.toURI().toURL()
      println "Adding URL to classpath: ${url}"
      if (cLoader instanceof URLClassLoader) {
        def method = URLClassLoader.class.getDeclaredMethod("addURL", URL)
        method.setAccessible(true)
        method.invoke(cLoader, url)
      } else {
        // JDK 9+ fallback: 建立新的 classloader
        def newCl = new URLClassLoader([url] as URL[], cLoader)
        Thread.currentThread().contextClassLoader = newCl
      }
    }
  }

  /**
   * 組合預設的資料庫定義元素，例如：<table tableName="client"></table>
   * @param tableMeta  TableMeta 物件，包含資料表名稱等資訊
   * @return 組合好的資料表元素字串
   */
  static def buildTableElement(TableMeta tableMeta) {
    // 定義換行字元和縮排
    def newlineChar = "\n\t\t"
    def sb = new StringBuilder()

    // 寫入 table comment
    if(StringUtils.isNotBlank(tableMeta.comment)) {
      sb.append("<!-- ${tableMeta.comment} -->")
      sb.append(newlineChar)
    }

    // 開始組合 table 元素
    sb.append("<table tableName=\"${tableMeta.tableName}\">")
      .append(newlineChar)
    // 針對每個欄位個別處理
    tableMeta.columns.each { col ->
//      sb.append("  <column name=\"${col.name}\" type=\"${col.type}\"")
//      if (col.isPrimaryKey) sb.append(" isPrimaryKey=\"true\"")
//      if (col.isForeignKey) sb.append(" isForeignKey=\"true\"")
//      if (col.comment) sb.append(" comment=\"${col.comment}\"")
//      sb.append("/>\n")
      println("check column: ${col.name}, type: ${col.type}, lenght: ${col.length} nullable: ${col.nullable} isPrimaryKey: ${col.isPrimaryKey}, isForeignKey: ${col.isForeignKey}, comment: ${col.comment}")
    }
//    tableMeta.primaryKeys.each { pk ->
//      sb.append("  <primaryKey columnName=\"${pk}\"/>\n")
//    }
//    tableMeta.foreignKeys.each { fk ->
//      sb.append("  <foreignKey columnName=\"${fk.columnName}\" referenceTable=\"${fk.referenceTable}\" referenceColumn=\"${fk.referenceColumn}\"/>\n")
//    }
    sb.append("</table>")
    return sb.toString()
  }

  /**
   * 從 JDBC 連線 URL 中解析出資料庫 schema 名稱
   * @param url JDBC 連線 URL
   * @return 資料庫 schema 名稱或 null
   */
  static def parseSchemaFromUrl(String url) {
    // MySQL/MariaDB: jdbc:mysql://host:port/dbname
    def mysqlMatcher = url =~ /jdbc:(mysql|mariadb):\/\/[^\/]+\/([^?;]+)/
    if (mysqlMatcher) return mysqlMatcher[0][2]

    // PostgreSQL: jdbc:postgresql://host:port/dbname
    def pgMatcher = url =~ /jdbc:postgresql:\/\/[^\/]+\/([^?;]+)/
    if (pgMatcher) return pgMatcher[0][1]

    // Oracle: jdbc:oracle:thin:@host:port:sid 或 jdbc:oracle:thin:@//host:port/service
    def oracleMatcher = url =~ /jdbc:oracle:thin:@(?:\/\/)?[^:\/]+(?::\d+)?[:\/]([^?;\/]+)/
    if (oracleMatcher) return oracleMatcher[0][1]

    // SQL Server: jdbc:sqlserver://host:port;databaseName=dbname
    def mssqlMatcher = url =~ /jdbc:sqlserver:\/\/[^;]+;databaseName=([^;?]+)/
    if (mssqlMatcher) return mssqlMatcher[0][1]

    // SQLite: jdbc:sqlite:/path/to/dbfile 或 jdbc:sqlite::memory:
    def sqliteMatcher = url =~ /jdbc:sqlite:(.+)/
    if (sqliteMatcher) return sqliteMatcher[0][1]

    return null
  }

  /**
   * 列出資料庫中的所有 TableMeta 物件（包含欄位、主鍵、外鍵等資訊）
   * @param driverClass JDBC 驅動類別名稱，例如：org.mariadb.jdbc.Driver
   * @param url JDBC 連線 URL
   * @param user JDBC 使用者名稱
   * @param password JDBC 密碼
   * @return TableMeta 物件集合
   */
  static List<TableMeta> listTableMeta(Object driverClass, String url, String user, String password) {
    Class.forName(driverClass as String)
    def conn = DriverManager.getConnection(url, user, password)
    def meta = conn.getMetaData()
    // 根據 JDBC 驅動類別決定是否使用 catalog
    def catalog = null
    def schema = null
    def useCatalog = driverQueryRuleUseCatalog[driverClass]?.catalog ?: false
    if(useCatalog) {
      catalog = parseSchemaFromUrl(url) as String
    }
    def useSchema = driverQueryRuleUseCatalog[driverClass]?.schema ?: false
    if(useSchema) {
      schema = parseSchemaFromUrl(url) as String
    }

    println "prepare scan database schema: ${schema} from URL: ${url}"
    println "use catalog: ${useCatalog}, catalog: ${catalog}, useSchema: ${useSchema}, schema: ${schema}"

    def rs = meta.getTables(catalog, schema, "%", ["TABLE"] as String[])
    def tables = []
    while (rs.next()) {
      def tableName = rs.getString("TABLE_NAME")
      def tableMeta = new TableMeta()
      tableMeta.tableName = tableName
      tableMeta.comment = rs.getString("REMARKS") ?: ""
      // 查詢欄位資訊
      def colRs = meta.getColumns(null, schema, tableName, "%")
      while (colRs.next()) {
        def colMeta = new ColumnMeta()
        colMeta.name = colRs.getString("COLUMN_NAME")
        colMeta.type = colRs.getString("TYPE_NAME")
        // 欄位長度
        colMeta.length = colRs.getInt("COLUMN_SIZE")
        // 是否允許為空
        colMeta.nullable = colRs.getInt("NULLABLE") == 1
        colMeta.comment = colRs.getString("REMARKS")
        tableMeta.columns << colMeta
      }
      colRs.close()
      // 查詢主鍵
      def pkRs = meta.getPrimaryKeys(null, schema, tableName)
      while (pkRs.next()) {
        def pkName = pkRs.getString("COLUMN_NAME")
        tableMeta.primaryKeys << pkName
        def pkCol = tableMeta.columns.find { it.name == pkName }
        if (pkCol) pkCol.isPrimaryKey = true
      }
      pkRs.close()
      // 查詢外鍵
      def fkRs = meta.getImportedKeys(null, schema, tableName)
      while (fkRs.next()) {
        def fkMeta = new ForeignKeyMeta()
        fkMeta.columnName = fkRs.getString("FKCOLUMN_NAME")
        fkMeta.referenceTable = fkRs.getString("PKTABLE_NAME")
        fkMeta.referenceColumn = fkRs.getString("PKCOLUMN_NAME")
        tableMeta.foreignKeys << fkMeta
        def fkCol = tableMeta.columns.find { it.name == fkMeta.columnName }
        if (fkCol) fkCol.isForeignKey = true
      }
      fkRs.close()
      tables << tableMeta
    }
    rs.close()
    conn.close()
    return tables
  }

  /**
   * 替換 mybatis.generator.db.tables 變數
   * @param project 專案實例
   * @param driverClass JDBC 驅動類別名稱，例如：org.mariadb.jdbc.Driver
   * @param connectionURL JDBC 連線 URL，例如：jdbc:mariadb://localhost:3306/mydb
   * @param fileContent mybatis-generator-config.xml 檔案內容
   * @return 替換後的檔案內容
   */
  static String replaceTableMetaInConfig(Project project, String driverClass, String connectionURL, String fileContent) {
    // 如果有提供 driverClass, connectionUrl, username, password，則進行資料庫連線並抓所有的資料表名稱
    def username = System.getenv('mybatis.generator.db.username')
    def password = System.getenv('mybatis.generator.db.password')
    if(StringUtils.isAnyBlank(driverClass, username, password, connectionURL)) {
      println "未提供完整的資料庫連線資訊，無法替換 mybatis.generator.db.tables。"
      return fileContent
    }

    // 確保 driverClass 已經在 classpath 中
    addDriverToClasspath(project, driverClass as String)

    // 從資料庫獲取所有資料表名稱
    def tables = listTableMeta(driverClass, connectionURL as String, username, password)
    if(!tables || tables.isEmpty()) {
      println "未找到任何資料表，請檢查資料庫連線資訊或資料庫是否有資料表。"
      return fileContent
    }

    // 組合預設的資料庫定義，例如: <table tableName="client"></table>
    def tablesString = tables.collect { tableMetaItem ->
      buildTableElement(tableMetaItem)
    }.join('\n\n\t\t')
    // 替代變數 <!-- ${mybatis.generator.db.tables} -->
    println "替換 mybatis.generator.db.tables ..."
    return fileContent.replaceAll('<!-- \\$\\{mybatis\\.generator\\.db\\.tables\\} -->', tablesString)
  }

  // ~ ----------------------------------------------------------
}
