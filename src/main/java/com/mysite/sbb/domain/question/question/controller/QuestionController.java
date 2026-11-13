package com.mysite.sbb.domain.question.question.controller;

import com.mysite.sbb.domain.question.answer.dto.AnswerWriteForm;
import com.mysite.sbb.domain.question.question.entity.Question;
import com.mysite.sbb.domain.question.question.service.QuestionService;
import com.mysite.sbb.domain.user.user.entity.SiteUser;
import com.mysite.sbb.domain.user.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/questions") // 이 컨트롤러의 모든 URL 앞에 /questions 가 붙는다
@RequiredArgsConstructor // final 필드를 받는 생성자를 만들어 준다 → 생성자 주입
public class QuestionController {
    private final QuestionService questionService; // 컨트롤러는 리포지터리 대신 서비스를 사용
    private final UserService userService;

    // GET /questions (교재: GET /question/list)
    @GetMapping
    // 교재는 ?page=0 이 첫 페이지. 개선: 사용자에게 보이는 번호와 같게 ?page=1 이 첫 페이지
    public String list(
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        int pageIndex = Math.max(page, 1) - 1; // 0 이하가 들어와도 첫 페이지로
        Page<Question> paging = questionService.getList(pageIndex);
        model.addAttribute("paging", paging); // 템플릿에서 ${paging} 로 사용
        return "question/question/list"; // templates/question/question/list.html
    }

    // GET /questions/{id} (교재: GET /question/detail/{id})
    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id, // URL 의 {id} 값을 매개변수로 받는다
            @ModelAttribute("answerForm") AnswerWriteForm answerForm, // 답변 작성 폼(빈 값)을 템플릿에 전달
            Model model
    ) {
        Question question = questionService.getQuestion(id);
        model.addAttribute("question", question);
        return "question/question/detail";
    }

    // 폼 입력값을 담는 객체 (교재: QuestionForm 클래스 + @Getter @Setter)
    // record: 생성자, getter(subject()), equals/hashCode/toString 을 자동으로 만들어 주는 불변 데이터 클래스
    // 메시지 앞의 "번호-필드명-" 은 오류를 순서대로 정렬하고, 첫 번째 오류 필드에 포커스를 주기 위한 약속 (form_errors.html)
    public record WriteForm(
            @NotBlank(message = "01-subject-제목은 필수항목입니다.")
            @Size(max = 200, message = "02-subject-제목은 200자 이하로 입력해주세요.")
            String subject,
            @NotBlank(message = "03-content-내용은 필수항목입니다.")
            String content
    ) {
    }

    // 같은 URL 이라도 GET 은 "폼 보여주기", POST 는 "폼 처리하기" (14168 26강)
    // @ModelAttribute("form"): 템플릿에서 ${form} 으로 쓸 수 있도록 모델에 담는다. (처음엔 빈 값)
    @PreAuthorize("isAuthenticated()") // 로그인하지 않았으면 로그인 페이지로 보낸다
    @GetMapping("/write")
    public String showWrite(@ModelAttribute("form") WriteForm form) {
        return "question/question/write";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/write")
    public String write(
            @ModelAttribute("form") @Valid WriteForm form, // @Valid: 위 검증 어노테이션을 실행
            BindingResult bindingResult, // 검증 결과. 이게 없으면 검증 실패 시 바로 400 예외가 난다
            Principal principal // 현재 로그인한 사용자 정보 (principal.getName() == username)
    ) {
        // 검증 실패 → 입력값과 오류를 가진 채로 폼을 다시 보여준다
        if (bindingResult.hasErrors()) return "question/question/write";

        SiteUser author = userService.getUser(principal.getName());
        Question question = questionService.create(form.subject(), form.content(), author);
        return "redirect:/questions/" + question.getId(); // 방금 작성한 질문의 상세로 이동
    }

    // 본인 글이 아니면 403 (교재: 400 BAD_REQUEST → 개선: 권한 문제이므로 403 FORBIDDEN)
    private Question getQuestionForAuthor(Long id, Principal principal) {
        Question question = questionService.getQuestion(id);
        if (!question.isAuthor(userService.getUser(principal.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return question;
    }

    public record ModifyForm(
            @NotBlank(message = "01-subject-제목은 필수항목입니다.")
            @Size(max = 200, message = "02-subject-제목은 200자 이하로 입력해주세요.")
            String subject,
            @NotBlank(message = "03-content-내용은 필수항목입니다.")
            String content
    ) {
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/modify")
    public String showModify(@PathVariable Long id, Principal principal, Model model) {
        Question question = getQuestionForAuthor(id, principal);
        model.addAttribute("question", question);
        model.addAttribute("form", new ModifyForm(question.getSubject(), question.getContent())); // 기존 값 채우기
        return "question/question/modify";
    }

    // 교재: POST /question/modify/{id} → 개선: PUT /questions/{id} (수정 = PUT)
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public String modify(
            @PathVariable Long id,
            @ModelAttribute("form") @Valid ModifyForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        Question question = getQuestionForAuthor(id, principal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("question", question);
            return "question/question/modify";
        }

        questionService.modify(question, form.subject(), form.content());
        return "redirect:/questions/" + id;
    }

    // 교재: GET /question/delete/{id} (링크 클릭 = GET 으로 삭제) → 개선: DELETE /questions/{id}
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        Question question = getQuestionForAuthor(id, principal);
        questionService.delete(question);
        return "redirect:/questions";
    }
}
