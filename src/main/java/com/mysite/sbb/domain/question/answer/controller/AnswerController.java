package com.mysite.sbb.domain.question.answer.controller;

import com.mysite.sbb.domain.question.answer.dto.AnswerWriteForm;
import com.mysite.sbb.domain.question.answer.entity.Answer;
import com.mysite.sbb.domain.question.answer.service.AnswerService;
import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.service.QuestionService;
import com.mysite.sbb.domain.user.user.service.UserService;
import com.mysite.sbb.global.exception.DataNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

// 답변은 질문에 딸린 자원이므로 URL 도 질문 아래에 둔다.
// 교재: /answer/create/{id}, /answer/modify/{id}, /answer/delete/{id}
// 개선: POST /questions/{questionId}/answers, PUT·DELETE /questions/{questionId}/answers/{id}
@Controller
@RequestMapping("/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController {
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping // POST = 생성. URL 에 create 를 쓰지 않아도 메서드로 의미가 드러난다.
    public String create(
            @PathVariable Long questionId,
            @ModelAttribute("answerForm") @Valid AnswerWriteForm form,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            // 검증 실패 → 질문 상세 화면을 오류와 함께 다시 그린다
            model.addAttribute("question", questionService.getQuestion(questionId));
            return "question/question/detail";
        }

        answerService.create(questionId, form.content(), userService.getUser(principal.getName()));
        // 생성 후에는 화면을 바로 그리지 않고 상세 페이지로 리다이렉트 (PRG 패턴: 새로고침 시 중복 등록 방지)
        return "redirect:/questions/" + questionId;
    }

    // 질문을 통해 답변을 찾고, 글쓴이가 아니면 403
    // (URL 의 questionId 와 실제 답변의 질문이 다르면 찾지 못하므로 404)
    private Answer getAnswerForAuthor(Long questionId, Long id, Principal principal) {
        Question question = questionService.getQuestion(questionId);
        Answer answer = question.findAnswerById(id)
                .orElseThrow(() -> new DataNotFoundException("answer not found: " + id));
        if (!answer.isAuthor(userService.getUser(principal.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return answer;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/modify")
    public String showModify(
            @PathVariable Long questionId,
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        Answer answer = getAnswerForAuthor(questionId, id, principal);
        model.addAttribute("answer", answer);
        model.addAttribute("form", new AnswerWriteForm(answer.getContent()));
        return "question/answer/modify";
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public String modify(
            @PathVariable Long questionId,
            @PathVariable Long id,
            @ModelAttribute("form") @Valid AnswerWriteForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        Answer answer = getAnswerForAuthor(questionId, id, principal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("answer", answer);
            return "question/answer/modify";
        }

        answerService.modify(answer, form.content());
        return "redirect:/questions/" + questionId;
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long questionId,
            @PathVariable Long id,
            Principal principal
    ) {
        Answer answer = getAnswerForAuthor(questionId, id, principal);
        answerService.delete(answer);
        return "redirect:/questions/" + questionId;
    }
}
