package com.sprintlog.sprintlogboot.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class NotificationEmitters {

    private final Map<Long, List<SseEmitter>> byUser = new ConcurrentHashMap<>();
    private final AtomicLong eventIds = new AtomicLong();

    private final Duration timeout;
    private final Clock clock;

    public NotificationEmitters(@Value("${sprintlog.notifications.stream-timeout:30m}") Duration timeout, Clock clock) {
        this.timeout = timeout;
        this.clock = clock;
    }

    public SseEmitter connect(Long userId) {
        return connect(userId, null);
    }

    /** 연결 하나를 연다 — 장부에 올리고, '연결됨' 을 먼저 보낸다. */
    public SseEmitter connect(Long userId, Instant tokenExpiresAt) {
        Duration limit = limitFor(tokenExpiresAt); // 토큰 만료 시간과 SSE 연결 신청 시간 사이의 시간을 구해서 SseEmitter의 수명을 결정한다.
        SseEmitter emitter = new SseEmitter(limit.toMillis());
        byUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        // 시한이 되면 '정상 종료' 로 닫는다. 이 줄이 없으면 스프링이 시한 초과 예외를 던져
        //   우리의 마지막 그물(GlobalExceptionHandler)이 ERROR 로 받는다 — 매 30분마다, 사람마다.
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("message", "알림 연결됨")));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        log.info("[알림 연결] 열림 — 사용자 {} · 열린 연결 {}개 · 시한 {} · 일꾼 {}",
                userId, count(userId), readable(limit), Thread.currentThread().getName());
        return emitter;
    }

    /** 이 사용자의 열린 연결 모두에 이벤트 하나를 보낸다. 보낸 연결 수를 돌려준다. */
    public int send(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = byUser.getOrDefault(userId, List.of());
        if (emitters.isEmpty()) {
            log.info("[알림 연결] 열린 연결 없음 — 사용자 {} · '{}' 은 화면으로 못 보낸다", userId, eventName);
            return 0;
        }
        String id = String.valueOf(eventIds.incrementAndGet());
        int sent = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().id(id).name(eventName).data(data));
                sent++;
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);   // 이미 떠난 연결 — 장부에서 뺀다
            }
        }
        log.info("[알림 연결] 보냄 — 사용자 {} · '{}' · {}개 연결 · 일꾼 {}", userId, eventName, sent, Thread.currentThread().getName());
        return sent;
    }

    @Scheduled(fixedDelayString = "${sprintlog.notifications.heartbeat:15s}")
    public void heartbeat() {
        int before = totalCount();
        if (before == 0) return;

        byUser.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(userId, emitter);
                }
            }
        });
        int after = totalCount();
        if (after < before) {
            log.info("[알림 연결] 하트비트 - 떠난 연결 {}개 정리, 남은 연결 {}개, 일꾼 {}", before-after, after, Thread.currentThread().getName());
        } else {
            log.info("[알림 연결] 하트비트 - 열린 연결 {}개, 일꾼 {}", after, Thread.currentThread().getName());
        }
    }

    // 서버가 내려갈 때 - 열려있는 연결을 우리가 먼저 닫는다.
    @EventListener(ContextClosedEvent.class)
    public void closeAll() {
        int total = totalCount();
        if (total == 0) return;
        log.info("[알림 연결] 서버 종료 - 열린 연결 {}개를 먼저 닫는다", total);
        byUser.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
    }

    // 서버 전체의 열린 연결 수
    public int totalCount() {
        return byUser.values().stream().mapToInt(List::size).sum();
    }

    public int count(Long userId) {
        return byUser.getOrDefault(userId, List.of()).size();
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = byUser.get(userId);
        if (emitters != null && emitters.remove(emitter)) {
            log.info("[알림 연결] 연결 정리 — 사용자 {} · 남은 연결 {}개", userId, emitters.size());
        }
    }

    private Duration limitFor(Instant tokenExpiresAt) {
        if (tokenExpiresAt == null) {
            return timeout;
        }
        Duration untilExpiry = Duration.between(clock.instant(), tokenExpiresAt);
        if (untilExpiry.compareTo(Duration.ofSeconds(1)) < 0) {
            return Duration.ofSeconds(1);   // 곧 만료될 토큰 — 잠깐만 열고 곧바로 다시 인증받게 한다
        }
        return untilExpiry.compareTo(timeout) < 0 ? untilExpiry : timeout;
    }

    private static String readable(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.toSecondsPart();
        return minutes > 0 ? minutes + "분 " + seconds + "초" : seconds + "초";
    }


}


















