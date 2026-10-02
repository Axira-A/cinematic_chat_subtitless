# Building Cinematic Chat

## Requirements

- JDK 21 for Minecraft 1.21.x; JDK 25 for 26.1.x. Install both for the full matrix. Shared modules always compile with `--release 21`.
- Python 3 for maintained build/verification scripts; PowerShell 7 on Windows or Bash on Linux/macOS for their respective entry points.
- Internet access to Gradle distributions, Maven Central, the Gradle Plugin Portal, Mojang, Fabric and NeoForge public repositories. Normal builds use standard Gradle caches.
- A graphical session and OpenGL/audio support for development clients. Linux CI uses Xvfb and software rendering.

Set `JAVA_HOME` to JDK 25 for a combined matrix run. Gradle locates the installed JDK 21 toolchain; if needed, set `org.gradle.java.installations.paths` in your user Gradle properties to the two JDK directories. Do not commit machine-specific paths. For a legacy-only run, `JAVA_HOME` may point to JDK 21.

NeoForge's external decompiler may request a 6 GiB heap independently of Gradle's heap. For constrained build machines, the following standard JVM environment options limit each Java process to 3 GiB and four processors. They also reach external build tools; `org.gradle.jvmargs` alone does not limit those tools. CI uses these limits. Keep enough total memory for Gradle and its child processes, and apply these options only in the build shell:

```powershell
$env:_JAVA_OPTIONS='-Xmx3g'
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4'
```

```bash
export _JAVA_OPTIONS='-Xmx3g'
export JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4'
```

If `JAVA_TOOL_OPTIONS` already contains required network or JVM settings, preserve them when adding the processor limit.

## Build inputs

[gradle/targets.json](gradle/targets.json) is the authoritative 15-version, two-loader matrix. [gradle/source-layout.json](gradle/source-layout.json) selects shared and version-specific adapters. All dependencies remain pinned: legacy Gradle 9.2.1, modern Gradle 9.4.0, ModDevGradle 2.0.148, Loom 1.15.5 and Fabric Loader 0.19.5. Fabric API and NeoForge versions come from the target matrix. Fabric uses Mojang Official Mappings for legacy targets and the unobfuscated plugin for 26.1.x.

The root Wrapper is a legacy convenience entry; `legacy/` and `modern/` are independent build roots. All three Wrapper JARs, distribution hashes and scripts are committed. Use the supplied Wrappers, not an arbitrary system Gradle installation. Locks live in `gradle/dependency-locks/`; all three `gradle/verification-metadata.xml` files share the same strict checksum baseline. Loom-generated mapping, game and remapped-mod content pins are intentional build inputs, not generated test reports.

## Individual targets

Run from the repository root. Windows examples:

```powershell
./legacy/gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=neoforge :neoforge:build :neoforge:smokeClasses check --no-parallel
./legacy/gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=fabric :fabric:build :fabric:smokeClasses check --no-parallel
./modern/gradlew.bat -p modern '-PmcVersion=26.1.2' -Ploader=fabric :fabric:build :fabric:smokeClasses check --no-parallel
```

On Unix, replace `./legacy/gradlew.bat` with `bash legacy/gradlew` and likewise for modern. The output is `targets/<minecraft>/<loader>/build/libs/cinematic-chat-<modVersion>+mc<minecraft>-<loader>.jar`. Each target has its own JAR; core modules are bundled. `build` checks runtime contents, all four language files, exact-version loader metadata, required Mixin classes, bytecode levels and exclusion of development scenarios/scripts.

## Full matrix

```powershell
pwsh -File tools/build/build-matrix.ps1
pwsh -File tools/build/build-matrix.ps1 -Minecraft 1.21.1,26.1.2 -Loader fabric
pwsh -File tools/build/build-matrix.ps1 -Smoke
```

```bash
bash tools/build/build-matrix.sh
bash tools/build/build-matrix.sh --minecraft 1.21.1 26.1.2 --loader fabric
bash tools/build/build-matrix.sh --smoke
```

The wrappers call the same Python implementation. `--list` lists a selection without building. Each run creates a new ignored `dist/matrix/<timestamp-id>/` with commands, exit codes, logs, copied JARs and SHA-256 values. Unknown/empty selections, failed commands, missing JARs and failed requested client scenarios return nonzero. Logs and results are never archived into documentation.

## Architecture and development

- `minecraft-api`: pure Java contracts for styled chat, sender identity, clocks, rendering and audio.
- `engine`: bounded CCML scanner/AST, grapheme segmentation, timeline, layout, queue and events. No Minecraft, loader, filesystem, network or audio backend dependencies. Diagnostics are language-neutral codes/arguments; adapters translate them.
- `client-common`: configuration persistence, HUD editor models, voice matching, audio scheduling/cache and client coordination.
- `adapters/common`, `adapters/api`, `adapters/overrides`, and `targets`: native Minecraft APIs, version-specific drawing/input/chat/audio bridges and loader registration. Source ownership is checked for all targets.

Keep one integration branch for the matrix. Version differences belong in explicit adapters/source selection; thirty long-lived branches would duplicate fixes. A development client uses `:<loader>:runClient`. The isolated test client uses `:<loader>:runSmoke`, creates a temporary world in that target's ignored `run-smoke/`, runs assertions and writes screenshots plus `smoke-report.json`. Test classes are outside the production source set. The native scenarios include four-language settings, HUD, voice and preview checks; a language change affects new generated samples, not real chat.

```powershell
./legacy/gradlew.bat -p legacy '-PmcVersion=1.21.1' -Ploader=neoforge :neoforge:runSmoke --no-parallel
```

Development profiles are offline: authenticated Signed Chat needs a separately authenticated client and signature-enforcing server. Do not describe offline scenarios as authenticated verification.

## Checks and clean verification

```text
python tools/verify/verify-wrapper.py
python tools/verify/verify-source-layout.py
python tools/verify/verify-localization.py
python tools/verify/verify-docs.py
```

Pure Java tests can run without selecting a loader:

```powershell
./legacy/gradlew.bat -p legacy -PcoreOnly check --no-parallel
./modern/gradlew.bat -p modern -PcoreOnly check --no-parallel
```

Localization checks reject duplicate keys, invalid JSON, empty strings, unequal key sets, incompatible placeholders and uncovered dynamic options. The documentation checker validates local paths, anchors and four-language navigation. Core tests cover parser bounds/timeline/Unicode, editor working copies and geometry, voice scheduling and backend failures.

To test cold dependency resolution, clone the candidate commit into a new directory and use a **new empty `GRADLE_USER_HOME`**, with no copied project `.gradle/`, build directories, tool caches or dependency caches. Run all checks and the full matrix there using public repositories and strict verification. Do not use `--offline` or disable verification. This verifies cold-source buildability, not cross-platform byte-for-byte JAR identity. The [matrix workflow](.github/workflows/matrix.yml) runs core/static checks and all targets; its manual **cold** option disables Gradle cache restore/save. Logs, JARs and screenshots are CI artifacts.

## Dependency maintenance

Do not automatically update checksums after an unexpected mismatch. First verify the exact coordinate, upstream source and download. For an intentional reviewed dependency change, use Gradle's verification/locking maintenance options, then merge the legacy/modern verification metadata with `tools/verify/merge-verification.py` and review the diff. It preserves the narrow generated-artifact trust entries whose contents are separately pinned.

`write-mapping-baseline.py` and `write-generated-game-baseline.py` read an explicitly selected standard Gradle Loom cache with `--loom-cache`. `write-generated-mod-baseline.py` reads original artifacts from `--gradle-home` (defaults to `GRADLE_USER_HOME` or `~/.gradle`) and remapped outputs from `legacy/.gradle/loom-cache/remapped_mods/`; it checks the committed input hashes before recording contents. The mapping rejection probe (`test-generated-mapping-verification.ps1`, optionally `-Game` or `-Mods`) runs alone, restores the baseline in `finally`, and writes only ignored output. Baseline writers are maintenance tools, not normal build steps.

## Original test audio

Source attribution is in [SOURCES.txt](adapters/common/minecraft/src/main/resources/assets/cinematic_chat/audio/SOURCES.txt). WAV regeneration uses `tools/assets/generate-test-audio.ps1`. For OGG, create an independent Python virtual environment, install `numpy` and `soundfile==0.13.1`, then run `tools/assets/generate-test-ogg.py` with that environment's Python. The encoder does not inject a private repository cache. Audio regeneration is unnecessary for ordinary builds.

Generated build, run, world, log, cache and IDE files are ignored. Wrapper JARs, original source audio, matrix scripts, tests and dependency/content baselines remain tracked.
