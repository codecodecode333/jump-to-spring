package com.mysite.sbb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Answer extends BaseEntity {
    @Column(columnDefinition = "TEXT")
    private String content;

    // 여러 답변(Many)이 하나의 질문(One)에 속한다. answer 테이블에 question_id 외래키가 생긴다.
    // LAZY: 답변을 조회할 때 질문은 실제로 사용할 때 조회한다. (기본값 EAGER 는 불필요한 조인을 만든다)
    // optional = false: 질문 없는 답변은 존재할 수 없다. (question_id NOT NULL)
    @ManyToOne(fetch = LAZY, optional = false)
    private Question question;

    public Answer(Question question, String content) {
        this.question = question;
        this.content = content;
    }
}
