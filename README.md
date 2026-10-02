# Cinematic Chat

纯客户端 Minecraft 沉浸式聊天字幕、CCML 演出引擎、可视化 HUD 编辑器与可选本地伪配音。MIT 许可证。服务器无需安装本模组。

**15 个 Minecraft 版本 × NeoForge / Fabric，共 30 个目标** 已实现并独立编译、启动和运行临时世界自动场景，覆盖 1.21 至 1.21.11、26.1、26.1.1、26.1.2。当前产物属于实验版本：已认证 Signed Chat、人工听感和部分实体输入验收仍待验证，尚不标记 Supported。逐目标证据与限制见 [实际验证](docs/validation.md) 和 [验收清单](docs/acceptance.md)。

本机与 GitHub Actions 的结果分别记录：[远端 CI 状态](docs/ci-status.md)。首次远端构建因缺少依赖元数据校验失败，修复后按真实运行结果更新。

项目结构、开发脚本、分支策略和 26.1.x 显式 API 兼容层的整改见 [审查与修改记录](docs/refactor-review.md)。

## 开发与验证

legacy 入口：Java 21、Gradle Wrapper 9.2.1；modern 入口：Java 25、Gradle Wrapper 9.4.0。共享核心始终编译为 Java 21。根目录 Wrapper 是 legacy 快捷入口。Windows：

```powershell
.\gradlew.bat -PcoreOnly check
.\gradlew.bat :neoforge:build
.\gradlew.bat :neoforge:runSmoke
.\gradlew.bat -Ploader=fabric :fabric:build
.\legacy\gradlew.bat -p legacy '-PmcVersion=1.21.3' -Ploader=fabric :fabric:build
.\modern\gradlew.bat -p modern -PcoreOnly check
.\tools\build\build-matrix.ps1 -Smoke
```

Linux/macOS 将 `gradlew.bat` 换为 `bash gradlew`，全矩阵用 `bash tools/build/build-matrix.sh --smoke`。运行客户端需要图形和音频环境。开发测试会创建临时单人世界，不使用现有存档。测试专用源码不进入发布 JAR；只有显式 smoke 属性才执行场景。构建或场景失败会返回非零结果，脚本不会根据依赖表推断支持。

本机 Java HTTPS 连接部分 Maven 仓库时出现连接重置。可选的 `tools/dev-cache/cache-*.ps1` 从官方仓库下载到被 Git 忽略的 `tools/.cache/maven`，再使用 `'-PdependencyCache=tools/.cache/maven' --offline`。这些缓存不是发布依赖，不应提交。

## 使用

- `/cchat config` 或 `/cinematicchat config`：设置与实时预览。
- `/cchat hud`：拖动、Shift 锁轴、边缘调宽、右下角缩放；Alt 临时禁用吸附。
- `/cchat voice`：声线管理；`voice list`、`voice reload`、`voice test <id>`、`voice assign <speaker> <id>`。
- `/cchat preview`、`clear`、`reload`、`debug`、`parser <text>`：本地操作和调试。

伪配音默认关闭，fallback 默认 None。配置目录为 `config/cinematic_chat/`，包含 `cinematic_chat.json`、`voices.json`、`hud_presets.json` 和 `audio/`。声音只接受该 audio 目录内的相对路径。

聊天历史仍保存原始 Component 与 CCML 文字。未安装模组的玩家看到原始标签。模组不修改签名、聊天发送、网络协议或服务器权限。离线开发客户端不能验证已认证 Signed Chat 端到端行为。

详情见 [架构](docs/architecture.md)、[构建](docs/building.md)、[CCML](docs/ccml.md)、[HUD 与声音](docs/hud-and-voice.md)、[116 节需求追踪](docs/requirements/traceability.md)、[交付报告](docs/final-report.md) 和 [版本依赖基线](gradle/targets.json)。用户自行放入 audio 目录的声音应具有相应使用权限；仓库仅附带原创、MIT 授权的短测试音。
