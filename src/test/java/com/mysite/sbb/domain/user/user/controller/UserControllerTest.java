package com.mysite.sbb.domain.user.user.controller;

import com.mysite.sbb.domain.user.user.repository.UserRepository;
import com.mysite.sbb.domain.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {
    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @Test
    @DisplayName("GET /users/signup: 회원가입 폼")
    void showSignup() {
        assertThat(mvc.get().uri("/users/signup"))
                .hasStatusOk()
                .hasViewName("user/user/signup");
    }

    @Test
    @DisplayName("POST /users/signup: 가입 성공 후 홈으로 리다이렉트")
    void signup() {
        assertThat(mvc.post().uri("/users/signup").with(csrf())
                .param("username", "user1")
                .param("password1", "1234")
                .param("password2", "1234")
                .param("email", "user1@test.com"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/");

        assertThat(userRepository.findByUsername("user1")).isPresent();
    }

    @Test
    @DisplayName("POST /users/signup: 비밀번호 확인이 다르면 가입되지 않는다")
    void signupPasswordMismatch() {
        assertThat(mvc.post().uri("/users/signup").with(csrf())
                .param("username", "user1")
                .param("password1", "1234")
                .param("password2", "5678")
                .param("email", "user1@test.com"))
                .hasStatusOk()
                .bodyText()
                .contains("2개의 비밀번호가 일치하지 않습니다.");

        assertThat(userRepository.findByUsername("user1")).isEmpty();
    }

    @Test
    @DisplayName("POST /users/signup: 이미 있는 아이디/이메일이면 항목별 오류를 보여준다")
    void signupDuplicated() {
        userService.create("user1", "user1@test.com", "1234");

        assertThat(mvc.post().uri("/users/signup").with(csrf())
                .param("username", "user1")
                .param("password1", "1234")
                .param("password2", "1234")
                .param("email", "user1@test.com"))
                .hasStatusOk()
                .bodyText()
                .contains("이미 사용 중인 사용자ID입니다.", "이미 사용 중인 이메일입니다.");
    }
}
