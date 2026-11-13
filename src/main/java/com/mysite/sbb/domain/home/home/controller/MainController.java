package com.mysite.sbb.domain.home.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {
    // 루트 URL 로 접속하면 질문 목록으로 보낸다.
    // "redirect:" 접두사 → 브라우저에게 302 응답 + Location 헤더를 보내서 해당 주소로 다시 요청하게 한다.
    @GetMapping("/")
    public String index() {
        return "redirect:/questions";
    }

    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "안녕하세요 ^ ^";
    }
}
