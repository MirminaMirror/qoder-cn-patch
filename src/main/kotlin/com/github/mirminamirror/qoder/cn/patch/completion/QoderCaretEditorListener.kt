package com.github.mirminamirror.qoder.cn.patch.completion

import com.alibabacloud.intellij.qoder.editor.CosyInlayManager
import com.alibabacloud.intellij.qoder.editor.CosyInlayRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

/**
 * 编辑器生命周期监听器，为每个打开的编辑器挂载行内补全与光标事件监听。
 *
 * 当 Qoder 渲染行内补全（[CosyInlayRenderer]）时激活光标高亮；
 * 当行内补全被采纳/移除、光标移动或编辑器关闭时，确保光标 100% 恢复原生视觉属性。
 */
class QoderCaretEditorListener : EditorFactoryListener {
  
  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor
    val parentDisposable = editor as? Disposable ?: editor.project ?: return
    
    editor.inlayModel.addListener(
      object : InlayModel.Listener {
        override fun onAdded(inlay: Inlay<*>) {
          if (inlay.renderer is CosyInlayRenderer) {
            QoderCaretColorManager.applyCompletionCaret(editor)
          }
        }
        
        override fun onRemoved(inlay: Inlay<*>) {
          if (inlay.renderer is CosyInlayRenderer) {
            if (!CosyInlayManager.getInstance().hasCompletionInlays(editor)) {
              QoderCaretColorManager.restoreOriginalCaret(editor)
            }
          }
        }
        
        override fun onUpdated(inlay: Inlay<*>, changeFlags: Int) {
          if (inlay.renderer is CosyInlayRenderer) {
            QoderCaretColorManager.applyCompletionCaret(editor)
          }
        }
      },
      parentDisposable
    )
    
    editor.caretModel.addCaretListener(
      object : CaretListener {
        override fun caretPositionChanged(e: CaretEvent) {
          if (!CosyInlayManager.getInstance().hasCompletionInlays(editor)) {
            QoderCaretColorManager.restoreOriginalCaret(editor)
          }
        }
      },
      parentDisposable
    )
  }
  
  override fun editorReleased(event: EditorFactoryEvent) {
    QoderCaretColorManager.restoreOriginalCaret(event.editor)
  }
}
