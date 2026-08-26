<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# qoder-cn-patch Changelog

## [Unreleased]

### Changed

- **IDE 兼容性扩展**：最低支持基线扩展至 **IntelliJ IDEA 2023.1 及以上全系列版本**（全面兼容 2023.1 ~ 2026.2+）。

## [1.0.0] - 2026-08-25

### Added

- **行内代码补全断流恢复**：
  - 恢复打字自动行内代码建议（在官方 NEXT 编码预测关闭时自动兜底介入，无需额外配置）；
  - 新增 `Alt + C` 手动触发行内代码补全快捷键；
  - 恢复 Tab 采纳后的下一行连环预测（Accept to Next）与光标移动预热。
- **全套配置面板恢复**：
  - 完整恢复「代码补全设置」面板（云端大模型开关、自动/手动触发补全长度、IDE 补全联动、禁用语言列表）；
  - 完整恢复「行间建议预测 (NES)」面板（推荐模式选择、代码移动开关）；
  - 新增补丁兜底总开关，支持一键启停经典补全机制。
- **光标高亮视觉定制**：
  - 新增代码补全期间光标视觉高亮（支持自定义十六进制颜色与取色器）及加粗显示，补全结束后 100% 还原原生光标。
- **右键菜单与状态栏入口**：
  - 编辑器右键上下文菜单恢复「优化代码」「生成代码注释」「生成单元测试」「解释代码」快捷功能；
  - 恢复框选代码时光标处的 Qoder 对话快捷按钮；
  - 编辑器右下角状态栏常驻 `Qoder Completion` 入口，点击直达配置面板。

[Unreleased]: https://github.com/MirminaMirror/qoder-cn-patch/compare/1.0.0...HEAD
[1.0.0]: https://github.com/MirminaMirror/qoder-cn-patch/commits/1.0.0
