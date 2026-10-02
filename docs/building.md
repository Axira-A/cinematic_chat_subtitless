# 构建、依赖与产物

共享 `minecraft-api`、`engine`、`client-common` 使用 Java 21；1.21.x adapter 使用 Java 21 和 Mojang Official Mappings，26.1.x adapter 使用 Java 25 的官方未混淆名称。每个目标的发布 JAR 自带共享核心。Fabric 需要 Fabric API，NeoForge 无附加配置或跨加载器运行时。

## 独立构建入口

Windows 使用 `gradlew.bat`，Linux/macOS 使用 `bash gradlew`。版本属性为 `mcVersion`：

```powershell
.\legacy\gradlew.bat -p legacy -PcoreOnly check
.\legacy\gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=neoforge :neoforge:build check
.\legacy\gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=fabric :fabric:runSmoke
.\modern\gradlew.bat -p modern '-PmcVersion=26.1.2' -Ploader=fabric :fabric:build check
.\tools\build\build-matrix.ps1 -Smoke
```

根目录 Wrapper 是 legacy 的快捷入口。modern 的 daemon JVM 条件固定为 Java 25，共享模块仍以 `--release 21` 编译。只构建需要的 Loader，不在一次 Gradle 调用中加载两个 Loader 插件。版本、Fabric API、NeoForge、Loader 和插件初始选择见 [targets.json](../gradle/targets.json)。

`targets.json` 的 `apiFamily` 明确选择 legacy 或 mc26.1 接口实现。共享源码位于 `adapters/common`，版本边界在 `adapters/api`，字节一致的 override 按 [source-layout.json](../gradle/source-layout.json) 中明确列出的版本复用。每个版本目录只包含构建入口和剩余本地差异，不再承担公共源码根目录。

全量脚本按 30 行独立顺序执行，支持 `-Minecraft`、`-Loader` 子集。Windows 辅助脚本使用 PowerShell 7；Linux/macOS 使用 Bash、Python 3 与 `bash tools/build/build-matrix.sh --smoke`。运行需要图形及 OpenAL 环境；CI 使用 Xvfb、Mesa 和 OpenAL null 驱动，人工听感需要有声设备。

`build` 包含资源处理、打包和 `verifyModJar`：检查共享核心、MIT、客户端 metadata、精确 Minecraft 版本、必需 Mixin、字节码级别，以及测试/Minecraft 类未混入发布 JAR。客户端实际启动才验证 Mixin 能否注入运行中的方法。

## 可选本机缓存

本机 Java HTTPS 访问部分仓库发生连接重置。`cache-*.ps1` 仅从 Fabric、NeoForge、Forge、Mojang、Maven Central 和 Gradle Plugin Portal 官方地址缓存文件；Mojang 游戏 JAR、映射和 assets 使用官方清单的内容 hash 校验。缓存不提交，不改变发布依赖。

```powershell
.\tools\dev-cache\cache-neoforge-target.ps1 -Minecraft 1.21.1
.\tools\dev-cache\cache-fabric-api.ps1 -Version '0.116.17+1.21.1'
.\tools\dev-cache\cache-minecraft-libraries.ps1 -Minecraft 1.21.1
.\legacy\gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=neoforge '-PdependencyCache=tools/.cache/maven' '-PneoFormCache=tools/.cache/neoformruntime' --offline :neoforge:build
```

`dependencyCache` 是优先查找的本地 Maven 目录，也覆盖插件创建的 Mojang 元数据/库仓库。`neoFormCache` 可将 NeoForm 的游戏 assets 和中间结果放在空间较大的磁盘。使用离线模式前需要缓存完整父 POM、BOM 和运行依赖。`cache-build-failures.ps1 -LogPath <日志>` 只补充 Gradle 明确报告的坐标，不能修复源码编译错误。

NeoForge 的 Mojang 库仓库明确使用 POM 图并忽略 Gradle metadata 重定向，使可选镜像中额外存在的 Central `.module` 不会改变版本图。Mojang Meta 仓库仍读取它自己的 module；Central、Fabric 和 NeoForge 工具仓库继续正常读取并校验 module。此策略在在线和离线构建中一致。

## 锁定与校验

Wrapper JAR 与 distribution SHA-256 均固定在版本管理中。运行 `python tools/verify/verify-wrapper.py` 验证三个入口。JAR 官方校验来源为 `https://services.gradle.org/distributions/gradle-<版本>-wrapper.jar.sha256`，distribution 来源为相同官方目录的 `gradle-<版本>-bin.zip.sha256`。

传递依赖锁文件放在 `gradle/dependency-locks/`。纯 Java 模块共享锁文件；adapter 与 Loader 按 Minecraft/Loader 分开，避免不同目标覆盖彼此的锁状态。[Gradle 锁定说明](https://docs.gradle.org/current/userguide/dependency_locking.html)

NeoForge 的四个 Netty epoll/kqueue 模块按操作系统条件出现。它们不要求在所有平台同时存在，版本由该 configuration 已锁定的 `netty-common` 固定；请求其他版本或缺少对应 pin 会失败。其传递依赖继续锁定，所有实际下载仍按 SHA-256 校验。这样不会把 Linux 专用模块强行加入 Windows 运行环境。

三个入口各自的 `gradle/verification-metadata.xml` 使用 SHA-256，校验 metadata 与二进制。首次生成清单是信任基线的建立，需要审阅；它不能证明上游文件不存在恶意行为。[Gradle 校验说明](https://docs.gradle.org/current/userguide/dependency_verification.html)

运行 `python tools/verify/verify-source-layout.py` 检查三个入口的校验文件一致、已审阅的 `.module` 校验以及全部目标的有效源码归属。工具在 CI 的独立核心任务中执行，随后才运行完整目标矩阵。首次 CI 暴露的元数据缺失和复测结果见 [CI 记录](ci-status.md)。

明确更新依赖时才使用：

```powershell
.\tools\build\build-matrix.ps1 -UpdateDependencyBaseline -DependencyCache 'tools/.cache/maven' -Offline
python tools/verify/merge-verification.py --official-cache tools/.cache/maven
```

合并工具保留 legacy/modern 已生成的校验，包括 Gradle 的 `also-trust` 多校验值，并加入官方缓存中的其他操作系统 native classifier 校验。更新后需执行开启正常校验的构建和客户端场景；不能用更新基线来忽略意外 hash 不匹配。

Loom 1.15.5 的本地派生 ZIP 使用生成时间，并且不同 1.21.x 版本复用 `loom:mappings:layered+hash.2198` 坐标。直接锁 ZIP SHA-256 会在干净缓存/CI 重建时报错。Gradle `trusted-artifacts` 例外逐个精确限定 1 个映射 JAR、30 个游戏派生 JAR 和 571 个 Fabric API 重映射 JAR，没有坐标或文件正则；其 POM、Loom、intermediary 和其他下载依赖仍正常校验。

`verifyGeneratedMappings` 在 adapter compile/check 前校验 ZIP 只含 `mappings/mappings.tiny`，逐版本固定该内容的 SHA-256 和原始 client/server 映射输入 hash，拒绝内容变化或额外 ZIP 文件。记录在 [generated-mappings-checksums.json](../gradle/generated-mappings-checksums.json)。`verifyGeneratedGame` 对每个 Fabric 目标校验官方 client/server 游戏输入 hash、base/processed 两个 JAR 的完整文件集和每个文件的内容，排除 ZIP 日期及压缩差异。逐文件 SHA-256 与文件名排序后再次 SHA-256，记录在 [generated-game-checksums.json](../gradle/generated-game-checksums.json)。校验任务没有缓存输出，每次 compile/check 都执行。精确例外只解决容器差异，不忽略内容校验。[Gradle 精确例外说明](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:trusted-artifacts)

1.21.x 的 Fabric API 重映射也生成本地 ZIP。`verifyGeneratedMods` 在 Loader 编译、check、客户端运行前核对全部重映射 artifact、ZIP 文件集与内容 SHA-256，固定记录见 [generated-mod-checksums.json](../gradle/generated-mod-checksums.json)。571 个本地 JAR 的 Gradle 例外逐个限定 group/name/version/file；不使用坐标正则。原始 Fabric API JAR 与元数据继续由 Gradle 校验，生成基线工具也必须先核对原始输入 hash。26.1.x 不重映射 Fabric API，不需要这一例外。

只有明确审阅输入与变更后才更新派生内容基线：

```powershell
python tools/verify/write-mapping-baseline.py --loom-cache "$env:USERPROFILE/.gradle/caches/fabric-loom"
python tools/verify/write-generated-game-baseline.py --loom-cache "$env:USERPROFILE/.gradle/caches/fabric-loom"
python tools/verify/write-generated-mod-baseline.py --official-cache tools/.cache/maven
python tools/verify/merge-verification.py
```

该工具核对每版本 Mojang 清单的输入 hash，再记录解包内容，不记录 ZIP 时间戳。`--allow-incomplete` 仅用于本地准备，缺失行会使 Gradle 校验失败，不能用于交付。缓存只有生成 ZIP、缺少原始映射输入时，应使用 `--refresh-dependencies` 重新生成完整缓存。

`tools/verify/test-generated-mapping-verification.ps1` 串行验证错误的内容 pin 和输入 pin 都会使构建失败，并在 `finally` 中逐字节恢复基线，再检查正确基线成功；加 `-Game` 检查游戏派生物。运行此探针期间不能同时执行其他构建；它不修改下载缓存。实际记录见 [generated-mappings.json](validation/generated-mappings.json)、[generated-game.json](validation/generated-game.json)，15 个 Fabric 目标的严格构建复查见 [generated-game-builds.json](validation/generated-game-builds.json)。

加 `-Mods` 验证重映射依赖的错误内容 pin、文件集合和 artifact 集合均被拒绝，再恢复原始基线验证成功。实际结果见 [generated-mod.json](validation/generated-mod.json)。全部 12 个旧版 Fabric 目标分别执行 `verifyGeneratedMods`，不把参考版本的结果推及其他版本。

## 输出与证据

产物命名为 `cinematic-chat-0.1.0+mc<版本>-<loader>.jar`，每个目标单独发布。全量脚本复制到 `dist/` 并计算 SHA-256。源码、依赖锁、校验清单、文档和 CI 入 Git；JAR、assets、存档、运行目录、日志和缓存不入 Git。

`runSmoke` 使用开发专用源码进入新建临时世界，输出 JSON、日志和截图。它不访问现有存档，测试代码不进入发布 JAR。通过自动场景不等于已完成所有验收。Signed Chat 的最终检查需要已认证客户端和启用签名验证的服务器；离线开发 profile 不能提供该证据。

`write-matrix-report.py` 汇总 `dist/` 中真实命令结果并生成 `SHA256SUMS`。`collect-evidence.ps1` 调用它的 `--archive` 模式，保存每个目标的命令、时间、断言、日志/截图校验值以及纯 Java 测试摘要。构建脚本运行子集时默认只保留该子集报告；补测可传 `-PreserveResults`，替换对应行并保留其他目标的实际命令与时间。最终必须重新生成完整 30 行报告。
