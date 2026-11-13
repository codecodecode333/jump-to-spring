# jump-to-spring

『점프 투 스프링부트』의 SBB(질문/답변 게시판)를 구현하면서, 장희성 강사의 수업 내용(14163, 14168)을 적용해 개선하는 복습용 프로젝트입니다.

## 기술 스택

- Java 25, Spring Boot 4.1.2, Gradle 9.8.2 (Kotlin DSL)
- Spring Web MVC, Spring Data JPA, H2, Lombok, DevTools
- (예정) Thymeleaf, Spring Security

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

- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:./db_dev;MODE=MySQL`, 사용자 `sa`, 비밀번호 없음)

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

### [004] chore(db): JPA·H2 의존성 추가, yaml 설정과 dev/test 프로필 분리
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/004)
- 출처: 교재 2-03, 14163 5~6강, 14168 최신 실습 환경
- 작업
  - 의존성: `spring-boot-starter-data-jpa`, `h2`(runtimeOnly), `spring-boot-h2console`, `spring-boot-starter-data-jpa-test`
  - `application.properties` → `application.yaml` + `application-dev.yaml` / `application-test.yaml`
  - dev: 파일 DB `jdbc:h2:./db_dev;MODE=MySQL`, test: 메모리 DB `jdbc:h2:mem:db_test;MODE=MySQL`
  - SQL 로그(`show-sql`, `format_sql`, 바인딩 값 TRACE), 트랜잭션 로그 TRACE
  - `SbbApplicationTests`에 `@ActiveProfiles("test")`, `.gitignore`에 `db_dev.*`
- 이유
  - **(개선) yaml**: 계층 구조라 `spring.jpa.hibernate.ddl-auto`처럼 긴 키의 중복이 사라지고 읽기 쉬움
  - **(개선) 프로필 분리**: 개발 DB(파일, 데이터 유지)와 테스트 DB(메모리, 매번 초기화)를 나눠 테스트가 개발 데이터를 오염시키지 않게
  - **(개선) `MODE=MySQL`**: 나중에 운영 DB를 MySQL로 바꿔도 SQL 문법 차이로 인한 문제를 줄이기 위해
  - 교재(Boot 3)는 H2 콘솔이 내장이지만 Boot 4는 `spring-boot-h2console` 모듈을 따로 추가해야 함
  - DB 파일은 각자 PC에서 생성되는 실행 결과물이므로 git에서 제외
- 결과
  - `./gradlew test` 3건 통과 (test 프로필로 메모리 DB 사용)
  - `/h2-console` 접속 → JDBC URL이 설정값으로 자동 채워지고 Connect 성공

### [005] feat(question): Question·Answer 엔티티 추가, BaseEntity로 공통 필드 분리
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/005)
- 출처: 교재 2-04, 14163 7~13강·22~25강, 14168 BaseEntity
- 작업
  - `BaseEntity`(`@MappedSuperclass`): `id`, `createDate`(`@CreatedDate`), `modifyDate`(`@LastModifiedDate`), `equals/hashCode`
  - `JpaConfig`에 `@EnableJpaAuditing`
  - `Question(subject, content, answers)`, `Answer(content, question)` 엔티티
- 이유 (교재와 다른 점)
  - **(개선) BaseEntity**: 모든 엔티티에 반복되는 id·작성일·수정일을 한 곳에 모으고, 날짜는 JPA Auditing이 자동으로 채움 → `setCreateDate(LocalDateTime.now())` 반복 제거 (14168)
  - **(개선) Setter 제거 + `@NoArgsConstructor(access = PROTECTED)`**: 아무 데서나 값을 바꾸지 못하게 하고, 생성자와 의미 있는 메서드(`addAnswer`)로만 상태를 바꿈
  - **(개선) `question.addAnswer(content)`**: 답변을 질문을 통해 만들어 양방향 관계(`answer.question`과 `question.answers`)가 항상 함께 맞춰짐 (14163 21강)
  - **(개선) `@ManyToOne(fetch = LAZY)`**: 기본값 EAGER는 답변만 필요할 때도 질문을 조인해서 가져옴 (14163 24~25강)
  - **(개선) `answers = new ArrayList<>()` 초기화**: 새로 만든 질문에서도 NPE 없이 바로 `add` 가능 (14163 23강)
  - **(개선) `orphanRemoval = true`**: 리스트에서 뺀 답변은 DB에서도 삭제 → 나중에 답변 삭제를 `question.getAnswers().remove(...)`로 처리 가능
  - **(개선) `id`를 `Long`으로**: 교재는 `Integer`, 강사는 `int`. `null`이면 "아직 저장 안 됨"이 명확하고 범위도 넓음
  - **(개선) equals/hashCode 보완**: 14168은 `getClass()` 비교 + `id` 해시 → LAZY 프록시와 비교하면 false, 저장 전후로 해시가 바뀜. `Hibernate.getClass()` 비교 + 클래스 기반 해시로 보완
  - `@EnableJpaAuditing`을 메인 클래스가 아닌 `JpaConfig`에 둔 이유: `@WebMvcTest`가 메인 클래스를 읽을 때 JPA가 없어 실패하는 문제를 피하기 위해
- 결과
  - 앱 실행 시 `question`, `answer` 테이블 생성, `answer.question_id` 외래키(FK) 생성 확인 (id는 `bigint identity`)
  - `./gradlew test` 3건 통과

### [006] feat(question): Question·Answer 리포지터리와 쿼리 메서드, 리포지터리 테스트
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/006)
- 출처: 교재 2-05, 14163 14~25강
- 작업
  - `QuestionRepository`(`findBySubject`, `findBySubjectAndContent`, `findBySubjectLike`), `AnswerRepository`
  - `Question.modify(subject, content)` 추가
  - `QuestionRepositoryTest` 11건: 저장, 조회, 쿼리 메서드, 수정, 삭제, 답변 생성·조회, orphanRemoval
- 이유
  - 교재처럼 리포지터리 사용법을 **테스트 코드로** 익힘 (교재·강사 방식)
  - **(개선) `@DataJpaTest`**: 강사는 `@SpringBootTest`(앱 전체)를 띄웠지만, 리포지터리 테스트는 JPA 부분만 띄우면 더 빠르고 범위가 명확함
  - **(개선) 테스트가 스스로 데이터 준비(`@BeforeEach`)**: 강사 테스트는 샘플 데이터(`TestInitData`)에 의존해 `findById(1)`처럼 id를 가정함 → 샘플 데이터를 바꾸면 테스트가 깨짐. 각 테스트가 자기 데이터를 만들고 롤백되게 함
  - **(개선) `em.flush()` + `em.clear()`**: 1차 캐시를 비워야 "정말 DB에 반영됐는지"를 확인할 수 있음 (변경 감지 UPDATE, cascade INSERT/DELETE 시점 학습)
  - **(개선) 수정은 `save()` 재호출 없이 변경 감지(Dirty Checking)**: 교재는 `setSubject` 후 `save()`를 다시 호출함
- 결과
  - `./gradlew test` 14건 통과

### [007] feat(global): 개발용 샘플 데이터 자동 생성 (BaseInitData)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/007)
- 출처: 14163 14강(TestInitData), 14168 2강(BaseInitData)
- 작업
  - `ApplicationRunner`로 앱 시작 직후 질문 2개 + 답변 2개 생성, 이미 데이터가 있으면 건너뜀
  - `@Profile("dev")`로 개발 환경에서만 실행
- 이유
  - 화면을 만들 때마다 H2 콘솔에서 손으로 데이터를 넣지 않아도 되게 (교재에는 없는 강사 방식)
  - `self`(@Lazy 자기 주입)로 호출하는 이유: `@Transactional`은 프록시를 거쳐야 동작하는데, `this.work1()`은 프록시를 거치지 않음
  - **(개선) `@Profile("dev")`**: 강사 코드는 테스트에서도 샘플 데이터가 들어가서 테스트 결과가 샘플 데이터에 따라 바뀜. [006]의 테스트는 자기 데이터를 직접 만드므로 테스트에서는 꺼 둠
- 결과
  - 개발 서버 재시작 시 `insert into question`, `insert into answer` SQL 로그 확인
  - `./gradlew test` 14건 통과 (test 프로필에서는 샘플 데이터 미생성)
