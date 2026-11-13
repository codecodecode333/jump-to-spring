package com.mysite.sbb.domain.question.answer.controller;

import com.mysite.sbb.domain.question.answer.repository.AnswerRepository;
import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnswerControllerTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;

    private Question question;

    @BeforeEach
    void setUp() {
        question = questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다."));
    }

    @Test
    @DisplayName("POST /questions/{id}/answers: 답변 등록 후 질문 상세로 리다이렉트")
    void create() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
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
                .param("content", "내용"))
                .hasStatus(404);
    }

    @Test
    @DisplayName("POST /questions/{id}/answers: 내용이 비어 있으면 저장하지 않고 상세 화면에 오류를 보여준다")
    void createInvalid() {
        assertThat(mvc.post().uri("/questions/{id}/answers", question.getId())
                .param("content", "  "))
                .hasStatusOk()
                .hasViewName("question/question/detail")
                .bodyText()
                .contains("답변 내용은 필수항목입니다.", "sbb가 무엇인가요?");

        assertThat(answerRepository.count()).isZero();
    }
}
