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
