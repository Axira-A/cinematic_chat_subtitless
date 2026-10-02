# 开发工具

以下工具仅供开发、构建和验收使用，不进入模组 JAR。

| 目录 | 用途 | 常用入口 |
|---|---|---|
| build | 独立目标构建与客户端矩阵 | `build-matrix.ps1 -Smoke`、`bash build-matrix.sh --smoke` |
| verify | Wrapper、源码归属、依赖及派生内容校验 | `verify-wrapper.py`、`verify-source-layout.py` |
| reports | 从实际日志、JAR、场景结果汇总证据 | `write-matrix-report.py --archive` |
| assets | 原创测试音重生成 | `generate-test-audio.ps1`、`generate-test-ogg.py` |
| dev-cache | 初次开发时的网络、磁盘及缓存恢复 | 见该目录说明 |

从仓库根目录执行工具；Python 工具只需 Python 3 标准库，OGG 素材重生成另需 `soundfile==0.13.1`。PowerShell 工具使用 PowerShell 7。被 Git 忽略的临时下载、编码器和审查源码统一存放在 `tools/.cache/`。

`write-generated-*-baseline.py` 与 `merge-verification.py` 用于明确审阅后的校验基线更新，不在普通构建或 CI 中自动运行。正常构建只读取并验证这些 pin。

项目在同一个集成分支维护全部目标，功能修改使用短期 `dev/<名称>` 分支。版本差异通过源码目录和构建矩阵表达，发布通过 tag 固定源码并分别提供各 Minecraft/Loader 的 JAR。无需为每个目标维护长期分支。
