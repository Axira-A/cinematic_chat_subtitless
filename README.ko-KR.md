# Cinematic Chat

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md) · [한국어](README.ko-KR.md)

Minecraft 채팅을 영화 같은 자막으로 보여 주는 클라이언트 전용 모드입니다. 제한이 있는 CCML 스크립트 엔진, 시각적 HUD 편집기, 선택적인 로컬 유사 음성을 제공합니다. 채팅 화면에서 기본 채팅 기록을 확인할 수 있으며 서버에는 모드를 설치할 필요가 없습니다.

## 기능

- 글자 서식, Unicode 자소 및 단어 단위 표시, 시간 제어, 부분 글자 효과.
- 메시지 출처 필터, 대기열 우선순위, 애니메이션, 읽기 시간 및 접근성 설정.
- 시각적 위치 설정, 아홉 기준점, 너비 및 배율 핸들, 맞춤, 여섯 기본 HUD 프리셋.
- 로컬 OGG Vorbis 또는 WAV PCM을 쓰는 음성 프로필과 다섯 재생 모드.
- 영어, 중국어 간체, 일본어, 한국어 인터페이스.

## 버전 범위

| Minecraft | Fabric | NeoForge |
|---|---|---|
| 1.21 | Experimental | Experimental |
| 1.21.1 | Experimental | Experimental |
| 1.21.2 | Experimental | Experimental |
| 1.21.3 | Experimental | Experimental |
| 1.21.4 | Experimental | Experimental |
| 1.21.5 | Experimental | Experimental |
| 1.21.6 | Experimental | Experimental |
| 1.21.7 | Experimental | Experimental |
| 1.21.8 | Experimental | Experimental |
| 1.21.9 | Experimental | Experimental |
| 1.21.10 | Experimental | Experimental |
| 1.21.11 | Experimental | Experimental |
| 26.1 | Experimental | Experimental |
| 26.1.1 | Experimental | Experimental |
| 26.1.2 | Experimental | Experimental |

모두 실험적인 빌드 대상입니다. 정확한 Minecraft 버전과 Loader에 맞는 JAR을 선택하세요. 인증된 Signed Chat 검사는 아직 진행되지 않았으며 오프라인 개발 시나리오는 이를 검증하지 않습니다.

## 설치

현재 저장소에는 Release가 없습니다. 접근 권한이 있으면 성공한 [Actions 빌드](https://github.com/Axira-A/cinematic_chat_subtitless/actions/workflows/matrix.yml)에서 해당 JAR을 받거나 [소스에서 빌드](BUILDING.md)하세요. 해당 Fabric Loader 및 Fabric API 또는 NeoForge를 설치하고 클라이언트의 `mods/` 폴더에 JAR을 넣습니다. 공통 모듈은 JAR에 포함됩니다.

## 빠른 시작

1. 월드에 들어가 `/cchat config`로 자막을 설정합니다.
2. `/cchat preview`로 Steve와 Alex의 환영 메시지를 표시합니다.
3. `/cchat hud`로 위치를 조정하고 적용합니다.

`/cinematicchat`은 `/cchat`의 별칭입니다. `clear`, `reload`, `debug`, `parser <text>`, `voice`도 사용할 수 있습니다. 설정 및 HUD 단축키는 Minecraft 조작 설정에서 변경합니다. 기본적으로 채팅 화면에서 자막을 숨기고 일시 정지하며 F1은 자막을 숨기고 유사 음성을 음소거합니다.

## CCML

CCML은 로컬 자막 표시만 제어하며 원래 채팅과 기본 기록을 보존합니다. 기본 신뢰 모드는 SAFE입니다.

```text
[speed=30]안녕하세요. [color=#67BDE5]Cinematic Chat에 오신 것을 환영합니다.[/color][hold=3]
```

태그, 시간 제어, 이스케이프 및 신뢰 모드는 [CCML 안내](docs/ko-KR/chat-code.md)를 참조하세요.

## HUD

자막 영역을 드래그해 이동하고 가장자리로 너비, 모서리로 배율을 조정합니다. Shift는 축을 고정하고 Alt는 맞춤을 잠시 해제합니다. 직접 연 편집기는 적용으로 확정합니다. 설정 화면에서 열었다면 부모 설정 화면도 저장해야 합니다. [HUD 안내](docs/ko-KR/hud-editor.md)를 참조하세요.

## 유사 음성

기본적으로 꺼져 있습니다. 자막이 나타날 때 짧은 로컬 소리를 재생하며 음성 합성은 아닙니다. 프로필을 만들고 Steve 또는 Alex를 연결한 뒤 `config/cinematic_chat/audio/`의 파일을 선택하고 테스트하여 저장합니다. [음성 안내](docs/ko-KR/voice.md)를 참조하세요.

## 언어

Minecraft 언어 메뉴에서 선택합니다. 새 미리 보기는 현재 언어를 사용합니다. 실제 채팅 본문, 사용자 이름, 프로필 ID 및 파일 이름은 원래 값을 유지합니다.

## 빌드

1.21.x에는 JDK 21, 26.1.x에는 JDK 25가 필요합니다. 공통 Java 모듈의 대상은 Java 21입니다. [BUILDING.md](BUILDING.md)에 Wrapper, 개별 및 전체 빌드, 개발 시나리오와 의존성 검증을 설명합니다.

## 라이선스

[MIT](LICENSE). 포함된 자체 제작 테스트 소리와 재생성 방법은 [SOURCES.txt](adapters/common/minecraft/src/main/resources/assets/cinematic_chat/audio/SOURCES.txt)를 참조하세요.
