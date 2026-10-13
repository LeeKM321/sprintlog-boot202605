package com.sprintlog.sprintlogboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "spring.graphql.graphiql.enabled", havingValue = "true")
public class GraphiQlSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain graphiQlFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/graphiql")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())   // 화면(HTML)일 뿐 — 데이터는 /graphql 이 준다
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                                + "script-src 'self' 'unsafe-inline' https://esm.sh; "
                                + "style-src 'self' 'unsafe-inline' https://esm.sh; "
                                + "worker-src 'self' blob: https://esm.sh; "
                                + "connect-src 'self' https://esm.sh; "
                                + "img-src 'self' data:; "
                                + "font-src 'self' data: https://esm.sh")));
        return http.build();
    }


}
