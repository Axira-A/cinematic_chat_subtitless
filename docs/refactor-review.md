# 项目结构与兼容层整改

2026-10-02，基于 `a23f0d8` 审查并整改。全部目标继续在 `dev/cinematic-chat` 集成分支维护。

## 已修改的问题

| 原问题 | 当前实现 |
|---|---|
| 公共源码寄存在 `targets/1.21.1`，其他目标依赖参考版本目录 | 公共业务迁至 `adapters/common`，参考版本与其他目标使用相同的构建 convention |
| 26.1.x 通过 Gradle 字符串替换生成 Java 源码，编译后的代码不便审查 | 删除生成脚本；`adapters/api/mc26.1` 提供显式类型桥接，共享界面使用 `GuiCanvas` |
| 相同 override 在多个版本复制，易漏改 | 字节相同的文件合并为共享 override，由 `gradle/source-layout.json` 明确列出使用版本 |
| 根据 Java 版本推测 Minecraft API | `gradle/targets.json` 明确声明 `apiFamily`，Java 版本只控制工具链与字节码 |
| 构建、缓存修复、素材和报告脚本混在同一目录 | 分为 `tools/build`、`verify`、`reports`、`assets`、`dev-cache`，更新全部引用 |
| 开发缓存隐藏了在线解析时缺少的 Gradle metadata 校验 | 从官方仓库核对坐标与 SHA-256，补齐固定校验；缓存工具下载 POM、module 与指定 artifact |
| CI 缺少独立核心测试与完整失败证据 | 新增核心任务，逐目标保留命令日志与校验报告，正常代码变更仍执行全部 30 个目标 |

adapter 与目标目录的 Java 文件从 141 个减少至 74 个，当前没有字节完全相同的重复 Java 文件。源码选择器允许明确的覆盖；`verify-source-layout.py` 检查目录、版本归属、有效类路径、覆盖冲突、现代 API 所有权和三份依赖校验的一致性。

## API 审查范围

已核对 26.1、26.1.1、26.1.2 的实际生成 Minecraft 源码，以及 Fabric API 的实际类签名。重点包括聊天准入入口与 `DisplayMode`、`GuiGraphicsExtractor` 绘制方法、Screen 的 extract 生命周期与输入事件、资源重载、按键、Profile 和开发场景 API。没有发现这些已核对调用使用不存在的 API。

26.1.x 的聊天捕获仍在 `addMessageToQueue(GuiMessage)` 写入历史之后，渲染 gate 仅针对 `DisplayMode.BACKGROUND`。绘制、界面生命周期、聊天入口与测试平台均有显式实现；现代适配不再依赖替换方法名的生成过程。准确签名与迁移依据见 [26.1](porting/26.1.md)、[26.1.1](porting/26.1.1.md)、[26.1.2](porting/26.1.2.md)。

编译证明符号存在；客户端启动与场景检查用于验证 Mixin 注入和运行行为。完整人工验收与已认证 Signed Chat 仍按 [验收清单](acceptance.md) 单独记录。不能从 API 审查或离线开发场景推导这些项目已经通过。

## Python、PowerShell 与分支

Python 和 PowerShell 保留为开发工具，均不进入发布 JAR。缓存恢复脚本留在 `dev-cache`，不是正常构建和 CI 的前置依赖；没有用户目录或本机盘符硬编码。用途、依赖与执行入口见 [tools/README.md](../tools/README.md)。

不为 15 个版本或 30 个目标建立长期分支：核心修复需要同时进入全部目标，版本差异已经由源码目录与构建矩阵隔离。功能工作使用短期 `dev/<名称>` 分支，发布通过 tag 固定源码并分别输出各版本 JAR。只有未来出现需要独立维护的长期产品线时，再建立维护分支。

## 验证证据

整改后的本机逐目标结果见 [矩阵报告](validation/matrix.md)，命令、UTC 时间、断言和 JAR/日志/截图哈希见 [matrix.json](validation/matrix.json)。远端干净环境的执行结果单独记录在 [CI 状态](ci-status.md)。历史失败保留，不用本地成功覆盖远端失败记录。
