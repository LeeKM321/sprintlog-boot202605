package com.sprintlog.sprintlogboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("sprintlog.jwt")
public class JwtProperties {

    private String secret;

    private Duration accessTokenValidity = Duration.ofMinutes(30);

    private String issuer = "sprintlog";

}
