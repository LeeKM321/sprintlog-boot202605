package com.sprintlog.sprintlogboot.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class LectureApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final WebClient slowApiClient;

    public Mono<Map<String, Object>> fetchLecture(long id, long serverDelayMs) {
        return slowApiClient.get()
                .uri(uriBuilder -> uriBuilder.path("/lectures/{id}").queryParam("ms", serverDelayMs).build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(LectureApiClient::worthRetrying))
                .doOnNext(body -> log.info("[강의 조회] id={} 받아옴 - {}", id, body))
                .onErrorResume(e -> {
                    log.warn("[강의 조회] id={} 실패 — {} : {}", id, e.getClass().getSimpleName(), e.getMessage());
                    Map<String, Object> fallback = new LinkedHashMap<>();
                    fallback.put("id", id);
                    fallback.put("title", "(불러오지 못함)");
                    return Mono.just(fallback);
                });
    }

    @CircuitBreaker(name = "lectureApi", fallbackMethod = "lectureUnavailable")
    public Mono<Map<String, Object>> fetchLectureGuarded(long id, long serverDelayMs, boolean fail) {
        return slowApiClient.get()
                .uri(uriBuilder -> uriBuilder.path("/lectures/{id}")
                        .queryParam("ms", serverDelayMs)
                        .queryParam("fail", fail)
                        .build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT);
    }

    /** 회로 차단기의 폴백 — 원래 메서드와 인자가 같고, 마지막에 예외 하나를 더 받는다. */
    private Mono<Map<String, Object>> lectureUnavailable(long id, long serverDelayMs, boolean fail, Throwable e) {
        log.warn("[회로 차단기] id={} 폴백 — {}", id, e.getClass().getSimpleName());
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("id", id);
        fallback.put("title", "(잠시 후 다시 시도해 주세요)");
        return Mono.just(fallback);
    }



    public Mono<List<Map<String, Object>>> fetchAll(List<Long> ids, long serverDelayMs) {
        return Flux.fromIterable(ids)
                // flatMap은 비동기 작업들을 순차적으로 기다리지 않고 동시에 구독하여 외부 API 요청을 한번에 여러개 쏜다.
                .flatMap(id -> fetchLecture(id, serverDelayMs))
                .collectList();
    }


    // 다시 재시도를 걸어볼 만한 실패인지를 판단 - 서버쪽 잘못과 시한 초과만
    private static boolean worthRetrying(Throwable e) {
        if (e instanceof WebClientResponseException http) {
            return http.getStatusCode().is5xxServerError();
        }
        return e instanceof TimeoutException;
    }

}
















