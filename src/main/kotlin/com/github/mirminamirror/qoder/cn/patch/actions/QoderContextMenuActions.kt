package com.github.mirminamirror.qoder.cn.patch.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAware
/**
 * 委托官方已注册 Action 的右键菜单动作基类。
 *
 * 通过 [targetActionId] 从 `ActionManager` 解析官方目标动作并转发 update / actionPerformed；
 * 官方动作不存在（如官方插件卸载或版本不匹配）时菜单项自动隐藏。
 */
abstract class BaseQoderDelegatingAction(
  private val targetActionId: String,
  private val defaultTitle: String,
) : AnAction(), DumbAware {
  
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  
  override fun update(e: AnActionEvent) {
    val target = ActionManager.getInstance().getAction(targetActionId)
    if (target != null) {
      ActionUtil.performDumbAwareUpdate(target, e, false)
      e.presentation.text = defaultTitle
    } else {
      e.presentation.isEnabledAndVisible = false
    }
  }
  
  override fun actionPerformed(e: AnActionEvent) {
    val target = ActionManager.getInstance().getAction(targetActionId)
    if (target != null) {
      ActionUtil.performActionDumbAwareWithCallbacks(target, e)
    }
  }
}

class OptimizeCodeAction : BaseQoderDelegatingAction(
  "com.alibabacloud.intellij.cosy.TriggerQoderOptimizeCodeGenerationAction",
  "优化代码 (Optimize Code)"
)

class GenerateCommentAction : BaseQoderDelegatingAction(
  "com.alibabacloud.intellij.cosy.TriggerQoderCodeGenerateCommentGenerationAction",
  "生成代码注释 (Generate Comments)"
)

class GenerateTestCaseAction : BaseQoderDelegatingAction(
  "com.alibabacloud.intellij.cosy.TriggerQoderTestcaseGenerationAction",
  "生成单元测试 (Generate Test Cases)"
)

class ExplainCodeAction : BaseQoderDelegatingAction(
  "com.alibabacloud.intellij.cosy.TriggerQoderExplainCodeGenerationAction",
  "解释代码 (Explain Code)"
)
