# RandomMorph

플레이어가 일정 시간마다 무작위 몹으로 변신하고, 몹별 외형·능력치·고유 능력을 사용하는 Minecraft **Fabric 모드**입니다. 현재 배포 버전은 **0.6.3+26.3**이며 기본 변신 주기는 **60초**입니다.

[최신 릴리스](https://github.com/beddeak/RandomMorph/releases/latest) · [0.6.3+26.3 JAR 다운로드](https://github.com/beddeak/RandomMorph/releases/download/v0.6.3%2B26.3/RandomMorph-Fabric-0.6.3%2B26.3.jar)

## 주요 기능

- 90종의 몹 외형과 종별 능력치·이동·호흡 특성 적용
- F 주 능력, Shift+F 보조 능력과 몹별 쿨다운 설정
- 변신 전후 체력 비율 유지, 플레이어별 타이머·일시정지·재추첨
- 몹의 종에 따른 주변 몹의 공격·회피·감지 관계 적용
- 기본 체력·공기 HUD와 현재 변신·남은 시간·능력 상태 표시

지원하는 능력과 행동은 종마다 다릅니다.

## 실행 환경

| 항목 | 요구 버전 |
| --- | --- |
| Minecraft Java Edition | 26.3 |
| Java | 25 이상 |
| Fabric Loader | 0.19.5 이상 |
| Fabric API | 0.161.0+26.3 이상, Minecraft 26.3용 |

## 설치

1. Minecraft 26.3에 맞는 Fabric Loader와 Fabric API를 준비합니다.
2. 게임·서버를 종료한 상태에서 `RandomMorph-Fabric-0.6.3+26.3.jar`와 Fabric API JAR을 `mods/` 폴더에 넣습니다. 서버의 `mods/`와 각 참가자의 게임 폴더 `mods/`에 모두 필요하며, LAN 플레이도 호스트와 참가자 모두 같은 버전을 사용합니다.
3. Fabric으로 게임·서버를 다시 실행합니다.
4. OP 또는 치트 권한으로 `/morph start`를 입력하면 온라인 참가자 전원이 3초 카운트다운 후 시작합니다. 한 명만 시작하려면 `/morph start 닉네임`을 사용합니다.

## 명령어

OP 또는 치트 권한이 필요합니다. `[targets]`에는 플레이어 이름이나 `@a` 같은 선택자를 사용할 수 있습니다.

| 명령어 | 설명 |
| --- | --- |
| `/morph start [targets]` | 3초 카운트다운 후 변신 시작, 대상 생략 시 온라인 전원 |
| `/morph stop [targets]` | 변신 종료 |
| `/morph pause [targets]` | 타이머 일시정지 |
| `/morph resume [targets]` | 타이머 재개 |
| `/morph reroll [targets]` | 즉시 재추첨, `/morph next`도 동일 |
| `/morph set <mob> [targets]` | 지정한 몹으로 변신 |
| `/morph status [targets]` | 현재 변신·남은 시간·주기 확인 |
| `/morph interval <초> [targets]` | 변신 주기 설정, 5~3600초 |
| `/morph cooldown <mob> [slot]` | 몹별 능력 쿨다운 조회 |
| `/morph cooldown <mob> [slot] <초 또는 reset>` | 쿨다운 설정 또는 해당 슬롯 기본값 복원, 0~3600초 |
| `/morph cooldown reset` | 모든 능력 쿨다운 기본값 복원 |
| `/morph instincts <true 또는 false>` | 플레이어 자동 추격·회피 보조 설정, 기본 꺼짐 |
| `/morph list` | 지원하는 몹 ID 표시 |
| `/morph reload` | 설정 다시 불러오기 |

`start`를 제외한 플레이어 대상 명령은 `[targets]` 생략 시 자신에게 적용되며, 콘솔에서는 대상을 지정합니다. `interval`은 대상 생략 시 전체·기본 주기를 변경하고, 대상 지정 시 현재 참가 세션에만 적용합니다. `slot`은 `primary`(F) 또는 `secondary`(Shift+F)이며 생략 시 쿨다운 조회는 두 슬롯, 설정은 F에 적용됩니다. 쿨다운 0은 대기 없음입니다.

예: `/morph set sheep @a`, `/morph interval 60`, `/morph cooldown blaze primary 9`.
