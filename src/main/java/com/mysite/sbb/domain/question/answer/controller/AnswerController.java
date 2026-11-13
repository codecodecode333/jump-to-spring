package com.mysite.sbb.domain.question.answer.controller;

import com.mysite.sbb.domain.question.answer.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 답변은 질문에 딸린 자원이므로 URL 도 질문 아래에 둔다.
// 교재: POST /answer/create/{id}  →  개선: POST /questions/{questionId}/answers
@Controller
@RequestMapping("/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController {
    private final AnswerService answerService;

    @PostMapping // POST = 생성. URL 에 create 를 쓰지 않아도 메서드로 의미가 드러난다.
    public String create(
            @PathVariable Long questionId,
            @RequestParam String content // 폼의 <textarea name="content"> 값
    ) {
        answerService.create(questionId, content);
        // 생성 후에는 화면을 바로 그리지 않고 상세 페이지로 리다이렉트 (PRG 패턴: 새로고침 시 중복 등록 방지)
        return "redirect:/questions/" + questionId;
    }
}
