# MyBatis Generator Gradle Plugin

## 簡介
本插件用於自動化 MyBatis Generator 相關任務，支援多種資料庫，並可自動產生、格式化 MyBatis 代碼。

## 功能
- 依據資料庫結構自動產生 MyBatis 相關程式碼
- 支援 Spotless Java 格式化（Eclipse formatter）
- 自動偵測 JDBC 驅動類別
- 依據資料庫自動產生 table 定義
- 支援主鍵、外鍵、欄位型別、備註等資訊

## 安裝
在 `build.gradle` 加入：

```groovy
plugins {
  id 'com.riease.mybatis-generator' version '1.15-SNAPSHOT'
}

repositories {
  mavenCentral()
  maven { url 'https://maven.pkg.github.com/riease/mybatis-generator-gradle-plugin' }
}
```

## 專案依賴
```groovy
dependencies {
  implementation 'org.apache.groovy:groovy:4.0.14'
  implementation 'org.apache.commons:commons-lang3:3.18.0'
}
```

## 使用方式
1. 設定資料庫連線資訊（可用環境變數或 gradle property）：
   - `mybatis.generator.db.driverClass`
   - `mybatis.generator.db.connectionURL`
   - `mybatis.generator.db.username`
   - `mybatis.generator.db.password`
2. 執行任務：
   - `copyMyBatisGeneratorConfig`：複製並自動產生 mybatis-generator-config.xml
   - `mybatisGenerate`：根據 config 產生 MyBatis 代碼
   - `myBatisGenerateAndFormat`：產生並格式化 MyBatis 代碼

## 主要任務說明
- `copyMyBatisGeneratorConfig`：
  - 依據資料庫結構自動產生 table 定義
  - 支援主鍵、外鍵、欄位型別、備註
- `mybatisGenerate`：
  - 執行 MyBatis Generator，產生 entity、mapper 等檔案，產出檔案會在 `${group}/dao/` 目錄下，例如 `com/example/dao/entity` 與 `com/example/dao/mapper` 
- `myBatisGenerateAndFormat`：
  - 產生Java檔案並自動執行 code 格式化，使用 Spotless Java 格式化（Eclipse formatter）

## 進階設定
- 可自訂目標 package：`mybatis.generator.target.package`
- 支援多種資料庫（MySQL、MariaDB、PostgreSQL、Oracle、SQLServer、SQLite）

## 範例
```shell
gradle copyMyBatisGeneratorConfig
# 產生 config 檔

gradle mybatisGenerate
# 產生 MyBatis 代碼

gradle myBatisGenerateAndFormat
# 產生並格式化
```


## 授權
MIT License

