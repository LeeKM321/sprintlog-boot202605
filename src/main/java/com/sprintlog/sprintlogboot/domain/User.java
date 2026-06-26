package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.*;

@Entity // 이 클래스는 JPA가 관리한다. 이 클래스는 데이터베이스의 한 행(인스턴스)에 정확하게 대응된다.
@Table(name = "users")
public class User extends BaseEntity {

    // @Column 속성으로 컬럼 제약을 표현한다. (null 여부, 길이)
    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

}
