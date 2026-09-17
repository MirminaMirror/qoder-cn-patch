package com.github.mirminamirror.qoder.cn.patch.util

import com.alibabacloud.intellij.qoder.common.CosySetting
import com.alibabacloud.intellij.qoder.editor.CosyInlayManager
import com.alibabacloud.intellij.qoder.product.ProductRuntime
import com.alibabacloud.intellij.qoder.ui.config.CosyPersistentSetting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor

/**
 * Qoder 上游核心服务安全桥接器。
 *
 * 兼容解决 Qoder 2026.917+ 物理删除 [CosyInlayManager.getInstance] 与
 * [CosyPersistentSetting.getInstance] 静态工厂方法导致的 `NoSuchMethodError`。
 *
 * 服务解析策略（按优先级顺次回退）：
 * 1. 官方推荐底层运行时通道：`ProductRuntime.applicationServiceOrNull`（在 2026.814 与 2026.917 双版本中稳定存在且签名一致）；
 * 2. 官方新版单例门面反射：`com.alibabacloud.intellij.qoder.service.ApplicationServices`；
 * 3. 宿主平台直接查找：`ApplicationManager.getApplication().getService(...)`。
 */
object QoderServiceBridge {
  @Volatile
  private var cachedInlayManager: CosyInlayManager? = null
  
  @Volatile
  private var cachedPersistentSetting: CosyPersistentSetting? = null
  
  
  /**
   * 获取当前全局 [CosyInlayManager] 实例；若服务未就绪则返回 null。
   */
  val inlayManager: CosyInlayManager?
    get() = cachedInlayManager ?: resolveService(CosyInlayManager::class.java, "inlayManager")?.also {
      cachedInlayManager = it
    }
  
  /**
   * 获取当前全局 [CosyPersistentSetting] 实例；若服务未就绪则返回 null。
   */
  val persistentSetting: CosyPersistentSetting?
    get() = cachedPersistentSetting ?: resolveService(CosyPersistentSetting::class.java, "persistentSetting")?.also {
      cachedPersistentSetting = it
    }
  
  /**
   * 获取官方当前配置实体 [CosySetting]；若未就绪则返回 null。
   */
  val cosySetting: CosySetting?
    get() = persistentSetting?.state
  
  /**
   * 检查当前编辑器是否可用行内补全，防 null 与异常保护。
   */
  fun isAvailable(editor: Editor?): Boolean =
    editor != null && runCatching {
      inlayManager?.isAvailable(editor) == true
    }.getOrDefault(false)
  
  /**
   * 检查当前编辑器是否正处于行内补全渲染中，防 null 与异常保护。
   */
  fun hasCompletionInlays(editor: Editor?): Boolean =
    editor != null && runCatching {
      inlayManager?.hasCompletionInlays(editor) == true
    }.getOrDefault(false)
  
  /**
   * 统一多级服务解析，保证无未捕获异常抛出。
   */
  private fun <T : Any> resolveService(serviceClass: Class<T>, accessorName: String): T? {
    // 1. 优先使用双版本均公开且原生支持的 ProductRuntime
    runCatching {
      ProductRuntime.applicationServiceOrNull(serviceClass)
    }.getOrNull()?.let { return it }
    
    // 2. 尝试 2026.917+ 新增的 ApplicationServices 单例门面
    runCatching {
      val appServicesClass = Class.forName("com.alibabacloud.intellij.qoder.service.ApplicationServices")
      val method = appServicesClass.getMethod(accessorName)
      @Suppress("UNCHECKED_CAST")
      method.invoke(null) as? T
    }.getOrNull()?.let { return it }
    
    // 3. 宿主平台直接检索
    return runCatching {
      ApplicationManager.getApplication()?.getService(serviceClass)
    }.getOrNull()
  }
}
