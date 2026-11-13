package com.mysite.sbb.domain.question.question.service;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import com.mysite.sbb.domain.user.user.entity.SiteUser;
import com.mysite.sbb.global.exception.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public static final int PAGE_SIZE = 10;

    // page: 0부터 시작하는 페이지 번호 (스프링 데이터 규칙)
    public Page<Question> getList(int page) {
        // 최신 글이 먼저 오도록 id 역순 정렬. (PK 라 인덱스가 있어 createDate 정렬보다 유리)
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Order.desc("id")));
        return questionRepository.findAll(pageable); // SELECT ... LIMIT 10 OFFSET ? + 전체 개수 COUNT 쿼리
    }

    public Question getQuestion(Long id) {
        // Optional 이 비어 있으면 예외를 던진다. (isPresent/get 조합보다 짧고 안전)
        return questionRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("question not found: " + id));
    }

    @Transactional // 쓰기 작업이므로 readOnly 가 아닌 트랜잭션
    public Question create(String subject, String content, SiteUser author) {
        return questionRepository.save(new Question(subject, content, author));
    }
}
