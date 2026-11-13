package com.mysite.sbb.domain.user.user.service;

import com.mysite.sbb.domain.user.user.entity.SiteUser;
import com.mysite.sbb.domain.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스프링 시큐리티가 로그인 처리 중에 호출하는 서비스.
 * 우리는 "아이디로 회원을 찾아서 UserDetails 로 돌려주기"만 하면 되고,
 * 비밀번호 비교(PasswordEncoder.matches)와 세션 저장은 시큐리티가 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSecurityService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SiteUser siteUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 권한: admin 아이디는 ROLE_ADMIN, 나머지는 ROLE_USER (교재의 UserRole 과 같은 규칙)
        List<GrantedAuthority> authorities = "admin".equals(username)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return new User(siteUser.getUsername(), siteUser.getPassword(), authorities);
    }
}
