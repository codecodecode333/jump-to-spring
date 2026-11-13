package com.mysite.sbb.domain.question.answer.service;

import com.mysite.sbb.domain.question.answer.entity.Answer;
import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.service.QuestionService;
import com.mysite.sbb.domain.user.user.entity.SiteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {
    private final QuestionService questionService;

    // 교재: new Answer() → setter → answerRepository.save()
    // 개선: 질문을 조회하고 question.addAnswer() 만 호출. 트랜잭션이 끝날 때 cascade PERSIST 로 INSERT 된다.
    @Transactional
    public Answer create(Long questionId, String content, SiteUser author) {
        Question question = questionService.getQuestion(questionId); // 같은 트랜잭션 안에서 조회 → 영속 상태
        return question.addAnswer(content, author);
    }

    @Transactional
    public void modify(Answer answer, String content) {
        answer.modify(content);
    }

    @Transactional
    public void delete(Answer answer) {
        answer.getQuestion().deleteAnswer(answer);
    }

    @Transactional
    public void vote(Answer answer, SiteUser user) {
        answer.vote(user);
    }
}
