package com.mysite.sbb.domain.user.user.controller;

import com.mysite.sbb.domain.user.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserLoginTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService.create("user1", "user1@test.com", "1234");
    }

    @Test
    @DisplayName("GET /users/login: 로그인 폼")
    void showLogin() {
        assertThat(mvc.get().uri("/users/login"))
                .hasStatusOk()
                .hasViewName("user/user/login");
    }

    @Test
    @DisplayName("로그인 성공: 홈으로 리다이렉트되고 인증된 상태가 된다")
    void loginSuccess() {
        assertThat(mvc.perform(formLogin("/users/login").user("user1").password("1234")))
                .hasRedirectedUrl("/");
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호가 틀리면 /users/login?error 로 돌아간다")
    void loginFail() {
        assertThat(mvc.perform(formLogin("/users/login").user("user1").password("wrong")))
                .hasRedirectedUrl("/users/login?error");
    }

    @Test
    @WithMockUser(username = "user1") // 로그인된 사용자로 요청
    @DisplayName("로그인 상태에서는 내비게이션 바에 아이디와 로그아웃이 보인다")
    void navbarWhenLoggedIn() {
        assertThat(mvc.get().uri("/questions"))
                .bodyText()
                .contains("user1", "로그아웃")
                .doesNotContain("회원가입");
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("로그아웃: POST /users/logout 후 홈으로")
    void logoutTest() {
        assertThat(mvc.perform(logout("/users/logout")))
                .hasRedirectedUrl("/");
    }
}
