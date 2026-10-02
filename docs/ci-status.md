# 远端 CI 实际状态

2026-10-02，首次运行 [36950804809](https://github.com/Axira-A/cinematic_chat_subtitless/actions/runs/36950804809)，提交 `a23f0d8`：矩阵生成成功，30 个目标构建失败，未进入客户端场景。

抽查 1.21.1、26.1、26.1.2 两种 Loader 的六份日志，失败均为依赖元数据校验：Fabric 的 7 个 Gradle `.module` 和 NeoForge 外部构建工具的 5 个 `.module` 未包含在校验基线中。不是 Java 编译或 Mixin 注入失败。

修复从官方仓库下载并核对元数据内的 component 坐标，固定 SHA-256，来源与 hash 见 [dependency-metadata.json](validation/dependency-metadata.json)。三个构建入口继续校验 metadata 和二进制。可选 Maven 缓存工具同步补充 `.module` 下载。

工作流新增独立核心测试、全部目标源码归属与 pin 一致性检查，固定 Ubuntu 24.04，并在失败时上传 Gradle 校验报告和命令日志。修复后的远端执行结果将在实际运行后记录；本机验证见 [matrix.md](validation/matrix.md)，两种环境的结果分别报告。

提交 `652df06` 的第二次运行 [36955402649](https://github.com/Axira-A/cinematic_chat_subtitless/actions/runs/36955402649) 在核心任务发现另外两项缺失：`apiguardian-api-1.1.2.module`、`junit-bom-5.11.4.pom`。已从 Maven Central 核对坐标并补入固定 hash；目标矩阵因核心任务失败而跳过，未记录为目标构建失败或通过。

提交 `2d88edf` 的运行 [36955699632](https://github.com/Axira-A/cinematic_chat_subtitless/actions/runs/36955699632) 核心任务通过，30 个目标仍因其他元数据缺项失败。随后扫描已经锁定且 POM 声明 `published-with-gradle-metadata` 的依赖，从官方仓库核对并增加 139 份 module 校验，未增加新的依赖版本。

提交 `2ffa352` 的运行 [36956715453](https://github.com/Axira-A/cinematic_chat_subtitless/actions/runs/36956715453) 核心任务通过。最早完成的 Fabric 目标被 Loom 本地重映射 JAR 的 ZIP hash 差异挡住；NeoForge 目标发现 Linux 专用 Netty transport 不在 Windows 生成的锁状态中。正在通过精确派生产物内容 pin 与配置对应的固定 Netty 版本处理，尚未将本次目标矩阵记录为通过。
