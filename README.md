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

若要在其他專案中引用本 plugin，請務必在 `settings.gradle` 檔案中加入下列 repository 設定，確保 Gradle 能正確下載相依套件：

```groovy
pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()

    // GitHub Packages repository for MyBatis Generator Gradle Plugin
    // 確保已設定環境變數 github.package.user 與 github.package
    maven {
      url = uri("https://maven.pkg.github.com/riease/mybatis-generator-gradle-plugin")
      credentials {
        username = System.getenv("github.package.user")
        password = System.getenv("github.package.token")
      }
    }
  }
}
```

在 `build.gradle` 加入：

```groovy
plugins {
  id 'com.riease.mybatis-generator' version '1.17-SNAPSHOT'
}

dependencies {
  implementation 'com.riease:mybatis-generator-gradle-plugin:1.17-SNAPSHOT'
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/riease/mybatis-generator-gradle-plugin")
    credentials {
      username = project.findProperty("github.package.user") ?: System.getenv("github.package.user")
      password = project.findProperty("github.package.token") ?: System.getenv("github.package.token")
    }
  }
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
### 參數說明  
本插件提供了多個參數設定，使用者可透過 `gradle.properties` 或環境變數進行配置。
  - `mybatis.generator.db.driverClass`: 資料庫驅動類別
  - `mybatis.generator.db.connectionURL`: 資料庫連線 URL
  - `mybatis.generator.copy.overwrite`：是否覆蓋已存在的 mybatis-generator-config.xml
  - `mybatis.generator.append.tables`：是否以 append 方式加入 table 定義


設定資料庫機敏參數（只可用環境變數）： 
  - `mybatis.generator.db.username`: 資料庫使用者名稱
  - `mybatis.generator.db.password`: 資料庫密碼


### 主要任務說明

- `copyMyBatisGeneratorConfig`：
  - 依據資料庫結構自動產生 table 定義
  - 支援主鍵、外鍵、欄位型別、備註
- `mybatisGenerate`：
  - 執行 MyBatis Generator，產生 entity、mapper 等檔案，產出檔案會在 `${group}/dao/` 目錄下，例如 `com/example/dao/entity` 與
    `com/example/dao/mapper`
- `myBatisGenerateAndFormat`：
  - 產生Java檔案並自動執行 code 格式化，使用 Spotless Java 格式化（Eclipse formatter）

### 進階設定

- 支援多種資料庫（MySQL、MariaDB、PostgreSQL、Oracle、SQLServer、SQLite）
- 可設定 `mybatis.generator.copy.overwrite` 參數：
  - 預設為 false，若設為 true，則複製 mybatis-generator-config.xml 時會覆蓋已存在的檔案。
- 可設定 `mybatis.generator.append.tables` 參數：
  - 預設為 true，產生 table 定義時會以 append 方式加入，不會覆蓋原有內容，若設定為 false 則不處理。

## 執行 task 範例

```shell
gradle copyMyBatisGeneratorConfig
# 產生 config 檔

gradle mybatisGenerate
# 產生 MyBatis 代碼

gradle myBatisGenerateAndFormat
# 產生並格式化
```

## MyBatis Generator Config 範本檔案說明

控制資料庫欄位對應到 Java 型別的規則

* forceBigDecimals：true 時，所有 DECIMAL/NUMERIC 欄位都用 BigDecimal，否則可能用 Long、Integer 等。
* useJSR310Types：true 時，日期型別用 Java 8 的 LocalDate、LocalDateTime 等（JSR-310），否則用 java.util.Date

```xml

<javaTypeResolver>
  <!-- 強制使用 BigDecimal 作為 Java 類型 -->
  <property name="forceBigDecimals" value="true"/>
  <!-- 使用 JSR-310 (Java 8) 日期類型 -->
  <property name="useJSR310Types" value="true"/>
</javaTypeResolver>
```

用來控制生成程式碼時的註釋行為
主要用途：  
決定是否在生成的 Java 類、Mapper、XML 等檔案中加入註釋。
可自訂註釋內容，例如是否包含欄位備註、是否顯示日期等。
常用屬性（以 property 方式設定）：

* addRemarkComments：true 時，會將資料庫欄位的備註（remarks）加入到 Java 屬性註釋。
* suppressDate：true 時，生成的註釋不包含日期。
* suppressAllComments：true 時，完全不生成註釋

```xml

<commentGenerator>
  <!-- 添加註釋到生成的代碼中 -->
  <property name="addRemarkComments" value="true"/>
  <!-- 抑制日期的生成 -->
  <property name="suppressDate" value="true"/>
</commentGenerator>
```

## 授權

MIT License
