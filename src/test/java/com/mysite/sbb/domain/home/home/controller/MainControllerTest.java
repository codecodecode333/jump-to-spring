package com.mysite.sbb.domain.home.home.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

// @WebMvcTest: 웹 계층(컨트롤러)만 띄우는 가벼운 테스트. 실제 서버(톰캣)는 뜨지 않는다.
@WebMvcTest(MainController.class)
class MainControllerTest {
    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("GET / 는 200과 함께 SBB 를 응답한다")
    void index() {
        assertThat(mvc.get().uri("/"))
                .hasStatusOk()
                .hasBodyTextEqualTo("SBB");
    }

    @Test
    @DisplayName("GET /hello 는 200과 함께 한글 인사말을 응답한다")
    void hello() {
        assertThat(mvc.get().uri("/hello"))
                .hasStatusOk()
                .hasBodyTextEqualTo("안녕하세요 ^ ^");
    }
}
