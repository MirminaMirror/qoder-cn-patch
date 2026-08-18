# qoder-cn-patch

🛠️ **Qoder CN (通义灵码) JetBrains 插件功能修复与增强伴生插件（Companion Plugin）**

针对 Qoder CN 新版（`2026.814` 等）相较于旧版（`3.3.5`）被官方移除 / 主动禁用的核心功能，
以**零字节码修改**的伴生插件方式恢复：声明 `depends` 依赖官方插件，直接复用官方保留的
补全管线（`InlayPreviewRequest` → `completionWithDebouncer` → `DefaultInlayCompletionCollector`）。

版本差异与断流根因的完整逆向分析见 [docs/05-version-diff-verified-report.md](docs/05-version-diff-verified-report.md)。

---

## ✨ 修复与恢复的功能清单

### 行内代码补全（核心断流恢复）

官方 2026.814 删除了 `CosyInlayManagerImpl.editorChanged` 的经典补全分支与
`CosyCommandListener.triggerCompletion`，并将 `CosyConfig.useNewNesFeature()` 开关整体移除，
导致打字自动补全与 Alt+P 手动补全全部断流（详见 docs/05 §2）。本插件：

1. **打字自动补全**（`QoderClassicCompletionListener`）：
   - 按项目注册 `CommandListener`，忠实还原官方 3.3.5 `triggerCompletion` 调度语义：
     打字命令（`Typing` / `输入`，含中文 IME）→ 文档真实变更 → 无起始选区 →
     无活动 Live Template → Lookup 允许 → 直调 `InlayPreviewRequest.generate(AUTO)`。
   - **仅当官方 NEXT 编码预测关闭时介入**（`!InlineEditUtil.isEnableNEXT()`），
     NEXT 开启时官方 `triggerCompletionNesCombination` 链路自洽，插件保持静默，避免双重渲染。
2. **手动触发补全**：`Alt + C`（`QoderTriggerInlayAction`）直调
   `InlayPreviewRequest.generate(MANUAL)`，绕过被断流的 `editorChanged`；
   官方失效的 Alt+P 保持原样不冲突。
3. **Tab 采纳后下一行预测**：捕获采纳写命令 `Apply Tongyi Inline Suggestion`，
   调用官方孤儿方法 `NextCompletionRequest.triggerNext`（对应 3.3.5 ACCEPT 非 NEXT 分支）。
4. **光标移动预热**：纯光标移动命令后 `triggerCursorPreCompletion` + 浮层清理（官方语义）。

### 设置界面

5. **完整恢复「代码补全设置」面板**（挂载在官方 `qodercn.settings` 设置树下）：
   云端大模型开关、自动 / 手动触发补全长度（行级 / 较短 / 较长 / 超长 / 关闭）、
   IDE 补全时展示行间建议、禁用语言列表；`apply` 后经 `Cosy.updateConfig` 实时推送后端。
6. **完整恢复「行间建议预测 (NES)」面板**：开关、推荐方式 (Inline / Side by Side / Auto)、代码移动。
7. **补丁兜底总开关**：可一键停用插件全部补全介入逻辑（`isModified` 基于表单快照精确判定，
   不再恒真）。

### 菜单与状态栏

8. **右键上下文菜单**：恢复「优化代码 (Alt+Shift+O)」「生成代码注释 (Alt+Shift+V)」
   「生成单元测试 (Alt+Shift+U)」「解释代码 (Alt+Shift+P)」，委托官方保留的 Action ID。
9. **框选代码对话按钮**（`QoderSelectionPopupAction`）：官方类 `CosySelectionPopupAction`
   与 `InlayCompletionHintFactory.showChatButtonAtCaret` 均保留但注册被注释禁用，此处直调恢复。
10. **状态栏设置入口**（`QoderPatchStatusBarWidget`）：替代被官方整体删除的
    `CosyCompletionTitleDisplayAction`，一键直达补丁设置面板。

---

## 🚀 构建与安装

### 1. 构建

```bash
# Windows
.\gradlew.bat buildPlugin

# macOS / Linux
./gradlew buildPlugin
```

产物输出：

```text
build/distributions/qoder-cn-patch-1.0.0.zip
```

### 2. 安装

前提：IDE 中已安装官方 **Qoder CN 2026.814+** 插件（本插件声明了对
`com.alibabacloud.intellij.cosy` 的依赖）。

1. 打开 **Settings / Preferences** (`Ctrl + Alt + S`)。
2. 进入 **Plugins** → 齿标 ⚙️ → **Install Plugin from Disk...**。
3. 选择 `qoder-cn-patch-1.0.0.zip`，重启 IDE。

### 3. 生效条件

- 打字自动补全兜底：官方设置中 **NEXT 编码预测关闭** 时生效（开启 NEXT 时官方链路自洽，
  插件静默）；可在补丁设置面板用总开关进一步控制。
- 手动补全 `Alt + C`：无前提条件，随时可用。

### 4. 沙盒调试

```bash
.\gradlew.bat runIde
```

---

## 🧪 验证清单（沙盒实测）

1. 官方设置中关闭 NEXT → 打字出现灰色行内补全 → Tab 采纳 → 出现下一行预测。
2. `Alt + C` 手动触发补全。
3. Settings → Qoder CN → Completion & NES Settings 面板加载 / 修改 / Apply 生效。
4. 编辑器右键菜单四项功能可用；框选代码后右键出现「框选代码对话」。
5. 状态栏出现 "Qoder Completion" 入口，点击直达设置面板。
