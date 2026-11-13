package com.mysite.sbb.global.markdown;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

/**
 * 마크다운 텍스트를 HTML 로 변환한다. 템플릿에서 ${@markdownService.render(...)} 로 호출.
 *
 * 교재(CommonUtil)는 기본 설정이라 본문에 쓴 <script> 같은 HTML 이 그대로 출력된다 → XSS 공격 가능.
 * 개선: escapeHtml(true) 로 사용자가 직접 쓴 HTML 태그는 글자로 바꾸고,
 *       sanitizeUrls(true) 로 javascript: 같은 위험한 링크 주소를 무력화한다.
 */
@Service
public class MarkdownService {
    // Parser 와 HtmlRenderer 는 상태가 없어(thread-safe) 한 번 만들어 재사용한다.
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String render(String markdown) {
        if (markdown == null) return "";
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
