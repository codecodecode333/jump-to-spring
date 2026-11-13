package com.mysite.sbb.domain.user.user.entity;

import com.mysite.sbb.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

// "User" 는 일부 DB 에서 예약어라서 테이블 이름 충돌을 피하려고 SiteUser 로 짓는다. (교재와 동일)
@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class SiteUser extends BaseEntity {
    @Column(unique = true, length = 30) // 같은 아이디로 두 번 가입 불가 (DB 유니크 제약)
    private String username;

    private String password; // 평문이 아닌 BCrypt 해시값이 저장된다

    @Column(unique = true)
    private String email;

    public SiteUser(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
