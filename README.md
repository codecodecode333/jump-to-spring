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

### [002] feat(main): MainController 추가, void 액션 메서드
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/002)
- 출처: 교재 2-02, 14163 3강
- 작업
  - `@Controller` 클래스 `MainController`에 `GET /`, `GET /hello` 액션 메서드 추가 (반환 타입 `void`, 콘솔 출력만)
- 이유
  - 요청이 `DispatcherServlet → HandlerMapping → 액션 메서드`로 흐르는 과정을 확인하기 위해
  - `@ResponseBody`가 왜 필요한지 500 에러로 먼저 체감하기 위해 (강사 방식, 교재는 바로 `@ResponseBody` 사용)
  - 컨트롤러는 메인 클래스 패키지(`com.mysite.sbb`) 하위에 둬야 컴포넌트 스캔 대상이 됨
- 결과
  - `/not-exist` → 404 (매핑된 메서드 없음)
  - `/`, `/hello` → 500. 서버 로그에 `index() 메서드 호출됨!`이 찍혀 **메서드는 실행됐음**을 확인
  - 원인: `Circular view path [hello]` — `void`면 스프링이 URL을 뷰(템플릿) 이름으로 쓰는데, 템플릿 엔진이 없어 다시 같은 URL로 포워드하려다 실패

### [003] feat(main): @ResponseBody로 문자열 응답 반환
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/003)
- 출처: 교재 2-02, 14163 4강
- 작업
  - `index()`, `hello()`에 `@ResponseBody`를 붙이고 반환 타입을 `String`으로 변경 (`"SBB"`, `"안녕하세요 ^ ^"`)
  - **(개선)** `MainControllerTest` 추가 — `@WebMvcTest` + `MockMvcTester`로 상태코드와 응답 본문 검증
- 이유
  - `@ResponseBody`가 있으면 반환값이 뷰 이름이 아니라 **응답 본문 그 자체**가 됨 → [002]의 500 해결
  - 강사 코드에는 테스트가 없지만, 브라우저로 매번 확인하지 않아도 기능이 깨졌는지 자동으로 알기 위해 테스트 추가
  - Boot 4에서는 `@WebMvcTest` 위치가 `org.springframework.boot.webmvc.test.autoconfigure`로 바뀜 (Boot 3: `...boot.test.autoconfigure.web.servlet`)
  - `MockMvcTester`(Spring 6.2+)는 AssertJ 스타일이라 기존 `MockMvc` + `andExpect` 체인보다 읽기 쉬움
- 결과
  - `/` → `SBB`, `/hello` → `안녕하세요 ^ ^` (둘 다 200, `text/plain;charset=UTF-8`, 한글 정상)
  - `./gradlew test` 3건 통과
