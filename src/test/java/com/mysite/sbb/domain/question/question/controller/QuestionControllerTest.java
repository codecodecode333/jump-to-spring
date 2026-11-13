package com.mysite.sbb.domain.question.question.controller;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.mysite.sbb.domain.user.user.service.UserService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

// 컨트롤러 → 리포지터리 → DB → 템플릿까지 실제로 연결해서 확인하는 통합 테스트
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "user1") // 기본적으로 user1 로 로그인한 상태로 요청 // 각 테스트가 끝나면 롤백
class QuestionControllerTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private QuestionRepository questionRepository;

    private Question q1;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService.create("user1", "user1@test.com", "1234");
        q1 = questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다.", null));
        questionRepository.save(new Question("스프링부트 모델 질문입니다.", "id는 자동으로 생성되나요?", null));
    }

    @Test
    @DisplayName("GET /questions: 질문 목록")
    void list() {
        assertThat(mvc.get().uri("/questions"))
                .hasStatusOk()
                .hasViewName("question/question/list")
                .bodyText()
                .contains("sbb가 무엇인가요?", "스프링부트 모델 질문입니다.")
                .contains("navbar-brand"); // 레이아웃의 내비게이션 바 포함
    }

    @Test
    @DisplayName("GET /questions/{id}: 질문 상세")
    void detail() {
        assertThat(mvc.get().uri("/questions/{id}", q1.getId()))
                .hasStatusOk()
                .hasViewName("question/question/detail")
                .bodyText()
                .contains("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다.");
    }

    @Test
    @DisplayName("GET /questions/{id}: 없는 질문이면 404")
    void detailNotFound() {
        assertThat(mvc.get().uri("/questions/{id}", 999_999))
                .hasStatus(404);
    }

    @Test
    @DisplayName("GET /questions/write: 질문 등록 폼")
    void showWrite() {
        assertThat(mvc.get().uri("/questions/write"))
                .hasStatusOk()
                .hasViewName("question/question/write");
    }

    @Test
    @DisplayName("POST /questions/write: 질문 등록 후 상세로 리다이렉트")
    void write() {
        assertThat(mvc.post().uri("/questions/write")
                .with(csrf())
                .param("subject", "새 질문 제목")
                .param("content", "새 질문 내용"))
                .hasStatus3xxRedirection();

        Question saved = questionRepository.findBySubject("새 질문 제목").orElseThrow();
        assertThat(saved.getContent()).isEqualTo("새 질문 내용");
    }

    @Test
    @DisplayName("POST /questions/write: 제목/내용이 비어 있으면 저장하지 않고 폼과 오류 메시지를 다시 보여준다")
    void writeInvalid() {
        long before = questionRepository.count();

        assertThat(mvc.post().uri("/questions/write")
                .with(csrf())
                .param("subject", " ")
                .param("content", ""))
                .hasStatusOk()
                .hasViewName("question/question/write")
                .bodyText()
                .contains("제목은 필수항목입니다.", "내용은 필수항목입니다.");

        assertThat(questionRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("POST /questions/write: 검증 실패 시 입력했던 값이 유지된다")
    void writeInvalidKeepsInput() {
        assertThat(mvc.post().uri("/questions/write")
                .with(csrf())
                .param("subject", "제목만 입력")
                .param("content", ""))
                .hasStatusOk()
                .bodyText()
                .contains("value=\"제목만 입력\"");
    }

    @Test
    @DisplayName("GET /questions?page=N: 한 페이지에 10개, 최신 글부터")
    void listPaging() {
        for (int i = 1; i <= 23; i++) questionRepository.save(new Question("페이징 %02d".formatted(i), "내용", null));
        // 총 25개 → 1페이지: 페이징 23 ~ 14, 3페이지: 나머지 5개

        assertThat(mvc.get().uri("/questions"))
                .hasStatusOk()
                .bodyText()
                .contains("페이징 23", "페이징 14")
                .doesNotContain("페이징 13");

        assertThat(mvc.get().uri("/questions?page=3"))
                .hasStatusOk()
                .bodyText()
                .contains("sbb가 무엇인가요?", "페이징 03")
                .doesNotContain("페이징 04");
    }

    @Test
    @DisplayName("GET /questions: 번호는 전체 개수부터 거꾸로 매겨진다")
    void listNumbering() {
        for (int i = 1; i <= 23; i++) questionRepository.save(new Question("페이징 %02d".formatted(i), "내용", null));
        // 총 25개 → 1페이지 첫 번호 25, 2페이지 첫 번호 15

        assertThat(mvc.get().uri("/questions")).bodyText().containsPattern("<td>25</td>\\s*<td>\\s*<a[^>]*>페이징 23");
        assertThat(mvc.get().uri("/questions?page=2")).bodyText().containsPattern("<td>15</td>\\s*<td>\\s*<a[^>]*>페이징 13");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("로그인하지 않으면 질문 등록 폼 대신 로그인 페이지로 보낸다")
    void showWriteAnonymous() {
        assertThat(mvc.get().uri("/questions/write"))
                .hasStatus3xxRedirection()
                .redirectedUrl().endsWith("/users/login");
    }

    @Test
    @DisplayName("질문을 등록하면 로그인한 사용자가 글쓴이로 저장된다")
    void writeSetsAuthor() {
        assertThat(mvc.post().uri("/questions/write")
                .with(csrf())
                .param("subject", "글쓴이 확인")
                .param("content", "내용"))
                .hasStatus3xxRedirection();

        Question saved = questionRepository.findBySubject("글쓴이 확인").orElseThrow();
        assertThat(saved.getAuthor().getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("PUT /questions/{id}: 글쓴이는 질문을 수정할 수 있다")
    void modify() {
        Question mine = questionRepository.save(new Question("원래 제목", "원래 내용", userService.getUser("user1")));

        assertThat(mvc.get().uri("/questions/{id}/modify", mine.getId()))
                .hasStatusOk()
                .bodyText().contains("value=\"원래 제목\"");

        assertThat(mvc.put().uri("/questions/{id}", mine.getId())
                .with(csrf())
                .param("subject", "수정된 제목")
                .param("content", "수정된 내용"))
                .hasRedirectedUrl("/questions/" + mine.getId());

        assertThat(questionRepository.findById(mine.getId()).orElseThrow().getSubject()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("PUT /questions/{id}: 다른 사람의 질문은 수정할 수 없다 (403)")
    void modifyForbidden() {
        userService.create("user2", "user2@test.com", "1234");
        Question others = questionRepository.save(new Question("남의 글", "내용", userService.getUser("user2")));

        assertThat(mvc.get().uri("/questions/{id}/modify", others.getId())).hasStatus(403);
        assertThat(mvc.put().uri("/questions/{id}", others.getId())
                .with(csrf())
                .param("subject", "몰래 수정")
                .param("content", "내용"))
                .hasStatus(403);

        assertThat(questionRepository.findById(others.getId()).orElseThrow().getSubject()).isEqualTo("남의 글");
    }

    @Test
    @DisplayName("DELETE /questions/{id}: 글쓴이는 질문을 삭제할 수 있고, 답변도 함께 삭제된다")
    void deleteQuestion() {
        Question mine = new Question("지울 질문", "내용", userService.getUser("user1"));
        mine.addAnswer("딸린 답변", null);
        questionRepository.save(mine);

        assertThat(mvc.delete().uri("/questions/{id}", mine.getId()).with(csrf()))
                .hasRedirectedUrl("/questions");

        assertThat(questionRepository.findById(mine.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /questions/{id}: 다른 사람의 질문은 삭제할 수 없다 (403)")
    void deleteForbidden() {
        userService.create("user2", "user2@test.com", "1234");
        Question others = questionRepository.save(new Question("남의 글", "내용", userService.getUser("user2")));

        assertThat(mvc.delete().uri("/questions/{id}", others.getId()).with(csrf()))
                .hasStatus(403);
        assertThat(questionRepository.findById(others.getId())).isPresent();
    }

    @Test
    @DisplayName("POST /questions/{id}/votes: 추천하면 추천 수가 늘고, 같은 사람이 다시 추천해도 1번만 센다")
    void vote() {
        assertThat(mvc.post().uri("/questions/{id}/votes", q1.getId()).with(csrf()))
                .hasRedirectedUrl("/questions/" + q1.getId());
        assertThat(mvc.post().uri("/questions/{id}/votes", q1.getId()).with(csrf()))
                .hasRedirectedUrl("/questions/" + q1.getId());

        assertThat(questionRepository.findById(q1.getId()).orElseThrow().getVoters()).hasSize(1);
    }

    @Test
    @DisplayName("상세 화면은 본문 마크다운을 HTML 로 보여주고, 직접 쓴 <script> 는 실행되지 않게 출력한다")
    void detailMarkdown() {
        Question q = questionRepository.save(new Question("마크다운", "**굵게**\n\n<script>alert(1)</script>", null));

        assertThat(mvc.get().uri("/questions/{id}", q.getId()))
                .hasStatusOk()
                .bodyText()
                .contains("<strong>굵게</strong>", "&lt;script&gt;")
                .doesNotContain("<script>alert(1)</script>");
    }

    @Test
    @DisplayName("GET /questions?kw=: 제목·내용·글쓴이·답변 내용으로 검색한다")
    void search() {
        userService.create("searcher", "s@test.com", "1234");
        Question byAuthor = questionRepository.save(new Question("글쓴이로 찾기", "내용", userService.getUser("searcher")));
        Question byAnswer = new Question("답변으로 찾기", "내용", null);
        byAnswer.addAnswer("답변에만 있는 특별한단어", null);
        byAnswer.addAnswer("특별한단어 두 번째", null); // 답변이 2개 걸려도 질문은 1번만 나와야 한다
        questionRepository.save(byAnswer);

        assertThat(mvc.get().uri("/questions?kw=searcher"))
                .bodyText().contains("글쓴이로 찾기").doesNotContain("sbb가 무엇인가요?");

        assertThat(mvc.get().uri("/questions?kw=특별한단어"))
                .bodyText()
                .contains("답변으로 찾기")
                .doesNotContain("글쓴이로 찾기")
                .containsOnlyOnce(">답변으로 찾기<");

        assertThat(mvc.get().uri("/questions?kw=sbb"))
                .bodyText().contains("sbb가 무엇인가요?").doesNotContain("스프링부트 모델 질문입니다.");
    }

    @Test
    @DisplayName("검색 중 페이지 링크에는 검색어가 유지된다")
    void searchPagingKeepsKeyword() {
        for (int i = 1; i <= 12; i++) questionRepository.save(new Question("검색페이징 %02d".formatted(i), "내용", null));

        assertThat(mvc.get().uri("/questions?kw=검색페이징"))
                .bodyText()
                .contains("value=\"검색페이징\"")
                .contains("/questions?page=2&amp;kw=");
    }
}
