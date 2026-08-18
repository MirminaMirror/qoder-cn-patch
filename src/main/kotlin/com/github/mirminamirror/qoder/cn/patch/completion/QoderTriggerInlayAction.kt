package com.github.mirminamirror.qoder.cn.patch.completion

import com.alibabacloud.intellij.qoder.core.Cosy
import com.alibabacloud.intellij.qoder.editor.CosyInlayManager
import com.alibabacloud.intellij.qoder.editor.inline.InlineEditUtil
import com.alibabacloud.intellij.qoder.editor.model.CompletionTriggerConfig
import com.alibabacloud.intellij.qoder.editor.model.InlayDisposeEventEnum
import com.alibabacloud.intellij.qoder.editor.model.InlayTriggerEventEnum
import com.alibabacloud.intellij.qoder.editor.request.InlayPreviewRequest
import com.alibabacloud.intellij.qoder.search.enums.CompletionTriggerModeEnum
import com.alibabacloud.intellij.qoder.ui.config.CosyPersistentSetting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * 手动触发经典行内补全的 Action，快捷键 `Alt + C`。
 *
 * 官方 `QoderTriggerInlayCompletionAction`（Alt+P）的 handler 完整保留，但其调用链
 * `editorChanged(MANUAL)` 在 2026.814 中被强制转发到 `inlineEdit(TYPING)` 并被 NEXT 开关门控拦截，
 * 因此本 Action 绕过 `editorChanged` 直接调用 `InlayPreviewRequest.generate(MANUAL)`。
 *
 * 该 Action 表达用户显式意图，不受补丁总开关与 NEXT 开关限制；执行前先清理既有浮层，
 * 避免与官方 NEXT 预测浮层叠加。
 */
class QoderTriggerInlayAction : AnAction(), DumbAware {
  
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  
  override fun update(e: AnActionEvent) {
    // 仅做轻量检查：历史上曾在此调用 checkCosy，实测在 BGT 单次耗时 2~3 秒，
    // 会导致菜单与快捷键响应明显卡顿；服务可用性校验延后到 actionPerformed。
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isEnabledAndVisible =
      e.project != null && editor != null && CosyInlayManager.getInstance().isAvailable(editor)
    e.presentation.text = "Trigger Qoder Inline Completion"
  }
  
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    if (!Cosy.INSTANCE.checkCosy(project, true)) return
    
    InlineEditUtil.disposeAllInline(editor, InlayDisposeEventEnum.TRIGGER_ACTION)
    QoderCaretColorManager.applyCompletionCaret(editor)
    val config = CompletionTriggerConfig.defaultConfig(InlayTriggerEventEnum.MANUAL_TRIGGER)
    InlayPreviewRequest.build().generate(config, editor, CompletionTriggerModeEnum.MANUAL)
    
    CosyPersistentSetting.getInstance().state?.isShowInlineTriggerTips = false
  }
}
