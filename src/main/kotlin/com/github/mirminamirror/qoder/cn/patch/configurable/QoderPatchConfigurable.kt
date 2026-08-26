package com.github.mirminamirror.qoder.cn.patch.configurable

import com.alibabacloud.intellij.qoder.common.CosySetting
import com.alibabacloud.intellij.qoder.core.Cosy
import com.alibabacloud.intellij.qoder.search.enums.CompletionGenerateLengthLevelEnum
import com.alibabacloud.intellij.qoder.ui.config.CosyPersistentSetting
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ColorPanel
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.*
import java.awt.Color

/**
 * 补丁设置面板 Configurable，挂载在官方 `qodercn.settings` 设置节点之下。
 *
 * 使用 Kotlin UI DSL 构建界面并直接绑定数据源：
 * - 官方补全 / NES 参数绑定到 `CosyPersistentSetting` 的 `CosySetting`，`apply` 后经
 *   `Cosy.updateConfig` 推送到语言服务后端；
 * - 补丁兜底总开关绑定到 [QoderPatchState]。
 *
 * `isModified` / `apply` / `reset` 由 `BoundConfigurable` 依据全部属性绑定自动推导，
 * 无需手写表单快照比对。
 */
internal class QoderPatchConfigurable : BoundSearchableConfigurable(
  ConfigurableBundle.message("configurableDisplayName"),
  "qoder.cn.patch.configurable",
  "com.github.mirminamirror.qoder.cn.patch.configurable.QoderPatchConfigurable"
) {
  
  /** 官方设置实体快照；面板生命周期内复用同一实例，绑定直接读写该实例。 */
  private val setting: CosySetting = CosyPersistentSetting.getInstance().state ?: CosySetting()
  
  /**
   * 自动 / 手动补全长度的可选项。
   *
   * @param key 官方 `generateLength` 取值（`CompletionGenerateLengthLevelEnum.label`）
   * @param label 展示文案（国际化）
   */
  private data class LengthOption(val key: String, val label: String) {
    override fun toString(): String = label
  }
  
  private val autoLengthOptions = listOf(
    LengthOption(CompletionGenerateLengthLevelEnum.LINE_LEVEL.label, ConfigurableBundle.message("length.level.line")),
    LengthOption(CompletionGenerateLengthLevelEnum.LEVEL_1.label, ConfigurableBundle.message("length.level.short")),
    LengthOption(CompletionGenerateLengthLevelEnum.LEVEL_2.label, ConfigurableBundle.message("length.level.long")),
    LengthOption(CompletionGenerateLengthLevelEnum.LEVEL_3.label, ConfigurableBundle.message("length.level.xlong")),
    LengthOption(CompletionGenerateLengthLevelEnum.NO.label, ConfigurableBundle.message("length.level.off"))
  )
  
  private val manualLengthOptions = autoLengthOptions.dropLast(1)
  
  override fun createPanel(): DialogPanel = panel {
    val patchState = QoderPatchState.getInstance()
    group(ConfigurableBundle.message("patch.group.title")) {
      row {
        checkBox(ConfigurableBundle.message("patch.enableClassicCompletion"))
          .bindSelected(patchState::classicCompletionEnabled)
      }
    }
    
    val caretColorEnabled = AtomicBooleanProperty(patchState.completionCaretColorEnabled)
    group(ConfigurableBundle.message("caret.group.title")) {
      row {
        checkBox(ConfigurableBundle.message("caret.enableColor"))
          .bindSelected(patchState::completionCaretColorEnabled)
          .onChanged { caretColorEnabled.set(it.isSelected) }
      }
      row(ConfigurableBundle.message("caret.color")) {
        cell(ColorPanel())
          .bind(
            { it.selectedColor },
            { panel, color -> panel.selectedColor = color },
            MutableProperty(
              { parseCaretColor(patchState.completionCaretColorHex) },
              { color -> patchState.completionCaretColorHex = toHexColor(color) }
            )
          )
          .enabledIf(caretColorEnabled)
      }
      row {
        checkBox(ConfigurableBundle.message("caret.bold"))
          .bindSelected(patchState::completionCaretBold)
          .enabledIf(caretColorEnabled)
      }
    }
    
    
    val cloudEnabled = AtomicBooleanProperty(setting.parameter.cloud.enable == true)
    group(ConfigurableBundle.message("completion.group.title")) {
      row {
        checkBox(ConfigurableBundle.message("completion.enableCloudModel"))
          .bindSelected(
            { setting.parameter.cloud.enable == true },
            { setting.parameter.cloud.enable = it }
          )
          .onChanged { cloudEnabled.set(it.isSelected) }
      }
      row(ConfigurableBundle.message("completion.autoTriggerLength")) {
        comboBox(autoLengthOptions)
          .bindItem(
            { autoLengthOptions.firstOrNull { it.key == currentAutoLengthKey() } },
            { option ->
              if (option != null) {
                setting.parameter.cloud.autoTrigger?.enable =
                  option.key != CompletionGenerateLengthLevelEnum.NO.label
                if (option.key != CompletionGenerateLengthLevelEnum.NO.label) {
                  setting.parameter.cloud.autoTrigger?.generateLength = option.key
                }
              }
            }
          ).enabledIf(cloudEnabled)
      }
      row(ConfigurableBundle.message("completion.manualTriggerLength")) {
        comboBox(manualLengthOptions)
          .bindItem(
            { manualLengthOptions.firstOrNull { it.key == currentManualLengthKey() } },
            { option ->
              if (option != null) {
                setting.parameter.cloud.manualTrigger?.generateLength = option.key
              }
            }
          ).enabledIf(cloudEnabled)
        label(ConfigurableBundle.message("completion.manualShortcutHint"))
      }
      row {
        checkBox(ConfigurableBundle.message("completion.showInlineWhenIdeCompletion"))
          .bindSelected(
            { setting.parameter.cloud.isShowInlineWhenIDECompletion },
            { setting.parameter.cloud.isShowInlineWhenIDECompletion = it }
          ).enabledIf(cloudEnabled)
      }
      row(ConfigurableBundle.message("completion.disableLanguages")) {
        textField()
          .bindText(
            { setting.parameter.cloud.disableLanguages?.joinToString(",") ?: "" },
            { text ->
              setting.parameter.cloud.disableLanguages =
                text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
          ).align(AlignX.FILL)
          .enabledIf(cloudEnabled)
      }
    }
    
    val nesEnabled = AtomicBooleanProperty(setting.nesConfig?.enabled == true)
    group(ConfigurableBundle.message("nes.group.title")) {
      row {
        checkBox(ConfigurableBundle.message("nes.enable"))
          .bindSelected(
            { setting.nesConfig?.enabled == true },
            { setting.nesConfig?.enabled = it }
          )
          .onChanged { nesEnabled.set(it.isSelected) }
      }
      buttonsGroup(ConfigurableBundle.message("nes.suggestMode")) {
        row {
          radioButton(ConfigurableBundle.message("nes.suggestMode.inline"), QoderNesMode.INLINE)
          radioButton(ConfigurableBundle.message("nes.suggestMode.sideBySide"), QoderNesMode.SIDE_BY_SIDE)
          radioButton(ConfigurableBundle.message("nes.suggestMode.auto"), QoderNesMode.AUTO)
        }
      }.bind(
        MutableProperty(
          { QoderNesMode.fromName(setting.nesConfig?.suggestMode) },
          { setting.nesConfig?.suggestMode = it.name }
        ),
        QoderNesMode::class.java
      ).enabledIf(nesEnabled)
      row {
        checkBox(ConfigurableBundle.message("nes.codeShift"))
          .bindSelected(
            { setting.nesConfig?.codeShiftEnabled == true },
            { setting.nesConfig?.codeShiftEnabled = it }
          )
          .enabledIf(nesEnabled)
        comment(ConfigurableBundle.message("nes.codeShift.hint"))
      }
    }
  }
  
  /** `apply` 完成属性绑定写回后，把官方参数实时推送到语言服务后端。 */
  override fun apply() {
    super.apply()
    Cosy.INSTANCE.updateConfig(setting.parameter)
  }
  
  /** 当前自动触发长度选项键：关闭时映射为 NO，缺省时回退 LEVEL_1。 */
  private fun currentAutoLengthKey(): String {
    val autoTrigger = setting.parameter.cloud.autoTrigger
    return when {
      autoTrigger?.enable == false -> CompletionGenerateLengthLevelEnum.NO.label
      else -> autoTrigger?.generateLength ?: CompletionGenerateLengthLevelEnum.LEVEL_1.label
    }
  }
  
  /** 当前手动触发长度选项键；缺省时回退 LEVEL_2。 */
  private fun currentManualLengthKey(): String =
    setting.parameter.cloud.manualTrigger?.generateLength
    ?: CompletionGenerateLengthLevelEnum.LEVEL_2.label
}

/** 将十六进制色值解析为 [Color]，异常时回退到默认粉色。 */
private fun parseCaretColor(hex: String): Color =
  ColorUtil.fromHex(hex, JBColor(0xFF79C6, 0xFF79C6)) ?: JBColor(0xFF79C6, 0xFF79C6)

/** 将 [Color] 转换为 HTML 十六进制颜色字符串。 */
private fun toHexColor(color: Color?): String =
  if (color != null) ColorUtil.toHtmlColor(color) else "#FF79C6"

/** NES 推荐方式的展示枚举，与官方 `QoderNesConfig.SuggestMode` 名称一一对应。 */
private enum class QoderNesMode {
  INLINE, SIDE_BY_SIDE, AUTO;
  
  companion object {
    fun fromName(name: String?): QoderNesMode =
      values().firstOrNull { it.name == name } ?: AUTO
  }
}
