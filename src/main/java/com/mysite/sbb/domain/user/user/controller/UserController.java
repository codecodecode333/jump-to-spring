package com.mysite.sbb.domain.user.user.controller;

import com.mysite.sbb.domain.user.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    public record SignupForm(
            @NotBlank(message = "01-username-사용자ID는 필수항목입니다.")
            @Size(min = 3, max = 25, message = "02-username-사용자ID는 3자 이상 25자 이하로 입력해주세요.")
            String username,
            @NotBlank(message = "03-password1-비밀번호는 필수항목입니다.")
            String password1,
            @NotBlank(message = "04-password2-비밀번호 확인은 필수항목입니다.")
            String password2,
            @NotBlank(message = "06-email-이메일은 필수항목입니다.")
            @Email(message = "07-email-이메일 형식이 올바르지 않습니다.")
            String email
    ) {
    }

    @GetMapping("/signup")
    public String showSignup(@ModelAttribute("form") SignupForm form) {
        return "user/user/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @ModelAttribute("form") @Valid SignupForm form,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) return "user/user/signup";

        // 어노테이션만으로는 검사할 수 없는 규칙은 직접 검사해서 특정 필드의 오류로 등록한다.
        if (!form.password1().equals(form.password2())) {
            bindingResult.rejectValue("password2", "passwordIncorrect", "05-password2-2개의 비밀번호가 일치하지 않습니다.");
        }
        // 교재: 저장을 시도하고 DataIntegrityViolationException 을 잡음
        // 개선: 저장 전에 미리 확인해서 "어느 항목이" 중복인지 알려준다
        if (userService.existsByUsername(form.username())) {
            bindingResult.rejectValue("username", "duplicated", "08-username-이미 사용 중인 사용자ID입니다.");
        }
        if (userService.existsByEmail(form.email())) {
            bindingResult.rejectValue("email", "duplicated", "09-email-이미 사용 중인 이메일입니다.");
        }
        if (bindingResult.hasErrors()) return "user/user/signup";

        userService.create(form.username(), form.email(), form.password1());
        return "redirect:/";
    }
}
