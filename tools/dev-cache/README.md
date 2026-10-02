# 本机缓存恢复工具

这些工具是初次 Windows 开发期间为 Java HTTPS 连接重置、重复下载和磁盘空间问题保留的恢复步骤。正常在线构建和 CI 不调用它们；它们不会成为发布运行时依赖。

`cache-*` 从指定官方仓库准备可选 Maven、游戏或资源缓存。Maven 工具同时尝试 POM、Gradle `.module` 和二进制；仅未发布 `.module` 的 HTTP 404 可以跳过，网络错误仍失败。缓存内容不自动加入信任清单，更新 pin 仍须审阅后调用 `tools/verify/merge-verification.py`。

`reuse-development-assets.ps1` 由资源准备工具调用；`reuse-minecraft-artifacts.ps1` 是手动恢复入口，用于在 NeoForm 与 Loom 之间搬运已按 Mojang 清单校验的下载。两个 reuse 工具依赖当前锁定的 Loom 1.15.5 缓存布局，包括 layered_hash_2198。升级 Loom 时应重新核对这两个工具，不能作为日常构建步骤。

所有 Gradle 缓存路径优先使用 `GRADLE_USER_HOME`，未配置时使用用户目录下的 `.gradle`。可选 Maven 文件统一写入 `tools/.cache/maven`；不要提交缓存或把本机缓存记录当成干净在线环境的验证结果。
