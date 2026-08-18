package com.github.mirminamirror.qoder.cn.patch.util

import com.intellij.util.application

/**
 * 确保在 EDT 线程中执行。
 */
fun runOnEdt(action: () -> Unit) {
  if (application.isDispatchThread) {
    action()
  } else {
    application.invokeLater(action)
  }
}
