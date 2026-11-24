# RandomMorph

플레이어가 일정 시간마다 다른 몹으로 변하는 Fabric 모드입니다.

[다운로드](https://github.com/beddeak/RandomMorph/releases/latest)

## 설치 및 설정

Minecraft Java 26.3, Java 25, Fabric Loader 0.19.5와 Fabric API 0.161.0+26.3을 사용합니다. 서버와 모든 참가자의 클라이언트에 Fabric API와 동일한 버전의 RandomMorph JAR을 각각 `mods/`에 넣고 실행하세요. 싱글플레이에서는 설치한 Fabric 프로필로 월드를 여세요.

첫 실행 후 `config/randommorph.json`에서 몹별 가중치와 능력 쿨다운을 설정할 수 있습니다. 변경 후 `/morph reload`로 적용하세요.

## 명령어

게임 관리자 권한이 필요합니다.

| 명령어 | 동작 |
|---|---|
| `/morph start [대상]` | 변신 시작. 대상을 생략하면 접속자 전체 |
| `/morph stop [대상]` | 변신 해제 |
| `/morph next [대상]`, `/morph reroll [대상]` | 다음 몹 추첨 |
| `/morph pause [대상]`, `/morph resume [대상]` | 타이머 일시정지·재개 |
| `/morph status [대상]` | 현재 상태 확인 |
| `/morph list`, `/morph set <몹> [대상]` | 지원 목록 조회·지정 변신 |
| `/morph size <1/2/4> [대상]` | 변신 크기 설정 |
| `/morph interval <5~3600> [대상]` | 변신 주기(초) 설정 |
| `/morph instincts <true/false>` | 추격·회피 본능 설정 |
| `/morph cooldown <몹> [primary/secondary] [초/reset]` | 쿨다운 조회·지정·기본값 복구(0~3600초) |
| `/morph cooldown reset`, `/morph reload` | 전체 쿨다운 복구·설정 다시 읽기 |
