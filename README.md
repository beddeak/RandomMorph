# RandomMorph

일정 시간마다 플레이어가 랜덤한 몹으로 변신하는 Minecraft 플러그인입니다. 기본 변신 주기는 **60초**이며, 몹의 외형과 기본 능력치를 적용합니다.

**Minecraft Java Edition 26.3 · Paper · Java 25**

현재 **0.1.0 개발 중 버전**입니다. 이 저장소는 플러그인 소개와 배포를 위한 공개 저장소이며, 아직 배포용 JAR은 공개되지 않았습니다.

## 주요 기능

- 90종의 몹 카탈로그를 기반으로 랜덤 변신
- 몹의 체력·이동 속도·공격력·방어력 적용
- 변신 전후의 체력 비율 유지
- 플레이어별 타이머, 일시정지 및 재개
- 특정 몹 지정과 즉시 랜덤 재추첨
- 보스바·액션바로 현재 몹, 체력과 남은 시간 표시

## 실행 환경

| 항목 | 버전 |
| --- | --- |
| Minecraft / Paper | 26.3 |
| Java | 25 |
| LibsDisguises | 26.9.26 |
| PacketEvents | 2.14.0 · Spigot용 |

LibsDisguises와 PacketEvents는 필수이며, 서버에 별도로 설치해야 합니다.

## 설치 (JAR 배포 후)

1. Java 25로 실행하는 Paper 26.3 서버를 준비합니다.
2. 서버를 종료하고 `plugins/` 폴더에 다음 JAR을 넣습니다.
   - `RandomMorph-0.1.0.jar`
   - [LibsDisguises 26.9.26](https://github.com/libraryaddict/LibsDisguises/releases/tag/v26.9.26)
   - [PacketEvents 2.14.0](https://github.com/retrooper/packetevents/releases/tag/v2.14.0)의 Spigot용 JAR
3. 서버를 시작하고 플러그인이 활성화되었는지 확인합니다.
4. 서바이벌 또는 모험 모드에서 `/morph start`로 시작합니다.

## 명령어

권한: **`randommorph.admin`** · 기본 **OP**

| 명령어 | 설명 |
| --- | --- |
| `/morph start [player]` | 랜덤 변신 시작 |
| `/morph stop [player]` | 변신 종료 |
| `/morph pause [player]` | 타이머 일시정지 |
| `/morph resume [player]` | 타이머 재개 |
| `/morph reroll [player]` | 즉시 랜덤 재추첨 |
| `/morph next [player]` | reroll과 동일 |
| `/morph set <mob> [player]` | 특정 몹으로 변신 |
| `/morph status [player]` | 현재 상태 확인 |
| `/morph reload` | 설정 다시 불러오기 |
| `/morph list` | 사용 가능한 몹 ID 표시 |

`[player]`를 생략하면 자신에게 적용됩니다. 콘솔에서는 대상 플레이어 이름을 지정해야 합니다. 몹 ID와 플레이어 이름은 Tab 자동 완성을 지원합니다.
