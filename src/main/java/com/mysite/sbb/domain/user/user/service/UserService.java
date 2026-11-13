package com.mysite.sbb.domain.user.user.service;

import com.mysite.sbb.domain.user.user.entity.SiteUser;
import com.mysite.sbb.domain.user.user.repository.UserRepository;
import com.mysite.sbb.global.exception.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig 에 등록한 BCryptPasswordEncoder

    @Transactional
    public SiteUser create(String username, String email, String rawPassword) {
        // 비밀번호는 절대 그대로 저장하지 않는다. BCrypt 는 같은 비밀번호도 매번 다른 해시를 만든다(salt).
        String encoded = passwordEncoder.encode(rawPassword);
        return userRepository.save(new SiteUser(username, encoded, email));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public SiteUser getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("user not found: " + username));
    }
}
