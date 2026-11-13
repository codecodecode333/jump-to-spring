package com.mysite.sbb.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 스프링 시큐리티 설정.
 * 시큐리티는 컨트롤러보다 앞단의 "필터"에서 모든 요청을 검사한다.
 * 의존성만 추가하면 모든 URL 이 로그인 필요로 바뀌므로, 일단 전부 허용하고 기능을 하나씩 붙여 나간다.
 */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/favicon.ico", "/resource/**").permitAll() // 정적 파일
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().permitAll() // 지금은 모두 허용, 로그인 기능 후 조정
                )
                // H2 콘솔은 <frame> 으로 화면을 구성하므로, 같은 출처(sameOrigin)의 frame 은 허용
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                // H2 콘솔의 폼에는 CSRF 토큰이 없으므로 CSRF 검사에서 제외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )
                // 로그인: 화면(GET)은 우리가 만들고, 처리(POST /users/login)는 시큐리티 필터가 한다
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .defaultSuccessUrl("/")
                )
                // 로그아웃: POST /users/logout (교재는 GET 도 허용하지만, 상태를 바꾸는 요청은 POST + CSRF 로)
                .logout(logout -> logout
                        .logoutUrl("/users/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true) // 세션 삭제
                );

        return http.build();
    }

    // 비밀번호 암호화 방식. 인터페이스(PasswordEncoder)로 등록해 두면 나중에 방식을 바꿔도 사용하는 쪽은 그대로
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
