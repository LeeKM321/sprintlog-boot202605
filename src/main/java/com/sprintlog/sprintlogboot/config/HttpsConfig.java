package com.sprintlog.sprintlogboot.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("https")
public class HttpsConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> plainHttpConnector(
            @Value("${sprintlog.https.http-port:8080}") int httpPort) {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(httpPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }


}
