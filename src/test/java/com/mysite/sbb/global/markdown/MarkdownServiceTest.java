package com.mysite.sbb.global.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 스프링 없이 순수 자바 객체로 테스트 (가장 빠른 단위 테스트)
class MarkdownServiceTest {
    private final MarkdownService markdownService = new MarkdownService();

    @Test
    @DisplayName("마크다운 문법이 HTML 로 변환된다")
    void render() {
        String html = markdownService.render("**굵게**\n\n- 항목1\n- 항목2");

        assertThat(html).contains("<strong>굵게</strong>", "<li>항목1</li>");
    }

    @Test
    @DisplayName("본문에 직접 쓴 HTML(<script>)은 실행되지 않도록 이스케이프된다 (XSS 방지)")
    void escapeHtml() {
        String html = markdownService.render("<script>alert('xss')</script>");

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("javascript: 링크는 무력화된다")
    void sanitizeUrls() {
        String html = markdownService.render("[클릭](javascript:alert(1))");

        assertThat(html).doesNotContain("javascript:");
    }
}
