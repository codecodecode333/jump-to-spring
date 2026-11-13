package com.mysite.sbb.domain.question.answer.controller;

import com.mysite.sbb.domain.question.answer.entity.Answer;
import com.mysite.sbb.domain.question.answer.repository.AnswerRepository;
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

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "user1") // 기본적으로 user1 로 로그인한 상태로 요청
class AnswerControllerTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;

    private Question question;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService.create("user1", "user1@test.com", "1234");
        question = questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다.", null));
    }

    @Test
    @DisplayName("POST /questions/{id}/answers: 답변 등록 후 질문 상세로 리다이렉트")
    void create() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
                .with(csrf())
                .param("content", "sbb는 질문답변 게시판입니다."))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/questions/" + question.getId());

        assertThat(answerRepository.count()).isEqualTo(1);
        assertThat(answerRepository.findAll().get(0).getContent()).isEqualTo("sbb는 질문답변 게시판입니다.");
    }

    @Test
    @DisplayName("POST /questions/{id}/answers: 없는 질문이면 404")
    void createNotFound() {
        assertThat(mvc.post().uri("/questions/{id}/answers", 999_999)
                .with(csrf())
                .param("content", "내용"))
                .hasStatus(404);
    }

    @Test
    @DisplayName("POST /questions/{id}/answers: 내용이 비어 있으면 저장하지 않고 상세 화면에 오류를 보여준다")
    void createInvalid() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
                .with(csrf())
                .param("content", "  "))
                .hasStatusOk()
                .hasViewName("question/question/detail")
                .bodyText()
                .contains("답변 내용은 필수항목입니다.", "sbb가 무엇인가요?");

        assertThat(answerRepository.count()).isZero();
    }

    @Test
    @DisplayName("CSRF 토큰 없이 POST 하면 403 (다른 사이트가 사용자 몰래 요청을 보내는 공격 차단)")
    void createWithoutCsrf() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
                .param("content", "내용"))
                .hasStatus(403);

        assertThat(answerRepository.count()).isZero();
    }

    @Test
    @WithAnonymousUser
    @DisplayName("로그인하지 않으면 답변을 등록할 수 없다 (로그인 페이지로)")
    void createAnonymous() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
                .with(csrf())
                .param("content", "내용"))
                .hasStatus3xxRedirection()
                .redirectedUrl().endsWith("/users/login");

        assertThat(answerRepository.count()).isZero();
    }

    private Answer saveAnswer(String username) {
        Answer answer = question.addAnswer("원래 답변", userService.getUser(username));
        questionRepository.flush(); // INSERT 해서 id 를 받는다
        return answer;
    }

    @Test
    @DisplayName("PUT /questions/{qid}/answers/{id}: 글쓴이는 답변을 수정할 수 있다")
    void modify() {
        Answer answer = saveAnswer("user1");

        assertThat(mvc.get().uri("/questions/{qid}/answers/{id}/modify", question.getId(), answer.getId()))
                .hasStatusOk();
        assertThat(mvc.put().uri("/questions/{qid}/answers/{id}", question.getId(), answer.getId())
                .with(csrf())
                .param("content", "수정된 답변"))
                .hasRedirectedUrl("/questions/" + question.getId());

        assertThat(answerRepository.findById(answer.getId()).orElseThrow().getContent()).isEqualTo("수정된 답변");
    }

    @Test
    @DisplayName("DELETE /questions/{qid}/answers/{id}: 글쓴이는 답변을 삭제할 수 있다")
    void deleteAnswer() {
        Answer answer = saveAnswer("user1");

        assertThat(mvc.delete().uri("/questions/{qid}/answers/{id}", question.getId(), answer.getId())
                .with(csrf()))
                .hasRedirectedUrl("/questions/" + question.getId());

        // 테스트는 하나의 트랜잭션 안이라 아직 DELETE 가 실행되지 않았다. flush 로 orphanRemoval 의 DELETE 를 실행시킨다.
        answerRepository.flush();
        assertThat(answerRepository.findById(answer.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 사람의 답변은 수정·삭제할 수 없다 (403)")
    void modifyDeleteForbidden() {
        userService.create("user2", "user2@test.com", "1234");
        Answer others = saveAnswer("user2");

        assertThat(mvc.put().uri("/questions/{qid}/answers/{id}", question.getId(), others.getId())
                .with(csrf())
                .param("content", "몰래 수정"))
                .hasStatus(403);
        assertThat(mvc.delete().uri("/questions/{qid}/answers/{id}", question.getId(), others.getId())
                .with(csrf()))
                .hasStatus(403);

        assertThat(answerRepository.findById(others.getId()).orElseThrow().getContent()).isEqualTo("원래 답변");
    }
}
