package com.mysite.sbb.domain.question.question.controller;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/questions") // 이 컨트롤러의 모든 URL 앞에 /questions 가 붙는다
@RequiredArgsConstructor // final 필드를 받는 생성자를 만들어 준다 → 생성자 주입
public class QuestionController {
    private final QuestionService questionService; // 컨트롤러는 리포지터리 대신 서비스를 사용

    // GET /questions (교재: GET /question/list)
    @GetMapping
    public String list(Model model) {
        List<Question> questions = questionService.getList();
        model.addAttribute("questions", questions); // 템플릿에서 ${questions} 로 사용
        return "question/question/list"; // templates/question/question/list.html
    }

    // GET /questions/{id} (교재: GET /question/detail/{id})
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) { // URL 의 {id} 값을 매개변수로 받는다
        Question question = questionService.getQuestion(id);
        model.addAttribute("question", question);
        return "question/question/detail";
    }
}
