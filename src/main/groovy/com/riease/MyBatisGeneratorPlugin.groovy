package com.riease


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

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
    project.tasks.register("copyMyBatisGeneratorConfig", JavaExec) {
      group = 'MyBatis'
      description = 'Copies mybatis-generator-config.xml to the project build-tools directory.'
      main = 'com.riease.mybatis.CopyMyBatisGeneratorConfigMain'
      classpath = project.sourceSets.main.runtimeClasspath
      // 設定參數，讓任務可以被執行
      args = []
      // 設定所使用的參數
      systemProperties = [
        'mybatis.generator.copy.overwrite': project.findProperty('mybatis.generator.copy.overwrite'),
        'mybatis.generator.append.tables': project.findProperty('mybatis.generator.append.tables'),
        'mybatis.generator.target.package': project.group,
        'mybatis.generator.db.driverClass': project.findProperty('mybatis.generator.db.driverClass'),
        'mybatis.generator.db.connectionURL': project.findProperty('mybatis.generator.db.connectionURL'),
        'mybatis.generator.db.username': System.getenv('mybatis.generator.db.username'),
        'mybatis.generator.db.password': System.getenv('mybatis.generator.db.password')
      ]
      doFirst {
        println "MyBatisGeneratorPlugin-${project.version}: copyMyBatisGeneratorConfig task is starting..."
        println "Copy mybatis-generator-config.xml to project"
      }
      // 任務執行結束後提示訊息
      doLast {
        println "MyBatis generator config file copied successfully."
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

  // ~ ----------------------------------------------------------
}
