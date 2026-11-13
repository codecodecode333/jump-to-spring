package com.mysite.sbb.domain.question.question.controller;

import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor // final 필드를 받는 생성자를 만들어 준다 → 생성자 주입
public class QuestionController {
    private final QuestionRepository questionRepository;

    // 교재: GET /question/list, 개선: 자원(questions) 중심 URL
    @GetMapping("/questions")
    public String list(Model model) {
        List<Question> questions = questionRepository.findAll();
        model.addAttribute("questions", questions); // 템플릿에서 ${questions} 로 사용
        return "question/question/list"; // templates/question/question/list.html
    }
}
