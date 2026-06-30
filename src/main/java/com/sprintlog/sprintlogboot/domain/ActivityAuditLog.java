package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 protected로 선언 (JPA가 사용하는 생성자)
public class ActivityAuditLog extends BaseEntity {

    // 어떤 작업이었는지 - "CREATE", "UPDATE", "DELETE"
    @Column(nullable = false, length = 20)
    private String action;

    // 상세 - 예: "활동 생성: Spring Bean Scope"
    @Column(nullable = false, length = 300)
    private String detail;

    public ActivityAuditLog(String action, String detail) {
        this.action = action;
        this.detail = detail;
    }
}








