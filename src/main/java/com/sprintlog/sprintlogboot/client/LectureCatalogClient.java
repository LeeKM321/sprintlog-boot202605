package com.sprintlog.sprintlogboot.client;

import com.sprintlog.sprintlogboot.exception.ErrorCode;
import com.sprintlog.sprintlogboot.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class LectureCatalogClient {

    private final RestClient restClient;

    /** 가짜 강의 플랫폼(slow-api)에 주는 '뜸 들이기' · '일부러 실패' 값 — 실제 강의 플랫폼이라면 없는 설정이다. */
    private final long simulatedDelayMs;
    private final boolean simulatedFailure;

    public LectureCatalogClient(RestClient.Builder builder,
                                @Value("${sprintlog.slow-api.base-url:http://localhost:8081}") String baseUrl,
                                @Value("${sprintlog.slow-api.lecture-delay-ms:0}") long simulatedDelayMs,
                                @Value("${sprintlog.slow-api.lecture-fail:false}") boolean simulatedFailure) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.simulatedDelayMs = simulatedDelayMs;
        this.simulatedFailure = simulatedFailure;
    }

    @CircuitBreaker(name = "lectureCatalog", fallbackMethod = "circuitOpen")
    public LectureInfo find(long id) {
        long start = System.currentTimeMillis();
        try {
            LectureInfo info = restClient.get()
                    .uri("/lectures/{id}?ms={ms}&fail={fail}", id, simulatedDelayMs, simulatedFailure)
                    .retrieve()
                    .body(LectureInfo.class);
            log.info("[강의 플랫폼] {}회차 받음 — {}ms", id, System.currentTimeMillis() - start);
            return info;
        } catch (RestClientException e) {
            // 꺼졌든(연결 거부) · 느리든(시한 초과) · 고장이든(5xx) — 기다리게 하지 않고 '지금은 못 한다(503)' 로 바로.
            log.warn("[강의 플랫폼] {}회차 실패 — {} · {}ms", id, e.getClass().getSimpleName(), System.currentTimeMillis() - start);
            throw new ExternalServiceException(ErrorCode.LECTURE_PLATFORM_UNAVAILABLE,
                    "강의 플랫폼이 응답하지 않습니다 — " + e.getClass().getSimpleName(), e);
        }
    }

    /**
     * 회로가 열려 있을 때 — 플랫폼을 부르지도 않고 여기로 온다(0초).
     * 매개변수 타입이 CallNotPermittedException 이라, 회로가 '못 부르게 막은' 경우에만 불린다
     * (플랫폼이 실패한 경우는 find 가 던진 503 이 그대로 나간다).
     */
    private LectureInfo circuitOpen(long id, CallNotPermittedException e) {
        throw new ExternalServiceException(ErrorCode.LECTURE_PLATFORM_UNAVAILABLE,
                "강의 플랫폼이 응답하지 않습니다 — 실패가 이어져 잠시 부르지 않는 중(회로 열림)", e);
    }
}
