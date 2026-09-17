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
      val isRecommendedVerification = providers.gradleProperty("recommendedVerification")
        .map { it.isBlank() || it.toBoolean() }
        .orElse(providers.environmentVariable("GITHUB_ACTIONS").map { it.toBoolean() })
        .getOrElse(false)

      if (isRecommendedVerification) {
        recommended()
      } else {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2023.3")
        create(IntelliJPlatformType.IntellijIdeaUltimate, "2026.2.3")
      }
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

tasks.verifyPlugin {
  offline.convention(!providers.environmentVariable("GITHUB_ACTIONS").isPresent)
  externalPrefixes.add("com.alibabacloud")
}
