package com.sprintlog.sprintlogboot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 공통 응답 조립 헬퍼 메서드
    private ProblemDetail problem(HttpStatus status, String code, String detail, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // 통합 핸들러 - 우리 도메인 예외(BusinessException을 상속받은 계열) 전부를 하나로.
    // 더이상 핸들러가 status를 하드코딩하지 않는다.
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
        if (ec.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            log.warn("[{}] {}", ec.getCode(), e.getMessage());
        }
        // 5xx(우리 서버 문제)는 원인을 확인해야 하니 ERROR + 스택트레이스(에러 객체를 마지막 인자로 전달)
        if (ec.getStatus().is5xxServerError()) {
            log.error("[{}] {}", ec.getCode(), e.getMessage(), e);
        } else {
            // 4xx(클라이언트 문제)는 예상된 상황이니 WARN 레벨로 간결하게.
            log.warn("[{}] {}", ec.getCode(), e.getMessage());
        }

        return problem(ec.getStatus(), ec.getCode(), e.getMessage(), ec.getDefaultMessage());
    }

    // 스프링이 던지는 프레임워크 예외이기 때문에 BusinessException 상속은 어렵다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {

        // 1. 오류 결과를 담을 Map을 선언합니다. (key: 필드명, value: 메시지)
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach((error) -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(), "요청 본문의 일부 필드가 유효하지 않습니다.", "입력 검증 실패");
        pd.setProperty("errors", errors);
        return pd;
    }

    // 400 — JSON 자체가 깨졌거나 enum 에 없는 값 등, 요청 본문을 읽지 못함
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(), "요청 본문(JSON)을 읽을 수 없습니다. 형식이나 값을 확인하세요.", "요청 본문 오류");
    }

    // 404 — 매핑된 핸들러가 없는 경로. 프레임워크가 던지는 NoResourceFoundException → C002
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException e) {
        log.warn("없는 경로 요청: {}", e.getResourcePath());
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                "요청하신 경로를 찾을 수 없습니다!", "경로 없음");
    }

    // 400 — 경로 변수·쿼리 파라미터의 타입 불일치(예: /activities/abc). 프레임워크 예외 → C001
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(),
                "요청 값 '" + e.getName() + "' 의 형식이 올바르지 않습니다.", "잘못된 요청 파라미터");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(), e.getMessage(), "잘못된 요청");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED.getCode(), e.getMessage(), "권한 없음");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        log.warn("[AUTH] 로그인 실패 - {}: {}", e.getClass().getSimpleName(), e.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "AUTH_401_BAD_CREDENTIALS",
                "이메일 또는 비밀번호가 올바르지 않습니다.", "인증 실패");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientGone(AsyncRequestNotUsableException e) {
        log.debug("[SSE] 클라이언트 연결 끊김: {}", e.getMessage());
    }


    // 500 — 그 밖의 예상 못 한 오류. 원본 메시지는 로그에만, 클라이언트엔 안전한 문구만
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        // 스프링이 던진 예외 중엔 알맞은 상태 코드를 이미 아는 것들이 있다(ErrorResponse — 대개 405 · 415 · 400, 드물게 5xx).
        //   500 으로 덮으면 클라이언트는 "서버 탓" 으로 읽고 다시 시도한다. 그 상태 코드와 헤더(405 의 Allow 등)를 그대로 돌려준다.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            if (status.is5xxServerError()) {
                log.error("[HTTP {}] {}", status.value(), e.getMessage(), e);   // 5xx(매핑 실수·시한 초과 등)는 우리가 볼 일 — 스택까지
            } else {
                log.warn("[HTTP {}] {}", status.value(), e.getMessage());       // 4xx 는 요청 쪽 잘못 — WARN 한 줄
            }
            return problem(status, "HTTP_" + status.value(),
                    errorResponse.getBody().getDetail(), status.getReasonPhrase());
        }
        log.error("예상치 못한 서버 오류", e);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return problem(ec.getStatus(), ec.getCode(), ec.getDefaultMessage(), "서버 오류");
    }

}







