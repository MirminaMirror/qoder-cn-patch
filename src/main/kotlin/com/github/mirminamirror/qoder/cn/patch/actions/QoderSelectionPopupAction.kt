package com.github.mirminamirror.qoder.cn.patch.actions

import com.alibabacloud.intellij.qoder.editor.InlayCompletionHintFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * 框选代码后在光标处显示 Qoder 对话按钮的 Action，挂载于编辑器右键菜单。
 *
 * 对应官方 `CosySelectionPopupAction`（类在 2026.814 中完整保留，但其在 plugin.xml 的
 * 注册被官方注释禁用，旁注"去掉框选代码弹出对话功能按钮"）。该官方 Action 未注册到
 * ActionManager，无法委托，故此处直接调用其核心实现
 * `InlayCompletionHintFactory.showChatButtonAtCaret`（public static，2026.814 保留）。
 *
 * 仅当选区存在且非空白时生效；无选区或无编辑器时菜单项隐藏。
 */
class QoderSelectionPopupAction : AnAction(), DumbAware {
  
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  
  override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val selection = editor?.selectionModel
    e.presentation.isEnabledAndVisible =
      editor != null && selection != null &&
      selection.hasSelection() && !selection.selectedText.isNullOrBlank()
  }
  
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    InlayCompletionHintFactory.showChatButtonAtCaret(editor)
  }
}
