package com.mysite.sbb.domain.question.question.entity;

import com.mysite.sbb.domain.question.answer.entity.Answer;
import com.mysite.sbb.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED) // JPA 는 기본 생성자가 필요하지만, 외부에서 빈 객체를 만들지는 못하게
public class Question extends BaseEntity {
    @Column(length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    // mappedBy: 외래키(question_id)는 Answer 쪽이 가진다. 이 필드는 DB 칼럼이 생기지 않는다.
    // PERSIST: 질문을 저장하면 새로 추가된 답변도 함께 저장, REMOVE: 질문을 지우면 답변도 함께 삭제
    // orphanRemoval: answers 리스트에서 빠진 답변은 DB 에서도 삭제
    @OneToMany(mappedBy = "question", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private final List<Answer> answers = new ArrayList<>(); // null 이 아니도록 바로 초기화

    public Question(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public void modify(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    // 답변은 항상 질문을 통해서 만든다. (양방향 관계의 양쪽을 한 번에 맞춰 준다)
    public Answer addAnswer(String content) {
        Answer answer = new Answer(this, content);
        answers.add(answer);
        return answer;
    }
}
