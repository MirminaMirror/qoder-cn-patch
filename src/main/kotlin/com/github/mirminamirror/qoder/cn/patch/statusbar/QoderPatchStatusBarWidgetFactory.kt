package com.github.mirminamirror.qoder.cn.patch.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

/**
 * 状态栏 Widget 工厂：注册补丁的"Qoder Completion"设置入口。
 *
 * Widget 实现见 [QoderPatchStatusBarWidget]；默认可用，用户可在状态栏项管理中显式隐藏。
 */
class QoderPatchStatusBarWidgetFactory : StatusBarWidgetFactory {
  
  override fun getId(): String = "QoderPatchStatusBarWidgetFactory"
  
  override fun getDisplayName(): String = "Qoder Completion Settings"
  
  override fun isAvailable(project: Project): Boolean = true
  
  override fun createWidget(project: Project): StatusBarWidget = QoderPatchStatusBarWidget(project)
  
  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
  
  override fun disposeWidget(widget: StatusBarWidget) {
    widget.dispose()
  }
}
