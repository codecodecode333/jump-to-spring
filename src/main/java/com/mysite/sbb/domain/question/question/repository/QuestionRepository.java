package com.mysite.sbb.domain.question.question.repository;

import com.mysite.sbb.domain.question.question.entity.Question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaRepository<엔티티, ID 타입> 을 상속하면 save, findById, findAll, delete, count 등이 자동으로 구현된다.
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 메서드 이름 규칙(findBy + 필드명 + 조건)만 지키면 스프링 데이터 JPA 가 쿼리를 만들어 준다.
    // 결과가 없을 수 있으므로 null 대신 Optional 로 받는다.
    Optional<Question> findBySubject(String subject); // WHERE subject = ?

    Optional<Question> findBySubjectAndContent(String subject, String content); // WHERE subject = ? AND content = ?

    List<Question> findBySubjectLike(String subject); // WHERE subject LIKE ?

    // 제목, 내용, 질문 글쓴이, 답변 내용, 답변 글쓴이 중 하나라도 검색어를 포함하는 질문 (JPQL: 테이블이 아닌 엔티티 기준 쿼리)
    // left join: 답변이나 글쓴이가 없는 질문도 결과에 포함
    // distinct: 답변이 여러 개 걸리면 같은 질문이 여러 줄이 되므로 중복 제거
    @Query("""
            select distinct q
            from Question q
            left join q.author qa
            left join q.answers a
            left join a.author aa
            where q.subject like concat('%', :kw, '%')
               or q.content like concat('%', :kw, '%')
               or qa.username like concat('%', :kw, '%')
               or a.content like concat('%', :kw, '%')
               or aa.username like concat('%', :kw, '%')
            """)
    Page<Question> search(@Param("kw") String kw, Pageable pageable);
}
