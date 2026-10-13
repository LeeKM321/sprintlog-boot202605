package com.sprintlog.sprintlogboot.notification;

import com.sprintlog.sprintlogboot.domain.User;
import com.sprintlog.sprintlogboot.dto.response.StreamTicketResponse;
import com.sprintlog.sprintlogboot.repository.UserRepository;
import com.sprintlog.sprintlogboot.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Optional;

import static com.sprintlog.sprintlogboot.notification.StreamTicketService.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final StreamTicketService tickets;
    private final UserRepository userRepository;
    private final NotificationEmitters emitters;
    private final JwtProvider jwtProvider;

    @PostMapping("/stream-ticket")
    public ResponseEntity<StreamTicketResponse> issueTicket(Authentication authentication,
                                                            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Optional<User> user = userRepository.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String ticket = tickets.issue(user.get().getId(), tokenExpiresAt(authorization));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new StreamTicketResponse(ticket, TTL.toSeconds()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestParam(required = false) String ticket) {
        Optional<Admission> admission = tickets.admit(ticket);
        if (admission.isEmpty()) {
            log.info("[알림 연결] 입장권 거절 - 없거나, 이미썼거나, 지났다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(emitters.connect(admission.get().userId(), admission.get().tokenExpiresAt()));
    }

    private Instant tokenExpiresAt(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return jwtProvider.parseClaims(authorization.substring(7)).getExpiration().toInstant();
    }

}














