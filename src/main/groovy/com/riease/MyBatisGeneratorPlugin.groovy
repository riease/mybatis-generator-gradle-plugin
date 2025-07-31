package com.riease

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

class MyBatisGeneratorPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    // Apply the plugin to the project
    project.plugins.apply('com.diffplug.spotless')

    // Configure Spotless for MyBatis generated files
    project.tasks.register("mybatisGenerate", JavaExec) {
      group = 'MyBatis'
      description = 'Generates MyBatis artifacts based on the configuration file.'
      main = 'org.mybatis.generator.api.ShellRunner'
      classpath = project.sourceSets.main.runtimeClasspath
      args = ['-configfile', 'src/main/resources/mybatis/generatorConfig.xml', '-overwrite', '-verbose']
      systemProperties = [
        'mybatis.generator.db.user': System.getenv('mybatis.generator.db.user'),
        'mybatis.generator.db.password': System.getenv('mybatis.generator.db.password')
      ]

      doLast {
        println "MyBatis generated successfully."
      }
    }

    // Configure Spotless to format MyBatis generated Java files
    project.tasks.register('myBatisGenerateAndFormat') {
      group = 'MyBatis'
      description = 'Generate MyBatis code and format with Spotless.'
      dependsOn 'mybatisGenerate', 'spotlessApply'
    }
  }
}
