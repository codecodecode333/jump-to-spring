package com.mysite.sbb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @EnableJpaAuditing 을 메인 클래스에 붙이면 @WebMvcTest(웹 계층만 띄우는 테스트)에서도
// JPA 설정을 찾다가 실패한다. 그래서 별도 설정 클래스로 분리한다.
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
