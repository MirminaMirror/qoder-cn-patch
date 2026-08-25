package com.github.mirminamirror.qoder.cn.patch.completion

import com.alibabacloud.intellij.qoder.editor.CosyInlayManager
import com.alibabacloud.intellij.qoder.editor.inline.InlineEditUtil
import com.alibabacloud.intellij.qoder.editor.model.CompletionTriggerConfig
import com.alibabacloud.intellij.qoder.editor.model.InlayDisposeEventEnum
import com.alibabacloud.intellij.qoder.editor.model.InlayTriggerEventEnum
import com.alibabacloud.intellij.qoder.editor.request.InlayPreviewRequest
import com.alibabacloud.intellij.qoder.editor.request.NextCompletionRequest
import com.alibabacloud.intellij.qoder.search.enums.CompletionTriggerModeEnum
import com.alibabacloud.intellij.qoder.util.CompletionUtil
import com.github.mirminamirror.qoder.cn.patch.configurable.QoderPatchState
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.application

/**
 * 经典行内补全兜底监听器，按项目级别（[Project]）在 `plugin.xml` 中注册为 `CommandListener`。
 *
 * ### 1. 背景与职责
 * 官方 Qoder 在升级到 2026.814 版本后，重构了行内补全调度架构，将传统的打字自动补全与 Tab 采纳后的
 * 下一行预测强制导向 NEXT 编码预测链路（`InlineEditUtil.isEnableNEXT()`）。当用户主动关闭 NEXT 预测时，
 * 官方原有经典补全链路被意外中断。本监听器精准还原官方 3.3.5 版本中 `CosyCommandListener.triggerCompletion`
 * 的经典调度语义，为关闭 NEXT 的用户提供稳定可靠的打字自动补全兜底。
 *
 * ### 2. 互斥与门控机制
 * - **NEXT 互斥**：仅当补丁总开关开启且官方 NEXT 编码预测处于关闭状态时生效（[isClassicFallbackActive]）。
 *   若官方 NEXT 开启，官方自身的 `triggerCompletionNesCombination` 链路完全自洽，本监听器必须保持静默，
 *   严防产生双重请求与双重行内 Inlay 渲染。
 * - **模板规避**：若当前编辑器存在活动的 Live Template 交互（如代码模板占位符跳转），自动放弃触发，
 *   避免干扰用户的模板输入流程。
 * - **选区判定**：若命令起始时存在文本选区，严格对齐官方规范不触发自动补全。
 *
 * ### 3. 恢复的两条关键官方链路
 * - **Tab 采纳后的连环预测**：在捕获到采纳命令（[APPLY_COMPLETION_COMMAND]）后，异步调度 [NextCompletionRequest.triggerNext]，
 *   实现“采纳一行、即刻预测下一行”的无缝连续编码体验，并同步激活光标高亮；
 * - **光标移动预热与清理**：在捕获到纯光标移动命令后，调用 [CompletionUtil.triggerCursorPreCompletion] 预热缓存，
 *   并清理过期的行内浮层、复原原生光标颜色。
 *
 * ### 4. 线程与生命周期模型
 * - 所有回调方法（[commandStarted]、[commandFinished]）均由 IntelliJ 命令分发管道在 EDT 线程同步回调；
 * - 本类为有状态监听器，内部维护命令起始时的快照状态，快照仅在单次 `commandStarted -> commandFinished` 配对内有效。
 */
class QoderClassicCompletionListener(private val project: Project) : CommandListener {
  
  /** 命令起始快照：文档 modificationStamp，用于判定命令期间是否发生真实修改。 */
  private var startStamp: Long = -1L
  
  /** 命令起始快照：光标偏移，用于判定纯光标移动命令。 */
  private var startOffset: Int = -1
  
  /** 命令起始快照：选区文本；命令起始已有选区时不触发补全（与官方行为一致）。 */
  private var startSelection: String? = null
  
  /** 快照有效性标记；commandStarted 未观察到当前编辑器时，跳过依赖快照的触发路径。 */
  private var snapshotValid: Boolean = false
  
  /**
   * 命令即将开始执行时的回调。
   *
   * 在文档内容和光标位置被命令修改前捕获环境快照：
   * 1. 记录文档修改时间戳（[startStamp]），用于在命令结束时比对文档是否发生实际增删；
   * 2. 记录光标初始偏移（[startOffset]），用于在命令结束时判断是否属于纯光标位移；
   * 3. 记录选区状态（[startSelection]），若起始已有高亮选区，命令结束后放弃补全。
   */
  override fun commandStarted(event: CommandEvent) {
    val editor = matchingMainEditorFor(event)
    if (editor == null) {
      snapshotValid = false
      return
    }
    startStamp = editor.document.modificationStamp
    startOffset = editor.caretModel.offset
    startSelection = editor.selectionModel.selectedText
    snapshotValid = true
  }
  
  /**
   * 命令执行完毕后的调度核心。
   *
   * 采用顶级门禁过滤无效或静默场景，并使用纯正向条件的自顶向下分支（`when`）进行业务动作分流：
   * - **分支 1（Tab 采纳连环预测）**：捕获采纳写命令，调度下一行预测并高亮光标；
   * - **分支 2（打字触发补全）**：常规文本输入且满足复合触发条件时，发起自动预测；
   * - **分支 3（纯光标位移）**：光标坐标发生位移时，预热下一位置并复原光标原生颜色。
   */
  override fun commandFinished(event: CommandEvent) {
    if (project.isDisposed || !isClassicFallbackActive()) return
    val editor = matchingMainEditorFor(event) ?: return
    val name = event.commandName ?: ""
    
    when {
      name == APPLY_COMPLETION_COMMAND -> handleApplyCompletion(editor)
      canTriggerTyping(editor, name) -> triggerTypingCompletion(editor)
      canHandleCaretMove(editor, name) -> handleCaretMovement(editor, name)
    }
  }
  
  
  /**
   * 经典兜底是否生效：补丁总开关开启且官方 NEXT 编码预测关闭。
   * NEXT 开启时官方链路自洽，补丁必须保持静默。
   */
  private fun isClassicFallbackActive(): Boolean =
    QoderPatchState.getInstance().classicCompletionEnabled && !InlineEditUtil.isEnableNEXT()
  
  /**
   * 处理 Tab 采纳补全后的连环预测调度。
   *
   * 官方新版此路径被强制导向 inlineEdit(ACCEPTED)，而 NEXT 关闭时该方法直接 return，
   * 导致连续预测中断。此处异步调度 triggerNext 还原 3.3.5 的连环补全能力，并在发起瞬间高亮光标。
   */
  private fun handleApplyCompletion(editor: Editor) {
    application.invokeLater {
      if (!project.isDisposed && !editor.isDisposed) {
        QoderCaretColorManager.applyCompletionCaret(editor)
        NextCompletionRequest.build().triggerNext(editor)
      }
    }
  }
  
  /**
   * 检查当前打字命令是否满足自动触发补全的复合条件：
   * 1. 命令名称属于打字白名单；
   * 2. 当前编辑器未处于 Live Template 交互中（[TemplateManager.getActiveTemplate] == null）；
   * 3. 具有有效的命令起始快照；
   * 4. 文档实际内容发生修改（[startStamp] 变化）；
   * 5. 命令起始时无文本选区（[startSelection] 为空）；
   * 6. Lookup 弹窗允许触发且编辑器支持行内渲染。
   */
  private fun canTriggerTyping(editor: Editor, name: String): Boolean =
    name in typingCommands &&
    TemplateManager.getInstance(project).getActiveTemplate(editor) == null &&
    snapshotValid &&
    editor.document.modificationStamp != startStamp &&
    startSelection.isNullOrEmpty() &&
    CompletionUtil.isTriggerWhenLookup(editor) &&
    CosyInlayManager.getInstance().isAvailable(editor)
  
  /**
   * 执行打字自动补全触发时序：清理既有浮层 -> 激活光标高亮 -> 发起补全请求。
   */
  private fun triggerTypingCompletion(editor: Editor) {
    InlineEditUtil.disposeAllInline(editor, InlayDisposeEventEnum.TYPING)
    QoderCaretColorManager.applyCompletionCaret(editor)
    InlayPreviewRequest.build().generate(
      CompletionTriggerConfig.defaultConfig(InlayTriggerEventEnum.TYPING),
      editor,
      CompletionTriggerModeEnum.AUTO
    )
  }
  
  /**
   * 检查当前命令是否满足纯光标位移的处理条件：
   * 1. 排除撤销/重做/剪切板及内部命令（`skipCommands` 与 `tongyi`/`qoder` 内部写命令）；
   * 2. 具有有效的命令起始快照；
   * 3. 光标物理偏移坐标发生实际位移（[startOffset] 变化）。
   */
  private fun canHandleCaretMove(editor: Editor, name: String): Boolean {
    val lower = name.lowercase()
    val isIgnored = lower in skipCommands || lower.contains("tongyi") || lower.contains("qoder")
    return !isIgnored && snapshotValid && startOffset != editor.caretModel.offset
  }
  
  /**
   * 处理纯光标移动命令：预热目标位置的上下文预补全，清理旧浮层，并立即复原光标原生颜色。
   */
  private fun handleCaretMovement(editor: Editor, commandName: String) {
    CompletionUtil.triggerCursorPreCompletion(editor)
    InlineEditUtil.disposeAllInline(editor, InlayDisposeEventEnum.CHANGE_CARET, commandName)
    QoderCaretColorManager.restoreOriginalCaret(editor)
  }
  
  /**
   * 解析命令事件对应的目标主编辑器。
   *
   * 确保仅当事件属于当前项目、具有关联文档、且聚焦的文本编辑器属于主编辑区（[EditorKind.MAIN_EDITOR]）
   * 且文档完全一致时才返回实例；其它场景（如控制台、Diff 查看器、输入框）返回 null 予以忽略。
   *
   * @return 匹配的当前项目主编辑器实例，不满足时返回 null。
   */
  private fun matchingMainEditorFor(event: CommandEvent): Editor? {
    if (event.project !== project) return null
    val document = event.document ?: return null
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    if (editor.editorKind != EditorKind.MAIN_EDITOR) return null
    return if (editor.document === document) editor else null
  }
  
  private companion object {
    /**
     * 官方 Tab 采纳经典补全时由 `WriteCommandAction` 执行的写命令名称。
     * 溯源参考：官方 2026.814 版本 `CosyInlayManagerImpl.applyCompletion:419`。
     */
    const val APPLY_COMPLETION_COMMAND = "Apply Tongyi Inline Suggestion"
    
    /**
     * 官方经典路径允许触发自动补全的打字命令名白名单。
     * 溯源参考：官方 3.3.5 版本 `CosyCommandListener.ALLOW_COMMANDS`。
     */
    val typingCommands = setOf("Typing", "输入")
    
    /**
     * 忽略的命令名称黑名单（转换为全小写后比对），包含中文本地化命令与剪切板/历史操作。
     * 溯源参考：官方 3.3.5 版本 `CosyCommandListener.SKIP_COMMANDS`。
     */
    val skipCommands = setOf(
      "undo", "backspace", "paste", "redo", "copy", "取消粘贴", "退格", "粘贴", "重做粘贴", "复制"
    )
  }
}
