package com.mysite.sbb.domain.question.question.service;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import com.mysite.sbb.global.exception.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 질문 관련 비즈니스 로직 담당.
 * 컨트롤러는 요청/응답만, 서비스는 "무엇을 할지", 리포지터리는 "DB 접근"만 맡는다.
 * - @Transactional(readOnly = true): 기본은 조회 전용 트랜잭션 (변경 감지 생략 → 조회 성능 이점)
 *   데이터를 바꾸는 메서드에만 @Transactional 을 따로 붙인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {
    private final QuestionRepository questionRepository;

    public List<Question> getList() {
        return questionRepository.findAll();
    }

    public Question getQuestion(Long id) {
        // Optional 이 비어 있으면 예외를 던진다. (isPresent/get 조합보다 짧고 안전)
        return questionRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("question not found: " + id));
    }
}
