package com.mysite.sbb;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest: JPA 관련 빈(엔티티, 리포지터리)만 띄우는 가벼운 테스트. 각 테스트는 끝나면 롤백된다.
// JpaConfig(@EnableJpaAuditing) 는 일반 설정 클래스라 자동으로 안 뜨므로 직접 Import 한다.
@ActiveProfiles("test")
@DataJpaTest
@Import(JpaConfig.class)
class QuestionRepositoryTest {
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private EntityManager em;

    private Question q1;
    private Question q2;

    // 테스트가 개발용 샘플 데이터에 의존하지 않도록, 각 테스트가 필요한 데이터를 직접 만든다.
    @BeforeEach
    void setUp() {
        q1 = questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다."));
        q2 = new Question("스프링부트 모델 질문입니다.", "id는 자동으로 생성되나요?");
        q2.addAnswer("네 자동으로 생성됩니다.");
        q2.addAnswer("맞습니다!");
        questionRepository.save(q2);
    }

    // 1차 캐시(영속성 컨텍스트)를 비워서, 이후 조회가 실제 DB SELECT 로 일어나게 한다.
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("save: id와 생성일시가 자동으로 채워진다")
    void save() {
        assertThat(q1.getId()).isNotNull();
        assertThat(q1.getCreateDate()).isNotNull(); // JPA Auditing
        assertThat(q1.getModifyDate()).isNotNull();
    }

    @Test
    @DisplayName("findAll")
    void findAll() {
        List<Question> questions = questionRepository.findAll();

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).getSubject()).isEqualTo("sbb가 무엇인가요?");
    }

    @Test
    @DisplayName("findById")
    void findById() {
        flushAndClear();

        Question question = questionRepository.findById(q1.getId()).orElseThrow();

        assertThat(question.getSubject()).isEqualTo("sbb가 무엇인가요?");
        assertThat(question).isEqualTo(q1); // BaseEntity.equals: 같은 id 면 같은 엔티티
    }

    @Test
    @DisplayName("findBySubject")
    void findBySubject() {
        Question question = questionRepository.findBySubject("sbb가 무엇인가요?").orElseThrow();

        assertThat(question.getId()).isEqualTo(q1.getId());
    }

    @Test
    @DisplayName("findBySubjectAndContent")
    void findBySubjectAndContent() {
        Question question = questionRepository
                .findBySubjectAndContent("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다.")
                .orElseThrow();

        assertThat(question.getId()).isEqualTo(q1.getId());
    }

    @Test
    @DisplayName("findBySubjectLike")
    void findBySubjectLike() {
        List<Question> questions = questionRepository.findBySubjectLike("sbb%");

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getSubject()).isEqualTo("sbb가 무엇인가요?");
    }

    @Test
    @DisplayName("수정: save 를 다시 호출하지 않아도 변경 감지(Dirty Checking)로 UPDATE 된다")
    void modify() {
        Question question = questionRepository.findById(q1.getId()).orElseThrow();

        question.modify("수정된 제목", question.getContent());
        flushAndClear(); // flush 시점에 변경된 필드를 감지해서 UPDATE 실행

        assertThat(questionRepository.findBySubject("수정된 제목")).isPresent();
    }

    @Test
    @DisplayName("삭제: 질문을 지우면 cascade REMOVE 로 답변도 함께 삭제된다")
    void delete() {
        assertThat(questionRepository.count()).isEqualTo(2);
        assertThat(answerRepository.count()).isEqualTo(2);

        questionRepository.delete(q2);
        flushAndClear();

        assertThat(questionRepository.count()).isEqualTo(1);
        assertThat(answerRepository.count()).isZero();
    }

    @Test
    @DisplayName("답변 생성: question.addAnswer + cascade PERSIST 로 answerRepository.save 없이 저장된다")
    void createAnswer() {
        Question question = questionRepository.findById(q1.getId()).orElseThrow();

        Answer answer = question.addAnswer("sbb는 질문답변 게시판입니다.");
        assertThat(answer.getId()).isNull(); // 아직 INSERT 전

        flushAndClear(); // flush 시 cascade PERSIST 로 INSERT

        assertThat(answerRepository.count()).isEqualTo(3);
        Question found = questionRepository.findById(q1.getId()).orElseThrow();
        assertThat(found.getAnswers()).hasSize(1);
    }

    @Test
    @DisplayName("답변 조회: 답변에서 질문을, 질문에서 답변 목록을 찾을 수 있다")
    void findAnswer() {
        flushAndClear();

        Question question = questionRepository.findById(q2.getId()).orElseThrow();
        List<Answer> answers = question.getAnswers(); // 이 시점에는 아직 SELECT 안 함 (LAZY 컬렉션)

        assertThat(answers).hasSize(2); // 실제 사용할 때 answer 테이블 SELECT
        Answer answer = answers.get(0);
        assertThat(answer.getContent()).isEqualTo("네 자동으로 생성됩니다.");
        assertThat(answer.getQuestion().getId()).isEqualTo(q2.getId());
    }

    @Test
    @DisplayName("orphanRemoval: 답변 목록에서 빼면 DB 에서도 삭제된다")
    void orphanRemoval() {
        Question question = questionRepository.findById(q2.getId()).orElseThrow();

        question.getAnswers().remove(0);
        flushAndClear();

        assertThat(answerRepository.count()).isEqualTo(1);
    }
}
