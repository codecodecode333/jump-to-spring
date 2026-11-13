package com.mysite.sbb.domain.question.question.controller;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
class QuestionListQueryCountTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("목록에서 답변 개수를 보여줘도 질문 수만큼 답변 SELECT 가 반복되지 않는다 (N+1 방지)")
    void answerCountWithoutNPlusOne() {
        for (int i = 1; i <= 10; i++) {
            Question q = new Question("질문 %02d".formatted(i), "내용");
            q.addAnswer("답변 A");
            q.addAnswer("답변 B");
            questionRepository.save(q);
        }
        em.flush();
        em.clear(); // 캐시를 비워서 목록 조회 시 실제로 SELECT 가 일어나게

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        assertThat(mvc.get().uri("/questions"))
                .hasStatusOk()
                .bodyText()
                .contains("질문 10");

        // 질문 페이지 SELECT 1 + 전체 개수 COUNT 1 + 답변 묶음 SELECT 1 = 3
        // (default_batch_fetch_size 가 없으면 답변 SELECT 가 질문 10개마다 따로 나가서 12번)
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
