import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
  id("org.jetbrains.changelog")
}

kotlin {
  jvmToolchain(21)
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  implementation("net.bytebuddy:byte-buddy:1.18.11")
  implementation("net.bytebuddy:byte-buddy-agent:1.18.11")
  
  
  intellijPlatform {
    intellijIdea("2026.2.1")
    plugin("com.alibabacloud.intellij.cosy", "2026.814.61156701")
    testFramework(TestFrameworkType.Platform)
  }
}

intellijPlatform {
  buildSearchableOptions = false
}
