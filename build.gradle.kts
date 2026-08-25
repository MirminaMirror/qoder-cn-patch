import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
  id("org.jetbrains.changelog")
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  
  intellijPlatform {
    intellijIdea("2026.2.1")
    plugin("com.alibabacloud.intellij.cosy", "2026.814.61156701")
    testFramework(TestFrameworkType.Platform)
  }
}
