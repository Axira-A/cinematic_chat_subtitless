# Maintained development tools

Run tools from the repository root. See [BUILDING.md](../BUILDING.md) for requirements and commands.

- `build/build-matrix.ps1` and `build/build-matrix.sh`: all targets or a selected subset, with optional development client scenarios. Results go to a new ignored `dist/matrix/` directory.
- `verify/`: Wrapper pins, source boundaries, four-language resources, documentation links, and reviewed dependency/content baselines.
- `assets/`: regeneration of the original test tones. The OGG encoder uses an independent Python virtual environment.

Baseline writers are deliberate maintenance operations, never an automatic response to a checksum failure. Normal builds read the committed locks and verification inputs and use public repositories and standard Gradle caches.
