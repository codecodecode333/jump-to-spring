package com.mysite.sbb.domain.user.user.service;

import com.mysite.sbb.domain.user.user.entity.SiteUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserServiceTest {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원 생성: 비밀번호는 평문이 아닌 BCrypt 해시로 저장된다")
    void create() {
        SiteUser user = userService.create("user1", "user1@test.com", "1234");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("1234").startsWith("$2a$");
        assertThat(passwordEncoder.matches("1234", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("같은 아이디로는 가입할 수 없다 (DB 유니크 제약)")
    void duplicateUsername() {
        userService.create("user1", "a@test.com", "1234");

        assertThat(userService.existsByUsername("user1")).isTrue();
        assertThatThrownBy(() -> userService.create("user1", "b@test.com", "1234"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
