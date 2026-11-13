package com.mysite.sbb.global.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * 모든 엔티티가 공통으로 가지는 id, 생성일시, 수정일시를 모아 둔 부모 클래스.
 * - @MappedSuperclass: 이 클래스 자체는 테이블이 되지 않고, 필드만 자식 엔티티의 칼럼으로 물려준다.
 * - @EntityListeners(AuditingEntityListener): 저장/수정 시점에 @CreatedDate, @LastModifiedDate 를 자동으로 채운다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id; // 아직 저장되지 않은 엔티티는 null

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    // 같은 테이블의 같은 id 면 같은 엔티티로 본다. (저장 전 엔티티끼리는 서로 다르다)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // LAZY 로딩 시 프록시 객체가 올 수 있으므로 getClass() 대신 Hibernate.getClass() 로 실제 클래스를 비교
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.getId());
    }

    // id 는 저장 전후로 바뀌므로 해시코드에 쓰지 않는다. (HashSet 에 넣은 뒤 저장해도 찾을 수 있도록)
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
