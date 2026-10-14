package com.sprintlog.sprintlogboot.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.stream.Collectors;

@ControllerAdvice
public class GraphQlErrorAdvice {

    /** 입력 검증 실패(@NotBlank · @Min …) — REST 의 400 자리. 어긴 규칙의 메시지를 모아 알려 준다. */
    @GraphQlExceptionHandler
    public GraphQLError invalidInput(ConstraintViolationException e, DataFetchingEnvironment env) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(" · "));
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(message)
                .build();
    }

    /** 우리가 정한 업무 예외 — ErrorCode 의 HTTP 상태를 GraphQL 의 분류로 옮긴다. */
    @GraphQlExceptionHandler
    public GraphQLError business(BusinessException e, DataFetchingEnvironment env) {
        ErrorType type = switch (e.getErrorCode().getStatus()) {
            case BAD_REQUEST -> ErrorType.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorType.UNAUTHORIZED;
            case FORBIDDEN -> ErrorType.FORBIDDEN;
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(e.getMessage())
                .build();
    }


}
