package com.mysite.sbb;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// JpaRepository<엔티티, ID 타입> 을 상속하면 save, findById, findAll, delete, count 등이 자동으로 구현된다.
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 메서드 이름 규칙(findBy + 필드명 + 조건)만 지키면 스프링 데이터 JPA 가 쿼리를 만들어 준다.
    // 결과가 없을 수 있으므로 null 대신 Optional 로 받는다.
    Optional<Question> findBySubject(String subject); // WHERE subject = ?

    Optional<Question> findBySubjectAndContent(String subject, String content); // WHERE subject = ? AND content = ?

    List<Question> findBySubjectLike(String subject); // WHERE subject LIKE ?
}
