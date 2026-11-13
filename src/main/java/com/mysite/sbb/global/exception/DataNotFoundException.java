package com.mysite.sbb.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 이 예외가 컨트롤러 밖으로 던져지면 스프링이 500 대신 404(NOT_FOUND) 로 응답한다.
// 직접 만든 예외라서 이름만 보고도 "데이터가 없다"는 상황을 알 수 있다.
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "entity not found")
public class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message) {
        super(message);
    }
}
