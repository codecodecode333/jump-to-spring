package com.mysite.sbb.domain.question.answer.controller;

import com.mysite.sbb.domain.question.answer.dto.AnswerWriteForm;
import com.mysite.sbb.domain.question.answer.service.AnswerService;
import com.mysite.sbb.domain.question.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// 답변은 질문에 딸린 자원이므로 URL 도 질문 아래에 둔다.
// 교재: POST /answer/create/{id}  →  개선: POST /questions/{questionId}/answers
@Controller
@RequestMapping("/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController {
    private final QuestionService questionService;
    private final AnswerService answerService;

    @PostMapping // POST = 생성. URL 에 create 를 쓰지 않아도 메서드로 의미가 드러난다.
    public String create(
            @PathVariable Long questionId,
            @ModelAttribute("answerForm") @Valid AnswerWriteForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            // 검증 실패 → 질문 상세 화면을 오류와 함께 다시 그린다
            model.addAttribute("question", questionService.getQuestion(questionId));
            return "question/question/detail";
        }

        answerService.create(questionId, form.content());
        // 생성 후에는 화면을 바로 그리지 않고 상세 페이지로 리다이렉트 (PRG 패턴: 새로고침 시 중복 등록 방지)
        return "redirect:/questions/" + questionId;
    }
}
