import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
  id("org.jetbrains.changelog")
}

kotlin {
  jvmToolchain(17)
}

intellijPlatform {
  // buildSearchableOptions = false
  pluginConfiguration {
    ideaVersion {
      sinceBuild.set("231")
      untilBuild.set(provider { null })
    }
  }
  
  pluginVerification {
    ides {
      create(IntelliJPlatformType.IntellijIdeaCommunity, "2023.1")
    }
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  
  intellijPlatform {
    intellijIdeaCommunity("2023.3")
    plugin("com.alibabacloud.intellij.cosy", "2026.814.61156701")
    testFramework(TestFrameworkType.Platform)
  }
}
