package com.github.mirminamirror.qoder.cn.patch.configurable

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 伴生补丁插件自身的持久化状态，独立于官方 `CosyPersistentSetting` 存储。
 *
 * @property classicCompletionEnabled 经典行内补全兜底总开关；仅当官方 NEXT 编码预测关闭时生效，
 * 避免与官方 `editPredict` 链路产生双重渲染。
 * @property completionCaretColorEnabled 代码补全时是否改变光标颜色。
 * @property completionCaretColorHex 代码补全时光标的高亮颜色十六进制值（例如 "#FF79C6"）。
 * @property completionCaretBold 代码补全时是否加粗显示光标。
 */
@Service(Service.Level.APP)
@State(name = "QoderPatchState", storages = [Storage("qoder-cn-patch.xml")])
class QoderPatchState : PersistentStateComponent<QoderPatchState> {
  
  var classicCompletionEnabled: Boolean = true
  
  var completionCaretColorEnabled: Boolean = true
  
  var completionCaretColorHex: String = "#FF79C6"
  
  var completionCaretBold: Boolean = true
  
  override fun getState(): QoderPatchState = this
  
  override fun loadState(state: QoderPatchState) {
    XmlSerializerUtil.copyBean(state, this)
  }
  
  companion object {
    @JvmStatic
    fun getInstance(): QoderPatchState = service()
  }
}
