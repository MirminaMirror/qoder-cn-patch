<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# qoder-cn-patch Changelog

## [Unreleased]

### Added

- 行内补全断流恢复：打字自动补全（CommandListener 兜底，NEXT 关闭时介入）、 Alt+C 手动触发、Tab 采纳后 triggerNext 下一行预测、光标移动预热。
- 设置面板挂载官方 `qodercn.settings` 节点，恢复代码补全 / NES 全套参数配置， 新增补丁兜底总开关；`isModified` 改为表单快照精确判定。
- 右键菜单恢复优化代码 / 生成注释 / 生成单测 / 解释代码；新增框选代码对话入口、 状态栏 "Qoder Completion" 设置直达 Widget。

### Removed

- 已移除实验性的任意 OpenAI-compatible `base_url` 集成及仅用于验证该路径的 BYOK 目录导出工具：Qoder Native 未将该字段用于 provider 路由，继续暴露会导致静默回退或错误路由。完整实验和源快照见 `research/qoder-native-openai-compatible-routing/`。

### Fixed

- 设置面板 `parentId` 指向不存在的 `cosy.settings` 导致无法挂载的问题。
- `isModified` 恒真导致的重复写配置与重复推送后端。
- 旧 typedHandler 仅覆盖 `charTyped`、无场景过滤的问题（改为 CommandListener 全量调度）。
