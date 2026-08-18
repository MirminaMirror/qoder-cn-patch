package com.github.mirminamirror.qoder.cn.patch.statusbar

import com.github.mirminamirror.qoder.cn.patch.configurable.QoderPatchConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

/**
 * 状态栏"Qoder Completion"入口 Widget，点击打开补丁的补全与 NES 设置面板。
 *
 * 对应官方 3.3.5 的 `CosyCompletionTitleDisplayAction`（状态栏文字 "Qoder Completion
 * Settings"，点击弹出云端补全快捷设置）；2026.814 将该 Action 连同类一起删除，
 * 本 Widget 提供等价的设置直达入口，目标页为本插件的 [QoderPatchConfigurable]。
 *
 * 继承 `EditorBasedWidget`：仅在有可用编辑器上下文时显示，无打开文件时自动隐藏。
 */
class QoderPatchStatusBarWidget(project: Project) :
  EditorBasedWidget(project), StatusBarWidget.TextPresentation {
  
  override fun getAlignment(): Float = Component.CENTER_ALIGNMENT
  
  override fun ID(): String = "QoderPatchStatusBarWidget"
  
  override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
  
  override fun getText(): String = "Qoder Completion"
  
  override fun getTooltipText(): String = "点击打开 Qoder 补全与 NES 设置（qoder-cn-patch）"
  
  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, QoderPatchConfigurable::class.java)
  }
  
  override fun install(statusBar: StatusBar) {
    // 文字型 Widget 无需额外安装逻辑
  }
}
