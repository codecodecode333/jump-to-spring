package com.mysite.sbb.domain.question.answer.dto;

import jakarta.validation.constraints.NotBlank;

// 답변 작성 폼. 질문 상세(QuestionController)에서 빈 폼을 그리고, AnswerController 에서 처리하므로
// 두 컨트롤러가 함께 쓰는 별도 파일로 둔다.
public record AnswerWriteForm(
        @NotBlank(message = "01-content-답변 내용은 필수항목입니다.")
        String content
) {
}
