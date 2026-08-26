package com.github.mirminamirror.qoder.cn.patch.completion

import com.alibabacloud.intellij.qoder.editor.CosyInlayManager
import com.github.mirminamirror.qoder.cn.patch.configurable.QoderPatchState
import com.github.mirminamirror.qoder.cn.patch.util.runOnEdt
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Color
import java.util.concurrent.TimeUnit

/**
 * Qoder 代码补全期间的光标视觉状态管理器。
 *
 * 负责在补全真正发起或行内建议展示时保存编辑器光标的原生视觉属性（颜色、粗细等），
 * 并根据用户设置切换为补全高亮光标；在补全生命周期结束（采纳、取消、移动、超时）后
 * 精准无损恢复光标的原本样式。
 */
object QoderCaretColorManager {
  
  /** 存储光标变色前的原始视觉属性快照的 Key。 */
  private val keyOriginalAttributes: Key<CaretVisualAttributes?> =
    Key.create<CaretVisualAttributes>("qoder.cn.patch.originalCaretAttributes")
  
  /** 标记当前编辑器是否处于补全光标高亮状态的 Key。 */
  private val keyCaretHighlightActive: Key<Boolean?> = Key.create<Boolean>("qoder.cn.patch.caretHighlightActive")
  
  /**
   * 当补全开始或建议渲染时，为指定编辑器的光标应用高亮颜色与粗细。
   *
   * @param editor 目标编辑器。
   */
  fun applyCompletionCaret(editor: Editor) {
    val state = QoderPatchState.getInstance()
    if (!state.completionCaretColorEnabled) return
    if (editor.isDisposed) return
    
    runOnEdt {
      if (editor.isDisposed) return@runOnEdt
      
      // 已处于高亮状态时避免重复覆盖原始快照
      val isActive = editor.getUserData(keyCaretHighlightActive) == true
      val primaryCaret = editor.caretModel.primaryCaret
      if (!isActive) {
        val originalAttrs = primaryCaret.visualAttributes
        editor.putUserData(keyOriginalAttributes, originalAttrs)
        editor.putUserData(keyCaretHighlightActive, true)
      }
      
      val targetColor = parseColor(state.completionCaretColorHex)
      val weight =
        if (state.completionCaretBold) CaretVisualAttributes.Weight.HEAVY else CaretVisualAttributes.Weight.NORMAL
      val newAttrs = CaretVisualAttributes(targetColor, weight)
      
      editor.caretModel.allCarets.forEach { caret: Caret ->
        caret.visualAttributes = newAttrs
      }
      
      // 超时防呆：15 秒后若未处于活动建议状态，自动恢复默认样式
      AppExecutorUtil.getAppScheduledExecutorService().schedule(
        {
          if (!editor.isDisposed) {
            runOnEdt {
              if (!editor.isDisposed &&
                  !CosyInlayManager.getInstance().hasCompletionInlays(editor)
              ) {
                restoreOriginalCaret(editor)
              }
            }
          }
        },
        15,
        TimeUnit.SECONDS
      )
    }
  }
  
  /**
   * 当补全结束（采纳、丢弃、光标移动、超时等）时，恢复编辑器光标的原始视觉属性。
   *
   * @param editor 目标编辑器。
   */
  fun restoreOriginalCaret(editor: Editor) {
    if (editor.isDisposed) return
    
    runOnEdt {
      if (editor.isDisposed) return@runOnEdt
      val isActive = editor.getUserData(keyCaretHighlightActive) == true
      if (!isActive) return@runOnEdt
      
      val originalAttrs =
        editor.getUserData(keyOriginalAttributes) ?: CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL)
      editor.putUserData(keyCaretHighlightActive, null)
      editor.putUserData(keyOriginalAttributes, null)
      
      editor.caretModel.allCarets.forEach { caret: Caret ->
        caret.visualAttributes = originalAttrs
      }
    }
  }
  
  /**
   * 将十六进制颜色字符串转换为 [Color]，解析失败时回退默认粉色。
   */
  private fun parseColor(hex: String): Color =
    ColorUtil.fromHex(hex, JBColor(0xFF79C6, 0xFF79C6)) ?: JBColor(0xFF79C6, 0xFF79C6)
}
