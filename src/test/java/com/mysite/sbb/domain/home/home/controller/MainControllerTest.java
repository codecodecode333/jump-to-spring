package com.mysite.sbb.domain.home.home.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.mysite.sbb.global.security.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

// @WebMvcTest: 웹 계층(컨트롤러)만 띄우는 가벼운 테스트. 실제 서버(톰캣)는 뜨지 않는다.
@WebMvcTest(MainController.class)
@Import(SecurityConfig.class) // @WebMvcTest 는 일반 설정 클래스를 읽지 않으므로, 우리 보안 설정을 직접 가져온다
class MainControllerTest {
    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("GET / 는 질문 목록(/questions)으로 리다이렉트한다")
    void index() {
        assertThat(mvc.get().uri("/"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/questions");
    }

    @Test
    @DisplayName("GET /hello 는 200과 함께 한글 인사말을 응답한다")
    void hello() {
        assertThat(mvc.get().uri("/hello"))
                .hasStatusOk()
                .hasBodyTextEqualTo("안녕하세요 ^ ^");
    }
}
