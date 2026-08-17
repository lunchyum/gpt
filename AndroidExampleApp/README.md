# 오늘의 학교

나이스(NEIS) Open API에서 학교 시간표와 급식 데이터를 가져와 Material 3 Expressive UI로 보여주고, 선택한 정보를 라이브 배경화면으로 적용하는 Android 앱입니다.

## 주요 기능

- 초등학교 / 중학교 / 고등학교 시간표 조회
- 날짜별 급식 조회
- 시간표 / 급식 / 둘 다 중 배경화면 콘텐츠 선택
- Android 라이브 배경화면 서비스로 적용
- 나이스 API 키와 학교 정보를 기기에 저장하고 요청 시 사용
- Android 12+ 동적 색상 + Material 3 Expressive 테마
- GitHub Actions에서 debug APK 빌드

## 설정

나이스 교육정보 개방 포털에서 API 인증키를 발급받은 뒤 앱에서 API 키, 교육청 코드, 학교 코드를 입력합니다. 시간표는 학교급, 학년, 반을 추가로 입력합니다.

급식 API는 `mealServiceDietInfo`, 시간표는 학교급에 따라 `elsTimetable`, `misTimetable`, `hisTimetable` 엔드포인트를 사용합니다.

## 빌드

```bash
cd AndroidExampleApp
./gradlew assembleDebug
```

AGP 9.1.1은 Gradle 9.3.1을 요구하므로 wrapper도 9.3.1에 맞춰져 있습니다.
