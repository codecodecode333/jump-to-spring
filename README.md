# jump-to-spring

『점프 투 스프링부트』의 SBB(질문/답변 게시판)를 구현하면서, 장희성 강사의 수업 내용(14163, 14168)을 적용해 개선하는 복습용 프로젝트입니다.

## 기술 스택

- Java 25, Spring Boot 4.1.2, Gradle 9.8.2 (Kotlin DSL)
- Spring Web MVC, Spring Data JPA, H2, Thymeleaf(+Layout Dialect), Validation, Spring Security, Lombok, DevTools

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

### [008] refactor(global): 도메인별 패키지 구조로 정리 (domain / global)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/008)
- 출처: 교재 2-06, 14163 26강, 14168 패키지 구조
- 작업
  ```
  com.mysite.sbb
  ├─ domain
  │  ├─ home/home/controller/MainController
  │  └─ question
  │     ├─ question/{entity/Question, repository/QuestionRepository}
  │     └─ answer/{entity/Answer, repository/AnswerRepository}
  └─ global
     ├─ jpa/{entity/BaseEntity, config/JpaConfig}
     └─ initData/BaseInitData
  ```
  - 테스트도 같은 패키지 구조로 이동
- 이유
  - 교재는 `question`, `answer`처럼 도메인별로만 나누고 한 폴더에 엔티티·리포지터리·컨트롤러를 섞어 둠
  - **(개선) `domain/{묶음}/{도메인}/{계층}` (14168)**: 답변은 질문에 딸린 개념이므로 `question` 묶음 아래 `question`, `answer`를 두고, 그 안을 `entity / repository / service / controller` 계층으로 나눔 → 파일이 많아져도 위치를 예측할 수 있음
  - **(개선) `global`**: 특정 도메인에 속하지 않는 공통 코드(BaseEntity, JPA 설정, 초기 데이터, 나중에 보안·예외 처리)를 분리
  - 모든 패키지가 메인 클래스(`com.mysite.sbb`) 하위라서 컴포넌트 스캔에는 영향 없음
- 결과
  - 동작 변화 없음, `./gradlew test` 14건 통과

### [009] feat(question): 질문 목록 페이지 (GET /questions)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/009)
- 출처: 교재 2-07, 14163 27~32강, 14168 3·18강
- 작업
  - Thymeleaf 의존성 추가 (`starter-thymeleaf`, `starter-thymeleaf-test`)
  - `QuestionController.list()` → `Model`에 `questions` 담고 `question/question/list` 템플릿 렌더링
  - dev 프로필: `thymeleaf.cache: false`, `thymeleaf.prefix: file:src/main/resources/templates/`
  - `QuestionControllerTest`: `@SpringBootTest` + `@AutoConfigureMockMvc` 통합 테스트
- 이유
  - **(개선) URL `GET /questions`**: 교재 `/question/list`는 동사(list)가 URL에 들어감. 자원(questions)을 복수형 명사로 표현하고 행위는 HTTP 메서드로 구분 (14163 37강 "HTTP URL의 좋은 예와 나쁜 예")
  - **(개선) 템플릿을 패키지 구조처럼 `question/question/list.html`에 배치 (14168)**: 템플릿이 늘어나도 도메인별로 찾기 쉬움
  - **(개선) `prefix: file:...`**: 빌드된 결과물(`build/resources`)이 아닌 소스 폴더를 직접 읽어서, 템플릿 수정 후 새로고침만 하면 반영됨 (14168 18강)
  - `Model`: 컨트롤러가 템플릿에 데이터를 넘기는 상자. 스프링이 액션 메서드 호출 시 자동으로 만들어 넣어 줌 (14163 28강)
- 결과
  - 브라우저 `/questions`에서 샘플 질문 2건의 제목·작성일시 표시
  - `./gradlew test` 15건 통과

### [010] feat(home): 루트 URL을 질문 목록으로 리다이렉트
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/010)
- 출처: 교재 2-08, 14163 33강
- 작업
  - `GET /` → `redirect:/questions`, `MainControllerTest`를 리다이렉트 검증으로 수정
- 이유
  - 사이트 첫 화면에 들어오면 바로 질문 목록을 보여 주기 위해
  - `redirect:`는 서버가 **302 + Location 헤더**를 보내고 브라우저가 그 주소로 다시 요청함 → 주소창도 `/questions`로 바뀜 (`forward:`는 서버 안에서만 넘겨서 주소창이 그대로)
- 결과
  - `GET /` → 302, `Location: /questions`
  - `./gradlew test` 15건 통과

### [011] refactor(question): QuestionService 도입, 컨트롤러는 서비스를 통해 조회
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/011)
- 출처: 교재 2-09, 14163 34강
- 작업
  - `QuestionService.getList()` 추가, `QuestionController`가 리포지터리 대신 서비스 사용
  - 서비스 클래스에 `@Transactional(readOnly = true)`
- 이유
  - 역할 분리: 컨트롤러는 요청/응답, 서비스는 업무 로직, 리포지터리는 DB 접근
  - 여러 컨트롤러가 같은 로직을 재사용할 수 있고, 나중에 엔티티 대신 DTO로 바꾸는 등의 변경을 서비스 한 곳에서 처리
  - **(개선) 클래스 레벨 `@Transactional(readOnly = true)`**: 교재 서비스에는 트랜잭션 설정이 없음. 조회는 읽기 전용(변경 감지 생략)으로, 쓰기 메서드에만 `@Transactional`을 따로 붙이는 방식
- 결과
  - 동작 변화 없음, `./gradlew test` 15건 통과

### [012] feat(question): 질문 상세 페이지 (GET /questions/{id}), 없는 질문은 404
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/012)
- 출처: 교재 2-10, 14163 35강
- 작업
  - `QuestionService.getQuestion(id)`: 없으면 `DataNotFoundException`
  - `global/exception/DataNotFoundException` (`@ResponseStatus(NOT_FOUND)`)
  - `QuestionController.detail()` + `question/question/detail.html`, 목록 제목에 상세 링크 (`@{|/questions/${question.id}|}`)
  - 테스트: 상세 200, 없는 id는 404
- 이유
  - **(개선) URL `GET /questions/{id}`**: 교재 `/question/detail/{id}` → "5번 질문"이라는 자원 자체를 주소로 표현
  - **커스텀 예외**: 그냥 `get()`을 부르면 `NoSuchElementException` → 500(서버 잘못). 실제로는 "요청한 데이터가 없음"이므로 404가 맞고, 예외 이름만 보고도 상황을 알 수 있음 (14163 35강)
  - **(개선) `orElseThrow`**: 교재의 `isPresent()` + `get()` 조합보다 짧고, `get()`을 실수로 먼저 부를 일이 없음
  - Thymeleaf `@{|...|}`: `@{}`는 URL(컨텍스트 경로 자동 처리), `|...|`는 문자열 안에 변수를 끼워 넣는 리터럴 치환 (14163 35강)
- 결과
  - `/questions/1` → 200, `/questions/999` → 404
  - `./gradlew test` 17건 통과

### [013] refactor(question): @RequestMapping으로 URL 프리픽스 공통화
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/013)
- 출처: 교재 2-11, 14163 36강
- 작업
  - `QuestionController`에 `@RequestMapping("/questions")`, 메서드는 `@GetMapping`, `@GetMapping("/{id}")`
- 이유
  - 같은 컨트롤러의 URL이 모두 `/questions`로 시작하므로 한 곳에 모아 중복을 없애고, 프리픽스를 바꿀 때 한 줄만 고치면 되게
  - 클래스 이름과 URL 프리픽스가 1:1로 대응되어 "이 URL은 어느 컨트롤러?"를 바로 찾을 수 있음
- 결과
  - URL 변화 없음, `./gradlew test` 17건 통과

### [014] feat(answer): 답변 등록 (POST /questions/{id}/answers)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/014)
- 출처: 교재 2-12, 14163 37강
- 작업
  - `AnswerService.create(questionId, content)`: 질문을 조회한 뒤 `question.addAnswer(content)`
  - `AnswerController`(`@RequestMapping("/questions/{questionId}/answers")`, `@PostMapping`)
  - 상세 페이지에 답변 개수, 답변 목록, 답변 등록 폼 추가
  - `AnswerControllerTest`: 등록 후 리다이렉트, 없는 질문 404
- 이유
  - **(개선) URL `POST /questions/{id}/answers`**: 교재 `POST /answer/create/{id}`는 동사(create)가 들어가고 답변이 어느 질문에 속하는지 URL에 드러나지 않음. 답변은 질문의 하위 자원이고 POST가 이미 "생성"이라는 뜻 (14163 37강, 14168 26강)
  - **(개선) `answerRepository.save()` 대신 `question.addAnswer()`**: 트랜잭션 안에서 영속 상태인 질문에 답변을 추가하면 cascade PERSIST로 커밋 시점에 INSERT됨 (14163 37강 강사 커스터마이징)
  - **(개선) 트랜잭션은 서비스에**: 강사는 컨트롤러에 `@Transactional`을 붙였지만, 트랜잭션 경계를 서비스로 모아 컨트롤러는 요청/응답만 담당하게
  - **PRG(Post-Redirect-Get)**: 등록 후 화면을 바로 그리지 않고 리다이렉트 → 새로고침해도 POST가 다시 전송되지 않음
- 결과
  - 브라우저에서 1번 질문에 답변 등록 → `/questions/1`로 리다이렉트되고 "1개의 답변이 있습니다." 표시
  - `./gradlew test` 19건 통과

### [015] style(question): 상세 페이지에 스타일시트 적용 (static/resource)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/015)
- 출처: 교재 2-13, 14163 38강, 14168 46강
- 작업
  - `static/resource/style.css` (답변 입력창 너비 100%, 버튼 여백), 상세 페이지에 `<link th:href="@{/resource/style.css}">`
- 이유
  - `static` 폴더의 파일은 컨트롤러 없이 URL로 바로 제공됨 (`static/resource/style.css` → `/resource/style.css`)
  - **(개선) `static/resource/` 하위에 배치 (14168 46강)**: 교재는 `static/style.css`. 나중에 스프링 시큐리티에서 "정적 파일은 모두 허용"을 `/resource/**` 한 줄로 설정할 수 있음
- 결과
  - `/resource/style.css` → 200 `text/css`
  - 정적 파일은 빌드 결과물(`build/resources`)에서 제공되므로, 새 파일 추가 후에는 빌드(또는 IDE의 Build) 필요

### [016] style(question): 부트스트랩(cdnjs)과 프리텐다드 폰트 적용
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/016)
- 출처: 교재 2-14, 14163 39강
- 작업
  - Bootstrap 5.3.8 CSS를 cdnjs에서, Pretendard 폰트를 jsDelivr에서 불러옴
  - 목록: `table` + 번호(`loop.count`) + 날짜 포맷(`#temporals.format`), 상세: `card`로 질문/답변 표시, 폼에 `form-control`, `btn`
- 이유
  - **(개선) CDN 사용 (14163 39강)**: 교재는 `bootstrap.min.css`를 내려받아 `static`에 복사함 → 저장소에 남의 라이브러리 파일이 섞이고 버전 업그레이드 시 파일을 다시 받아야 함. CDN은 URL의 버전만 바꾸면 되고 브라우저 캐시도 공유됨
  - **(개선) 프리텐다드 폰트 (14163 39강)**: 한글 가독성이 좋은 오픈 폰트. 가변(Variable) + 동적 서브셋 CSS 한 줄로, 실제 사용하는 글자만 내려받음
  - `#temporals.format`: `LocalDateTime`의 `2026-11-13T14:18:45.960484` 같은 기본 출력을 `yyyy-MM-dd HH:mm`으로
  - `loop.count`: `th:each`의 상태 변수로 1부터 세는 번호 (DB id와는 다름)
- 결과
  - 목록/상세 화면에 부트스트랩 스타일 적용 확인 (스크린샷)
  - `./gradlew test` 19건 통과

### [017] refactor(global): 표준 HTML 구조와 공통 레이아웃 (layout.html)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/017)
- 출처: 교재 2-15, 14168 44~45강
- 작업
  - `thymeleaf-layout-dialect` 의존성, `templates/layout.html` (`<!DOCTYPE>`, `lang`, `meta charset/viewport`, CSS/JS 링크)
  - 각 페이지는 `layout:decorate="~{layout}"` + `layout:fragment="content"`만 작성
  - `layout:title-pattern="$CONTENT_TITLE | $LAYOUT_TITLE"`로 페이지별 제목 (`질문 목록 | SBB`, `질문 제목 | SBB`)
  - Bootstrap JS 번들도 레이아웃에서 한 번 로드 (다음 단계 내비게이션 바에서 사용)
- 이유
  - 각 페이지에 `<head>`, CSS 링크가 반복되던 것을 한 곳으로 → CSS를 바꿀 때 레이아웃 한 파일만 수정
  - `<html>`, `<head>`, `<body>`를 갖춘 표준 HTML이어야 브라우저가 문서를 정확히 해석함 (교재 2-15)
  - **(개선) `title-pattern` (14168 45강)**: 교재는 모든 페이지 제목이 같음. 브라우저 탭/즐겨찾기에서 페이지를 구분할 수 있게
  - **(개선) `xmlns:th`, `xmlns:layout` 선언 (14168 44강)**: IDE의 `Namespace 'layout' is not bound` 경고 제거
- 결과
  - `/questions` → `<title>질문 목록 | SBB</title>`, `/questions/2` → `<title>스프링부트 모델 질문입니다. | SBB</title>`
  - `./gradlew test` 19건 통과

### [018] feat(question): 질문 등록 폼과 저장 (GET·POST /questions/write)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/018)
- 출처: 교재 2-16, 14168 3~7강·26강
- 작업
  - `QuestionService.create(subject, content)` (`@Transactional`)
  - `GET /questions/write` 폼, `POST /questions/write` 저장 후 새 질문 상세로 리다이렉트
  - 목록 하단에 "질문 등록하기" 버튼, `write.html`
- 이유
  - **(개선) 같은 URL + 다른 HTTP 메서드 (14168 26강)**: GET은 "폼 보여주기", POST는 "처리하기". `th:action`을 비워 두면 현재 URL로 전송되어 폼 주소를 따로 적지 않아도 됨 (14168 5강)
  - 데이터를 바꾸는 요청은 반드시 POST (GET은 조회 전용, 주소창·로그·캐시에 남음, 14168 7강)
  - **(개선) 저장 후 목록이 아니라 방금 쓴 질문의 상세로 이동**: 교재는 목록으로 이동. 작성 결과를 바로 확인할 수 있게
- 결과
  - 브라우저에서 질문 등록 → `/questions/3`으로 이동, 제목·내용 표시 (탭 제목 `테스트 질문입니다 | SBB`)
  - `./gradlew test` 21건 통과

### [019] feat(question): 질문 등록 입력값 검증 (record 폼, @Valid, 오류 프래그먼트)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/019)
- 출처: 교재 2-16, 14168 9~25강·40강
- 작업
  - `spring-boot-starter-validation` 의존성
  - `QuestionController.WriteForm` record: `@NotBlank`, `@Size(max = 200)`
  - `@ModelAttribute("form") @Valid WriteForm form, BindingResult bindingResult` → 실패 시 폼 다시 렌더링
  - `write.html`: `th:object="${form}"`, `th:field="*{subject}"`
  - `templates/form_errors.html` 프래그먼트: 오류를 번호순으로 정렬해 표시하고 첫 오류 필드에 포커스
  - 테스트: 빈 값이면 저장 안 됨 + 오류 메시지, 입력값 유지
- 이유
  - 빈 제목/내용이 DB에 저장되는 것을 막기 위해 (서버 검증은 JS를 꺼도 우회할 수 없음, 14168 35~36강)
  - **(개선) 폼 DTO를 record로 (14168 40강)**: 교재 `QuestionForm`은 `@Getter @Setter` 클래스 + 별도 파일. record는 한 줄로 불변 객체를 만들고, 이 컨트롤러에서만 쓰므로 컨트롤러 안에 둠
  - **`BindingResult`**: 없으면 검증 실패 시 바로 400 에러 페이지. 있으면 오류를 담아 컨트롤러가 직접 처리 (14168 13강)
  - **`th:field`**: `id`, `name`, `value`를 자동 생성 → 검증 실패 후에도 입력값이 남아 있음 (14168 10·22강)
  - **(개선) 오류 메시지 `01-subject-...` 규칙 (14168 23~25강)**: `#fields.allErrors()`는 순서가 보장되지 않음. 번호로 정렬하고, 필드명으로 첫 오류 입력칸에 포커스를 줘서 사용자가 바로 고칠 수 있게
- 결과
  - 제목만 입력하고 저장 → 같은 페이지에 "내용은 필수항목입니다." 표시, 제목 입력값 유지 (스크린샷)
  - `./gradlew test` 23건 통과

### [020] feat(answer): 답변 등록 입력값 검증
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/020)
- 출처: 교재 2-16, 14168 9~25강
- 작업
  - `AnswerWriteForm` record(`@NotBlank content`)를 `answer/dto`에 별도 파일로
  - `AnswerController`: 검증 실패 시 질문을 다시 조회해 상세 화면을 오류와 함께 렌더링
  - `QuestionController.detail()`에서 빈 `answerForm`을 모델에 담아 상세 폼이 `th:object`를 쓸 수 있게
  - 테스트: 빈 답변은 저장 안 됨 + 상세 화면에 오류 표시
- 이유
  - 빈 답변 저장 방지 (질문 등록과 같은 방식으로 일관성 유지)
  - `WriteForm`(질문)은 한 컨트롤러에서만 써서 내부 record로 뒀지만, 답변 폼은 **질문 상세(빈 폼 표시)와 답변 컨트롤러(처리) 두 곳**에서 쓰므로 별도 파일로 분리
  - 실패 시 리다이렉트하면 오류와 입력값이 사라지므로, 상세 화면을 직접 다시 그림
- 결과
  - `./gradlew test` 24건 통과

### [021] feat(global): 내비게이션 바 추가
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/021)
- 출처: 교재 3-01
- 작업
  - `templates/navbar.html` 프래그먼트(로고 `SBB` → `/`, 로그인 링크 자리), `layout.html`에서 `th:replace`로 포함
  - 목록 테스트에 내비게이션 바 포함 여부 검증 추가
- 이유
  - 모든 페이지에서 홈으로 돌아가고 로그인할 수 있는 공통 메뉴가 필요
  - 레이아웃에 한 번만 넣으면 모든 페이지에 적용됨 ([017]에서 레이아웃을 만든 효과)
  - 좁은 화면에서는 메뉴가 접히고 햄버거 버튼으로 펼침 → [017]에서 레이아웃에 미리 넣어 둔 Bootstrap JS 번들이 필요한 이유
  - 교재는 로고에 `href="/"`, 개선: `th:href="@{/}"` (컨텍스트 경로가 바뀌어도 동작)
- 결과
  - 모든 페이지 상단에 `SBB | 로그인` 메뉴 표시 (스크린샷)
  - `./gradlew test` 24건 통과

### [022] feat(question): 질문 목록 페이징 (한 페이지 10개, 최신순)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/022)
- 출처: 교재 3-02
- 작업
  - `QuestionService.getList(page)`: `PageRequest.of(page, 10, Sort.by(desc("id")))` → `Page<Question>`
  - `GET /questions?page=N` (기본 1), 템플릿에 이전/번호/다음 페이지 링크 (현재 페이지 앞뒤 5개)
  - `BaseInitData.work2()`: 개발용 질문 300개
  - 테스트: 25개 중 1페이지는 최신 10개, 3페이지는 남은 5개
- 이유
  - 질문이 수백 개가 되면 한 번에 다 가져오는 것은 느리고 보기도 어려움 → `LIMIT/OFFSET` 쿼리로 필요한 10개만 조회
  - **(개선) 페이지 번호를 1부터**: 교재는 `?page=0`이 첫 페이지이고 화면에도 `0`이 표시됨. 사용자에게 익숙한 1부터 쓰고, 컨트롤러에서 `page - 1`로 변환 (0 이하는 1로 보정)
  - **(개선) 정렬 기준 `id` 역순**: 교재는 `createDate` 역순. 결과는 같지만 PK는 인덱스가 있어 정렬이 빠름
  - **(개선) `@{/questions(page=${page})}`**: 교재의 `@{|?page=${page}|}` 문자열 이어붙이기 대신 Thymeleaf의 URL 파라미터 문법 → 값이 자동으로 URL 인코딩됨 (나중에 검색어가 붙을 때 유리)
  - **(개선) 300개 샘플 데이터를 테스트가 아닌 dev 초기 데이터로**: 교재는 테스트 코드를 실행해 개발 DB에 300개를 넣음. 테스트가 개발 DB를 바꾸면 안 되므로 dev 프로필 초기 데이터로 생성
- 결과
  - `/questions?page=2` → `[290]`~`[281]` 표시, 페이지 버튼 `2` 활성화 (스크린샷)
  - `./gradlew test` 25건 통과

### [023] feat(question): 게시물 번호를 전체 개수 기준 역순으로 표시
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/023)
- 출처: 교재 3-03
- 작업
  - 번호 = `paging.totalElements - (paging.number * paging.size) - loop.index`
  - 테스트: 25개일 때 1페이지 첫 번호 25, 2페이지 첫 번호 15
- 이유
  - [022]까지는 `loop.count`라 모든 페이지가 1~10으로 반복됨
  - 최신 글이 가장 큰 번호, 가장 오래된 글이 1이 되도록 "전체 개수에서 앞에 지나온 개수만큼 뺀다"
  - DB의 `id`를 번호로 쓰지 않는 이유: 글이 삭제되면 번호가 듬성듬성 비게 됨
- 결과
  - `./gradlew test` 26건 통과

### [024] feat(question): 목록에 답변 개수 표시, N+1 쿼리 문제 해결
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/024)
- 출처: 교재 3-04
- 작업
  - 목록 제목 옆에 답변 개수 배지 (답변이 있을 때만)
  - `application.yaml`: `hibernate.default_batch_fetch_size: 100`
  - `QuestionListQueryCountTest`: Hibernate 통계로 목록 조회 시 실행된 SQL 개수 검증
- 이유
  - **(개선) N+1 문제**: 교재 코드 그대로면 목록의 질문 10개마다 `question.answers`를 꺼낼 때 답변 SELECT가 각각 실행됨 → 질문 1번 + 개수 1번 + 답변 10번 = **12번**
  - `default_batch_fetch_size`를 설정하면 LAZY 컬렉션을 `WHERE question_id IN (?, ?, ...)`으로 한 번에 모아서 조회 → **3번**
  - 설정 한 줄로 모든 LAZY 연관관계에 적용되고, 엔티티·쿼리 코드는 바꿀 필요 없음
  - 테스트로 고정해 두면 나중에 실수로 N+1이 다시 생겼을 때 바로 알 수 있음
- 결과
  - 설정을 지우고 테스트를 돌리면 `12L`로 실패, 설정이 있으면 3 이하로 통과 (설정의 효과를 직접 확인)
  - `./gradlew test` 27건 통과

### [025] chore(security): 스프링 시큐리티 도입 (모든 요청 허용, CSRF 적용)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/025)
- 출처: 교재 3-05, 14168 46강
- 작업
  - `spring-boot-starter-security`, `spring-boot-starter-security-test`
  - `global/security/SecurityConfig`: 정적 파일·H2 콘솔·나머지 모두 허용, H2 콘솔용 `frameOptions(sameOrigin)`와 CSRF 예외
  - 테스트의 POST 요청에 `.with(csrf())`, `@WebMvcTest`에 `@Import(SecurityConfig.class)`
  - CSRF 토큰 없는 POST는 403인지 검증하는 테스트 추가
- 이유
  - 회원가입·로그인 기능의 기반. 시큐리티는 컨트롤러보다 앞단의 필터에서 동작함 (14168 46강)
  - 의존성만 넣으면 모든 페이지가 로그인 필요로 바뀌므로 일단 전부 허용 → 이후 단계에서 필요한 곳만 막음
  - **CSRF**: 다른 사이트가 로그인한 사용자 몰래 우리 서버로 POST를 보내는 공격을 막기 위해, 우리 폼에만 있는 토큰을 함께 보내야 처리됨. `th:action`을 쓴 폼에는 Thymeleaf가 `_csrf` 숨은 필드를 자동으로 넣어 줌
  - **(개선) 정적 파일 `/resource/**` 허용 한 줄**: [015]에서 정적 파일을 `static/resource`에 모아 둔 효과 (14168 46강)
  - **(개선) `@WebMvcTest` + `@Import(SecurityConfig.class)`**: 웹 슬라이스 테스트는 일반 설정 클래스를 읽지 않아 기본 보안(모두 로그인 필요)이 적용되므로 우리 설정을 명시적으로 가져옴
- 결과
  - 목록·상세·H2 콘솔 정상, 상세 페이지 폼에 `_csrf` 숨은 필드 생성 확인
  - `./gradlew test` 28건 통과

### [026] feat(user): 회원 엔티티(SiteUser)와 회원 생성 서비스, BCrypt 비밀번호 암호화
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/026)
- 출처: 교재 3-06
- 작업
  - `domain/user/user`: `SiteUser`(username·email 유니크, password), `UserRepository`(`findByUsername`, `existsByUsername`, `existsByEmail`), `UserService.create()`
  - `SecurityConfig`에 `PasswordEncoder` 빈(`BCryptPasswordEncoder`)
  - `UserServiceTest`: 비밀번호가 해시로 저장되는지, 같은 아이디 중복 가입 시 예외
- 이유
  - 비밀번호를 평문으로 저장하면 DB가 유출됐을 때 그대로 노출됨 → 단방향 해시(BCrypt)로 저장하고 로그인 시 `matches()`로 비교
  - BCrypt는 같은 비밀번호도 매번 다른 해시(salt 포함)를 만들어 미리 계산된 표(레인보우 테이블) 공격에 강함
  - `PasswordEncoder` 인터페이스로 빈을 등록해서, 나중에 암호화 방식을 바꿔도 서비스 코드는 그대로
  - **(개선) `SiteUser`도 `BaseEntity` 상속 + Setter 없이 생성자로만 생성** (교재는 `@Setter` + `new SiteUser()`)
  - **(개선) `existsByUsername/Email`**: 다음 단계에서 가입 전에 중복을 미리 검사해 사용자에게 필드별 오류로 알려주기 위해 (DB 유니크 제약은 최종 안전장치로 유지)
- 결과
  - `./gradlew test` 30건 통과

### [027] feat(user): 회원가입 화면과 검증 (비밀번호 확인, 아이디·이메일 중복)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/027)
- 출처: 교재 3-06, 14168 9~25강
- 작업
  - `UserController`: `GET/POST /users/signup`, `SignupForm` record(`@NotBlank`, `@Size`, `@Email`)
  - 비밀번호 확인 불일치, 아이디·이메일 중복은 `bindingResult.rejectValue()`로 해당 필드 오류 등록
  - `user/user/signup.html`, 내비게이션 바에 "회원가입" 링크
  - `UserControllerTest` 4건: 폼, 가입 성공, 비밀번호 불일치, 중복
- 이유
  - 어노테이션으로 표현할 수 없는 규칙(두 필드 비교, DB 조회가 필요한 중복)은 컨트롤러에서 검사하고 같은 오류 목록에 합쳐 보여줌
  - **(개선) 중복을 저장 전에 검사**: 교재는 저장을 시도한 뒤 `DataIntegrityViolationException`을 잡아 "이미 등록된 사용자입니다" 하나로 처리 → 아이디와 이메일 중 무엇이 중복인지 알 수 없음. 미리 `exists`로 확인해 항목별로 알려줌 (DB 유니크 제약은 동시 가입 같은 경우의 최종 안전장치로 유지)
  - **(개선) URL `/users/signup`**: 다른 자원과 같이 복수형 프리픽스, 폼은 record (14168 40강)
- 결과
  - `./gradlew test` 34건 통과

### [028] feat(user): 로그인·로그아웃 (formLogin, UserDetailsService, POST 로그아웃)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/028)
- 출처: 교재 3-07
- 작업
  - `UserSecurityService implements UserDetailsService`: 아이디로 회원을 찾아 `UserDetails`(권한 `ROLE_USER`/`ROLE_ADMIN`)로 반환
  - `SecurityConfig`: `formLogin(loginPage("/users/login"))`, `logout(logoutUrl("/users/logout"))`
  - `GET /users/login` 화면, 실패 시 `?error` 메시지
  - `thymeleaf-extras-springsecurity6`: 내비게이션 바에서 `sec:authorize`로 로그인 전(로그인·회원가입) / 후(아이디·로그아웃) 메뉴 전환
  - dev 초기 데이터에 회원 `admin`, `user1`, `user2` (비밀번호 `1234`)
  - `UserLoginTest` 5건: 로그인 성공/실패, 로그인 후 메뉴, 로그아웃
- 이유
  - 로그인 처리(비밀번호 비교, 세션 저장)는 시큐리티 필터가 하고, 우리는 "회원 조회"와 "로그인 화면"만 제공하면 됨
  - **(개선) 로그아웃을 POST + CSRF로**: 교재는 `GET /user/logout` 링크도 허용함 → 다른 사이트의 `<img src=".../logout">` 하나로 사용자를 강제 로그아웃시킬 수 있음. 상태를 바꾸는 요청은 POST 폼으로
  - **(개선) 시큐리티 테스트 지원 활용**: `formLogin()`, `logout()`, `@WithMockUser`로 실제 로그인 흐름과 로그인 상태 화면을 테스트
  - **(개선) 개발용 회원을 초기 데이터로**: 매번 회원가입하지 않고 바로 로그인해서 다음 기능(글쓴이, 수정/삭제)을 확인
- 결과
  - 브라우저에서 `user1` 로그인 → 홈(질문 목록)으로 이동, 상단에 `user1 | 로그아웃` 표시 (스크린샷)
  - `./gradlew test` 39건 통과

### [029] feat(question): 질문·답변에 글쓴이 추가, 글쓰기는 로그인 필요
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/029)
- 출처: 교재 3-08
- 작업
  - `Question.author`, `Answer.author` (`@ManyToOne(fetch = LAZY)` → `SiteUser`), 생성자·`addAnswer()`에 author 인자
  - `@EnableMethodSecurity` + 글쓰기 액션에 `@PreAuthorize("isAuthenticated()")`, `Principal`로 현재 사용자 조회
  - `UserService.getUser(username)`, 목록에 글쓴이 칸, 상세에 글쓴이 표시 (`author?.username`)
  - dev 초기 데이터에 글쓴이 지정
  - 테스트: 클래스 레벨 `@WithMockUser("user1")`, 비로그인 시 로그인 페이지로 이동(`@WithAnonymousUser`), 저장된 글쓴이 확인
- 이유
  - 누가 쓴 글인지 알아야 다음 단계에서 "본인 글만 수정/삭제"를 구현할 수 있음
  - 로그인하지 않은 사용자가 글쓰기 URL에 직접 접근하면 `@PreAuthorize`가 막고, 시큐리티가 로그인 페이지로 보냄
  - **(개선) `author`도 LAZY**: 목록에서 글쓴이를 보여줄 때는 [024]의 `default_batch_fetch_size`가 작성자도 IN 절로 묶어서 조회
  - `?.`(안전 탐색 연산자): 글쓴이 기능 이전에 만든 데이터는 `author`가 `null`이라 그냥 `.username`이면 오류
  - 작성자와 무관한 테스트(리포지터리 등)는 `author`에 `null`을 넘김
- 결과
  - `./gradlew test` 42건 통과

### [030] feat(question): 질문 수정 (PUT /questions/{id}, 글쓴이만 가능)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/030)
- 출처: 교재 3-09, 14168 32·41강
- 작업
  - `spring.mvc.hiddenmethod.filter.enabled: true`
  - `GET /questions/{id}/modify` 수정 폼(기존 값 채움), `PUT /questions/{id}` 처리, `ModifyForm` record
  - `Question.isAuthor(user)`, 글쓴이가 아니면 `403 FORBIDDEN`
  - 상세: 글쓴이에게만 "수정" 버튼, 수정된 적 있으면 `modified at` 표시
  - 테스트: 본인 글 수정 성공, 남의 글은 폼·수정 모두 403
- 이유
  - **(개선) `PUT` 메서드 사용 (14168 41강)**: 교재는 `POST /question/modify/{id}`. HTML 폼은 GET/POST만 되므로 `th:method="put"`이 숨은 필드 `_method=put`을 만들고, hiddenmethod 필터가 서버에서 PUT으로 바꿔 줌 → URL은 자원(`/questions/{id}`), 행위는 메서드로
  - **(개선) 권한 없음은 403**: 교재는 `400 BAD_REQUEST`("수정권한이 없습니다"). 400은 "요청 형식이 잘못됨", 403은 "누군지는 알지만 권한이 없음"이라 의미가 맞음
  - **(개선) 글쓴이 확인을 엔티티 메서드(`isAuthor`)로**: 교재는 컨트롤러에서 `getUsername().equals(...)` 문자열 비교. "글쓴이인가?"는 질문이 아는 규칙이므로 엔티티에 두고, 비교는 `BaseEntity.equals`(id 비교)
  - **(개선) 수정일시는 `BaseEntity`의 `@LastModifiedDate`가 자동 기록**: 교재는 `modifyDate` 필드를 추가하고 직접 `LocalDateTime.now()`를 넣음
  - 수정은 `save()` 없이 변경 감지로 UPDATE ([006]에서 확인한 Dirty Checking)
- 결과
  - 브라우저: `user1`로 질문 작성 → 수정 → 상세에 수정된 내용과 `modified at` 표시 (`_method=put` 숨은 필드 확인)
  - `./gradlew test` 44건 통과

### [031] feat(question): 질문 삭제 (DELETE /questions/{id}), global.js로 링크에서 DELETE 전송
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/031)
- 출처: 교재 3-09, 14168 41·43·44강
- 작업
  - `DELETE /questions/{id}`: 글쓴이만 가능(403), 삭제 후 목록으로, cascade로 답변도 삭제
  - `static/resource/global.js`: `<a method="delete" data-confirm="...">` 클릭 시 확인창 → 숨은 폼(POST + `_method=DELETE` + CSRF 토큰) 전송
  - `layout.html`: `<meta name="_csrf">`, `<meta name="_csrf_parameter">`, `global.js` 로드
  - 테스트: 본인 질문 삭제(답변 포함), 남의 질문 403
- 이유
  - **(개선) 삭제를 GET 링크로 하지 않음**: 교재는 `<a href="/question/delete/{id}">`(GET). GET은 브라우저 미리 불러오기, 크롤러, 다른 사이트의 `<img>`로도 실행될 수 있어 데이터를 지우는 요청에 쓰면 위험하고 CSRF 보호도 받지 못함
  - **(개선) `global.js`로 공통화 (14168 43~44강)**: 링크 모양은 유지하면서, `method` 속성만 붙이면 어느 페이지에서든 DELETE/PUT 요청이 되게 함. 교재는 페이지마다 `class="delete"` + 인라인 스크립트로 `location.href` 이동
  - CSRF 토큰은 Thymeleaf가 폼에만 자동으로 넣어 주므로, JS로 만드는 폼을 위해 레이아웃의 `<meta>`에 토큰을 노출
- 결과
  - 브라우저: 상세의 "삭제" 클릭 → 확인 → 목록으로 이동, 해당 질문은 404 (`<meta name="_csrf">` 존재 확인)
  - `./gradlew test` 46건 통과

### [032] feat(answer): 답변 수정·삭제 (PUT·DELETE /questions/{qid}/answers/{id})
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/032)
- 출처: 교재 3-09, 14168 37~39강
- 작업
  - `Answer.isAuthor()`, `Answer.modify()`, `Question.findAnswerById()`, `Question.deleteAnswer()`
  - `AnswerService.modify()`, `AnswerService.delete()` (질문의 답변 목록에서 제거 → orphanRemoval로 DELETE)
  - `GET .../answers/{id}/modify`, `PUT .../answers/{id}`, `DELETE .../answers/{id}`, `question/answer/modify.html`
  - 상세: 답변 글쓴이에게만 수정/삭제 버튼, 답변 수정일시
  - 테스트: 수정, 삭제, 남의 답변 403
- 이유
  - **(개선) 답변을 질문을 통해 찾고 지움 (14168 37강 `findCommentById`, `deleteComment`)**: 질문(애그리거트 루트)이 자기 답변 목록을 관리 → URL의 `questionId`와 실제 답변의 질문이 다르면 찾을 수 없어 404 (다른 질문의 답변을 엉뚱한 URL로 건드릴 수 없음)
  - **(개선) 삭제는 `answerRepository.delete()` 대신 `answers.remove()`**: [005]의 `orphanRemoval = true` 덕분에 목록에서 빼기만 하면 DELETE. 메모리의 `question.answers`와 DB가 항상 같은 상태로 유지됨
  - `answers.remove(answer)`가 올바른 답변을 찾는 근거: [005]에서 보완한 `BaseEntity.equals`(id 비교)
  - 질문과 같은 규칙(PUT/DELETE, 글쓴이만, 403)으로 일관성 유지
  - 테스트에서만 `flush()`가 필요한 이유: 테스트 전체가 하나의 트랜잭션이라 커밋 전까지 DELETE가 실행되지 않음 (실제 요청은 서비스 트랜잭션이 끝날 때 실행)
- 결과
  - `./gradlew test` 49건 통과

### [033] feat(question): 질문·답변 추천 (POST .../votes)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/033)
- 출처: 교재 3-10
- 작업
  - `Question.voters`, `Answer.voters`: `@ManyToMany Set<SiteUser>` + `vote(user)`
  - `POST /questions/{id}/votes`, `POST /questions/{qid}/answers/{id}/votes` (로그인 필요)
  - 상세: 질문·답변마다 "추천 N" 버튼 (`a[method=post]` + 확인창)
  - 테스트: 추천 수 증가, 같은 사람이 두 번 추천해도 1
- 이유
  - 한 회원이 여러 글을, 한 글을 여러 회원이 추천하므로 다대다 → JPA가 중간 테이블(`question_voters`, `answer_voters`)을 만들어 줌
  - `Set`을 써서 중복 추천을 자동으로 막음 (`BaseEntity.equals/hashCode` 기준으로 같은 회원 판별)
  - **(개선) 추천을 GET이 아닌 POST로**: 교재는 `GET /question/vote/{id}` 링크 + JS `location.href`. 데이터를 바꾸는 요청이므로 [031]의 `global.js`를 재사용해 POST + CSRF로 전송 (`method="post"` 속성 하나만 추가)
  - **(개선) URL `/votes`**: "추천 하나를 생성한다"로 보고 자원(votes) + POST
  - 추천 수 표시는 LAZY 컬렉션이지만 [024]의 batch fetch 설정으로 답변들의 추천자도 묶어서 조회
- 결과
  - 브라우저: 2번 질문 추천 → "추천 1" 표시
  - `./gradlew test` 51건 통과

### [034] feat(answer): 답변 작성·수정·추천 후 해당 답변 위치로 이동 (앵커)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/034)
- 출처: 교재 3-11
- 작업
  - 상세의 답변마다 `<a th:id="|answer_${answer.id}|">` 앵커
  - 답변 작성·수정·추천 후 `redirect:/questions/{qid}#answer_{id}` (`redirectToAnswer()`로 공통화)
  - `AnswerService.create()`에서 `flush()`로 INSERT를 즉시 실행해 id 확보
  - 테스트: 리다이렉트 URL에 `#answer_{id}` 포함
- 이유
  - 답변이 많은 질문에서 작업 후 페이지 맨 위로 돌아가면 방금 작업한 답변을 다시 찾아야 함 → URL의 `#id`로 브라우저가 해당 위치로 스크롤
  - **cascade로 저장하면 id는 언제 생길까?**: INSERT는 flush(보통 트랜잭션 커밋) 때 실행되고, IDENTITY 전략은 INSERT 후에야 id를 알 수 있음. 앵커에 id가 바로 필요하므로 서비스에서 `flush()`. 테스트처럼 더 큰 트랜잭션 안에서 호출돼도 동작함 (처음엔 `#answer_null`로 테스트가 실패해서 발견)
  - **(개선) 리다이렉트 URL 생성을 한 메서드로**: 교재는 컨트롤러 메서드마다 `String.format("redirect:/question/detail/%s#answer_%s", ...)`를 반복
- 결과
  - `./gradlew test` 51건 통과

### [035] feat(global): 질문·답변 본문 마크다운 적용 (XSS 방지)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/035)
- 출처: 교재 3-12
- 작업
  - `org.commonmark:commonmark:0.31.0` 의존성
  - `global/markdown/MarkdownService.render()`: `HtmlRenderer`에 `escapeHtml(true)`, `sanitizeUrls(true)`
  - 상세 화면: `th:with="contentHtml=${@markdownService.render(...)}" th:utext="${contentHtml}"`
  - 테스트: `MarkdownServiceTest` 3건(변환, `<script>` 이스케이프, `javascript:` 링크 무력화) + 상세 화면 통합 테스트
- 이유
  - 질문/답변에 코드 블록, 목록, 강조 등을 쓸 수 있게
  - **(개선) XSS 방지**: 교재의 `CommonUtil`은 기본 설정이라 본문에 `<script>`를 쓰면 `th:utext`로 그대로 출력되어, 그 글을 보는 모든 사람의 브라우저에서 스크립트가 실행됨(저장형 XSS). 사용자가 직접 쓴 HTML은 글자로 바꾸고 위험한 URL은 제거
  - **(개선) `Parser`/`HtmlRenderer`를 필드로 한 번만 생성**: 교재는 호출할 때마다 새로 만듦. 상태가 없는(thread-safe) 객체라 재사용 가능
  - 이름을 `CommonUtil` 대신 `MarkdownService`로: 무엇을 하는지 이름으로 드러남
  - Thymeleaf 3.1은 `th:utext` 안의 표현식을 제한된 모드로 평가해서 빈 호출(`@markdownService`)이 막힘 → `th:with`로 먼저 계산 (테스트로 발견)
  - 0.32.0은 공개된 지 2주 남짓이라 검증된 0.31.0 사용
- 결과
  - `./gradlew test` 55건 통과

### [036] feat(question): 질문 검색 (제목·내용·글쓴이·답변)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/036)
- 출처: 교재 3-13
- 작업
  - `QuestionRepository.search(kw, pageable)`: JPQL `@Query` + `left join`(글쓴이, 답변, 답변 글쓴이) + `distinct`
  - `QuestionService.getList(page, kw)`: 검색어가 비어 있으면 조인 없는 `findAll`
  - `GET /questions?kw=...&page=N`, 목록 상단에 검색창, 페이지 링크에 검색어 유지
  - 테스트: 글쓴이·답변 내용·제목 검색, 답변이 여러 개 걸려도 질문은 한 번, 페이지 링크에 `kw` 포함
- 이유
  - **JPQL**: 테이블이 아니라 엔티티와 필드 이름(`q.author`, `q.answers`)으로 조인 → 연관관계를 따라가며 쿼리를 쓸 수 있음
  - `left join`이어야 답변이나 글쓴이가 없는 질문도 검색됨, `distinct`가 없으면 답변 수만큼 같은 질문이 반복됨 (테스트로 고정)
  - **(개선) 검색 폼을 그냥 GET 폼으로**: 교재는 숨은 `searchForm` + JS로 `page`, `kw`를 채워 전송함. Thymeleaf의 `@{/questions(page=..., kw=${kw})}`로 링크에 검색어를 붙이면 JS가 필요 없고, 검색 결과 URL을 공유·북마크할 수 있음
  - **(개선) 교재는 Specification → `@Query` 두 가지를 소개**: 여기서는 읽기 쉬운 `@Query` 하나만 사용, 검색어가 없을 때는 불필요한 조인을 하지 않도록 분기
- 결과
  - 브라우저: `[29`로 검색 → `[290]`~`[299]` 10건, 검색창에 검색어 유지 (스크린샷)
  - `./gradlew test` 57건 통과

### [037] docs(video): 교재 대비 개선점 소개 영상 (HyperFrames)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/037)
- 출처: [001]~[036] 작업내역
- 작업
  - `video/`: HyperFrames(HTML → MP4) 프로젝트, `index.html` 한 파일에 9개 장면 (67초, 1920×1080, 30fps)
  - 장면: 소개 → URL 설계 → 엔티티 설계 → N+1(12→3) → 보안 → 테스트 57건 → 폼·사용성 → 패키지 구조 → 마무리
  - Pretendard 폰트를 로컬 파일(`assets/fonts`, SIL OFL)로 포함, CLI 버전 고정(`hyperframes@0.8.222`)
  - 렌더 결과물(`renders/`)과 캐시(`.hyperframes/`)는 git에서 제외
- 이유
  - 교재와 비교해 무엇을, 왜 바꿨는지를 짧게 정리해 유튜브로 공유하기 위해
  - 영상도 코드(HTML)라서 내용이 바뀌면 문구만 고쳐 다시 렌더링 가능
  - 렌더러는 CDN 폰트를 보장하지 못해서(검사 단계 오류) 폰트를 파일로 포함
  - 첫 프레임은 유튜브 썸네일로 쓰일 수 있어 제목이 0초부터 보이게 함
- 결과
  - `npm run check` 통과, `npm run render` → `renders/jump-to-spring-improvements.mp4` (H.264, 67초, 2.7MB)
  - 다시 만들기: `cd video && npm run render`

### [038] chore(video): 설명 영상용 구조 개편 (장별 서브 컴포지션, edge-tts 내레이션·자막 빌드)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/038)
- 출처: [037] 개선점 영상
- 작업
  - `video/compositions/00-intro.html` ~ `09-outro.html`: 장 10개를 서브 컴포지션으로 분리 (`data-composition-src`)
  - `video/assets/theme.css`: 색·폰트·단계 배지(교재 방식/문제점/개선/결과)·코드 블록·터미널·자막 스타일 공통화
  - `video/scripts/build.py`: `narration/*.txt` → edge-tts(`ko-KR-InJoonNeural`)로 줄마다 음성 생성 → 장 오디오 + 줄별 시간(cue) → 장 HTML에 시간 주입 → 루트 `index.html`과 유튜브 챕터 시간표(`build/chapters.txt`) 생성
  - `requirements.txt`(edge-tts 7.3.1 고정), `npm run setup`, `npm run build`, 생성물(`audio/*.mp3`, `build/`, `.venv/`)은 git 제외
- 이유
  - 12분 영상을 한 파일에 쓰면 고치기 어려우므로 장 단위로 나눔 (HyperFrames 권장 구조)
  - HyperFrames 내장 TTS(Kokoro)는 한국어가 없어서 edge-tts 사용
  - **줄 단위로 음성을 만드는 이유**: 줄 하나가 곧 자막 한 줄이 되고, 줄마다 정확한 시작 시간을 알 수 있어 장면 애니메이션을 "몇 번째 문장이 나올 때"에 맞출 수 있음 (whisper 전사 없이도 자막 타이밍이 정확)
  - 장 길이는 실제 음성 길이로 자동 결정 → 원고를 고치면 다시 빌드만 하면 됨
  - 같은 문장은 캐시해서 다시 만들지 않음
  - 장마다 스크립트를 `(() => { ... })()`로 감싼 이유: 여러 장이 한 페이지로 합쳐질 때 `const CUES` 같은 변수 이름이 충돌하지 않게
- 결과
  - 임시 원고 2줄로 시험: 음성 16.8초, 분당 약 321자, 자막이 문장별로 바뀌는 것을 스냅샷으로 확인 (임시 원고는 삭제)
  - `hyperframes check` 통과 (빈 장 10개, 40초)

### [039] docs(video): 설명 영상 대본 작성 (10장, 약 11분)
- [링크](https://github.com/codecodecode333/jump-to-spring/commit/039)
- 출처: README 작업내역 [001]~[037], 교재 저장소(pahkey/sbb3) 코드 대조
- 작업
  - `video/narration/00-intro.txt` ~ `09-outro.txt`: 한 줄 = 자막 한 줄 = TTS 한 덩어리, 자막 기준 약 3,300자
  - 각 장은 교재 방식 → 문제점 → 개선 코드 → 결과 증명 순서
  - `build.py`: `자막 || 읽을 말` 문법 추가 (자막엔 `BaseEntity`, TTS엔 "베이스 엔티티")
- 이유
  - 대본을 먼저 확정해야 음성 길이가 정해지고, 장면 타이밍을 문장에 맞출 수 있음
  - 교재 쪽 주장(GET 삭제·추천·로그아웃, 400 응답, 마크다운 기본 설정, 중복 가입 단일 메시지, LAZY 없음, Setter)은 교재 코드에서 직접 확인한 것만 사용
  - 우리 쪽 수치(커밋 36개, 테스트 57건과 클래스별 개수, 쿼리 12→3)는 실제 실행 결과 사용
  - TTS는 영어 코드 용어를 어색하게 읽으므로 읽을 말을 따로 지정
  - 첫 초안은 예상 15.7분이라 목표(약 12분)에 맞춰 줄이고, 자막 한 줄을 약 45자 안팎으로 나눔
- 결과
  - 예상 내레이션 약 11.0분 (분당 약 321자 기준, 장면 여유 포함 약 12분 예상)
