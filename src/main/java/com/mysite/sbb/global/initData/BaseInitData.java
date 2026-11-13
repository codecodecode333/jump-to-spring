package com.mysite.sbb.global.initData;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발(dev) 환경에서 앱이 시작될 때 샘플 데이터를 넣는다.
 * - @Profile("dev"): 테스트(test 프로필)에서는 실행되지 않는다. 테스트는 자기 데이터를 직접 만든다.
 * - ApplicationRunner: 스프링이 완전히 뜬 직후 한 번 실행된다.
 */
@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    // @Transactional 은 프록시를 통해 호출될 때만 동작한다.
    // this.work1() 처럼 내부에서 직접 부르면 트랜잭션이 안 걸리므로, 프록시인 자기 자신(self)을 주입받아 호출한다.
    // @Lazy: 자기 자신을 주입받는 순환 참조를 피하기 위해 실제 사용 시점에 주입
    @Autowired
    @Lazy
    private BaseInitData self;

    private final QuestionRepository questionRepository;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> self.work1();
    }

    @Transactional
    public void work1() {
        if (questionRepository.count() > 0) return; // 파일 DB 라 재시작 시 중복 생성 방지

        questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다."));

        Question q2 = new Question("스프링부트 모델 질문입니다.", "id는 자동으로 생성되나요?");
        q2.addAnswer("네 자동으로 생성됩니다.");
        q2.addAnswer("맞습니다!");
        questionRepository.save(q2);
    }
}
