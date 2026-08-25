package com.github.mirminamirror.qoder.cn.patch.configurable

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.ConfigurableBundle"

/**
 * 配置界面国际化资源束工具类
 *
 * 用于获取配置界面相关的国际化消息，支持即时消息和延迟加载消息两种方式
 */
object ConfigurableBundle {
  private val INSTANCE = DynamicBundle(ConfigurableBundle::class.java, BUNDLE)
  
  /**
   * 获取指定键的国际化消息
   *
   * @param key 消息键值，对应资源文件中的键
   * @param params 消息参数，用于替换消息中的占位符
   * @return 格式化后的国际化消息字符串
   */
  fun message(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
    vararg params: Any,
  ): String = INSTANCE.getMessage(key, *params)
  
  /**
   * 获取指定键的延迟加载国际化消息
   *
   * @param key 消息键值，对应资源文件中的键
   * @param params 消息参数，用于替换消息中的占位符
   * @return 包含国际化消息字符串的 Supplier 对象，可延迟加载消息
   */
  @Suppress("Unused")
  fun lazyMessage(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
    vararg params: Any,
  ): Supplier<String> = INSTANCE.getLazyMessage(key, *params)
}
