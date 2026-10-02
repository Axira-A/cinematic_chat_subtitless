# CCML 채팅 코드

[English](../en/chat-code.md) · [简体中文](../zh-CN/chat-code.md) · [日本語](../ja-JP/chat-code.md) · [한국어](../ko-KR/chat-code.md)

CCML은 기본 게임이 수락한 플레이어 및 시스템 채팅(`/say` 포함)에서 읽습니다. 출처 설정과 신뢰 모드에 따라 분석 여부가 결정됩니다. `/cchat parser <text>`는 로컬에서 스크립트를 검사하고 `/cchat preview`는 현재 언어의 환영 메시지를 만듭니다. 다른 플레이어에게 보내지 않습니다.

```text
[speed=30]안녕하세요. [color=#67BDE5][bold]Cinematic Chat에 오신 것을 환영합니다.[/bold][/color][hold=3]
```

| 태그 | 의미 |
|---|---|
| `[pause=0.5]` | 앞의 글자가 모두 나타난 뒤 지정한 초만큼 정지합니다. |
| `[wait]` | 앞의 유한 등장 및 표시 애니메이션을 기다립니다. 계속되는 shake/wave는 기다리지 않습니다. |
| `[hold=3]` | 전체 글자 표시 후 최소 유지 시간이며 여러 값 중 최댓값을 씁니다. |
| `[speed=30]`, `[speed=default]` | 초당 표시 단위 또는 설정 속도입니다. |
| `[reveal=instant]` | `instant`, `typewriter`, `smooth`, `word` 중 선택합니다. |
| `[clear]`, `[replace]` | 현재 entry의 글자를 지우고 뒤의 글자로 바꿉니다. |
| `[erase=2]` | 현재 entry 끝에서 최대 두 자소를 삭제합니다. |
| `[fadein=0.2]`, `[fadeout=0.3]` | 등장 및 퇴장 페이드 초이며 0–3으로 제한합니다. |
| `[br]` | 줄바꿈입니다. `\n`도 사용할 수 있습니다. |
| `[color=#67BDE5]…[/color]` | RGB 색입니다. |
| `[scale=1.2]…[/scale]`, `[alpha=0.8]…[/alpha]` | 부분 배율 및 불투명도입니다. |
| `[bold]…[/bold]`, `[italic]…[/italic]`, `[underline]…[/underline]` | 굵게, 기울임, 밑줄입니다. |
| `[shake strength=0.7 frequency=12]…[/shake]` | 흔들림 크기와 빈도입니다. |
| `[wave amplitude=2 frequency=2]…[/wave]` | 물결 크기와 빈도입니다. |

서식은 중첩할 수 있으며 열었던 역순으로 닫습니다. `\[`와 `\]`는 대괄호, `\\`는 역슬래시, `\n`은 줄바꿈입니다. 알 수 없는 태그, 잘못된 인수 및 짝이 맞지 않는 태그는 일반 글자로 표시합니다. 유효한 숫자가 범위를 벗어나면 제한합니다. 표시와 삭제는 Unicode 자소 단위이며 연속된 CJK는 단어 모드에서도 자소 단위를 사용합니다.

SAFE 기본 제한은 pause ≤3초, hold ≤10초, scale 0.75–1.5, shake ≤1픽셀, wave ≤2픽셀, 누적 ≤60초입니다. ALL도 절대 제한을 지키며 누적은 ≤120초입니다. SYSTEM_ONLY는 구조적으로 시스템으로 분류한 출처만 분석하고 UNKNOWN은 포함하지 않습니다. OFF는 태그를 일반 글자로 표시합니다. 모든 모드에서 입력 길이, 노드, 깊이 및 효과를 제한합니다. 누적 시간 제한에 도달하면 나머지를 일반 글자로 표시합니다. 태그는 파일을 읽거나 명령을 실행하거나 패킷을 보내지 않습니다. 원래 Component와 기록을 보존합니다.

[README](../../README.ko-KR.md)
