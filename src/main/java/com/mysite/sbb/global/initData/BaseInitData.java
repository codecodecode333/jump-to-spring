package com.mysite.sbb.global.initData;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import com.mysite.sbb.domain.user.user.entity.SiteUser;
import com.mysite.sbb.domain.user.user.repository.UserRepository;
import com.mysite.sbb.domain.user.user.service.UserService;

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
    private final UserRepository userRepository;
    private final UserService userService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work0();
            self.work1();
            self.work2();
        };
    }

    // 개발용 회원 (비밀번호 1234)
    @Transactional
    public void work0() {
        if (userRepository.count() > 0) return;

        userService.create("admin", "admin@test.com", "1234");
        userService.create("user1", "user1@test.com", "1234");
        userService.create("user2", "user2@test.com", "1234");
    }

    @Transactional
    public void work1() {
        if (questionRepository.count() > 0) return; // 파일 DB 라 재시작 시 중복 생성 방지

        SiteUser user1 = userService.getUser("user1");
        SiteUser user2 = userService.getUser("user2");

        questionRepository.save(new Question("sbb가 무엇인가요?", "sbb에 대해서 알고 싶습니다.", user1));

        Question q2 = new Question("스프링부트 모델 질문입니다.", "id는 자동으로 생성되나요?", user2);
        q2.addAnswer("네 자동으로 생성됩니다.", user1);
        q2.addAnswer("맞습니다!", user2);
        questionRepository.save(q2);
    }

    // 페이징 확인용 질문 300개
    @Transactional
    public void work2() {
        if (questionRepository.count() >= 300) return;

        SiteUser user1 = userService.getUser("user1");
        for (int i = 1; i <= 300; i++) {
            questionRepository.save(new Question("테스트 데이터입니다:[%03d]".formatted(i), "내용무", user1));
        }
    }
}
