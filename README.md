# jump-to-spring

『점프 투 스프링부트』의 SBB(질문/답변 게시판)를 구현하면서, 장희성 강사의 수업 내용(14163, 14168)을 적용해 개선하는 복습용 프로젝트입니다.

## 기술 스택

- Java 25, Spring Boot 4.1.2, Gradle 9.8.2 (Kotlin DSL)
- Spring Web MVC, Lombok, DevTools
- (예정) Spring Data JPA, H2, Thymeleaf, Spring Security

## 참고 자료

- 점프 투 스프링부트: [교재](https://wikidocs.net/book/7601) / [GitHub](https://github.com/pahkey/sbb3)
- 14163 점프 투 스프링부트 개선: [강의](https://www.slog.gg/p/14163/raw) / [GitHub](https://github.com/jhs512/p-14163-2)
- 14168 JPA, 타임리프 개선: [강의](https://www.slog.gg/p/14168/raw) / [GitHub](https://github.com/jhs512/p-14168-2)
- 추가 기능 도전과제: [3-14 SBB 추가 기능 구현하기](https://wikidocs.net/162814)

## 실행 방법

```bash
./gradlew bootRun
```

브라우저에서 http://localhost:8080 접속 (포트 변경: `./gradlew bootRun --args=--server.port=8082`)

## 커밋 규칙

- 제목: `[순번] type(scope): 요약` — Conventional Commits + 순번
  - type: `feat` 기능, `fix` 버그, `refactor` 구조 개선, `test` 테스트, `docs` 문서, `chore` 설정/빌드, `style` 화면
- 본문: `작업 내용` / `작업 이유` / `검증`, 푸터 `Refs:` 에 교재·강의 출처
- 커밋 하나 = 작은 작업 하나 + 아래 작업내역 기록 하나

## 작업내역

### [001] chore(project): Spring Initializr로 스프링부트 프로젝트 생성
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/001)
- 출처: 교재 1장·2-01, 14163 2강
- 작업
  - start.spring.io로 프로젝트 생성 (Boot 4.1.2, Java 25, Gradle Kotlin DSL)
  - 패키지 `com.mysite.sbb`, 메인 클래스 `SbbApplication`
  - 의존성은 `webmvc`, `lombok`, `devtools`만 추가
- 이유
  - 강사의 최신 실습 환경과 맞춰 강의 코드와 1:1 비교가 가능하도록
  - JPA·H2·Thymeleaf는 교재에서 처음 등장할 때 추가해 각 의존성의 역할을 체감하기 위해
  - 교재(Boot 3)의 `spring-boot-starter-web`은 Boot 4에서 `spring-boot-starter-webmvc`로 이름이 바뀜
- 결과
  - `./gradlew test` 통과 (`contextLoads` 1건)
  - `bootRun` 후 `/` 접속 시 Whitelabel Error Page(404) 확인 — 서버는 떴지만 `/`를 처리할 컨트롤러가 아직 없다는 의미
  - JDK 25는 Gradle toolchain이 IntelliJ가 설치한 `~/.jdks`에서 자동 탐지 (별도 경로 설정 불필요)
