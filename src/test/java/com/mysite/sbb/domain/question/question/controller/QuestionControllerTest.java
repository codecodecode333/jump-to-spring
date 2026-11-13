package com.mysite.sbb.domain.question.question.controller;

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

// 컨트롤러 → 리포지터리 → DB → 템플릿까지 실제로 연결해서 확인하는 통합 테스트
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 각 테스트가 끝나면 롤백
class QuestionControllerTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private QuestionRepository questionRepository;

    private Question q1;

    @BeforeEach
    void setUp() {
        q1 = questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다."));
        questionRepository.save(new Question("스프링부트 모델 질문입니다.", "id는 자동으로 생성되나요?"));
    }

    @Test
    @DisplayName("GET /questions: 질문 목록")
    void list() {
        assertThat(mvc.get().uri("/questions"))
                .hasStatusOk()
                .hasViewName("question/question/list")
                .bodyText()
                .contains("sbb가 무엇인가요?", "스프링부트 모델 질문입니다.");
    }
}
