package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts the REST version of the prototype. Spring lives only in this package: the same
 * {@link IwfcFacade} that the console uses is exposed as a bean, so nothing inside it knows about Spring.
 */
@SpringBootApplication
@EnableScheduling
public class IwfcApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IwfcApiApplication.class, args);
    }

    /**
     * Storage settings are read from Spring's Environment, so they can come from real environment variables
     * or, in tests, from properties that override them.
     */
    @Bean
    IwfcFacade iwfcFacade(Environment environment) {
        Map<String, String> settings = new HashMap<>();
        for (String key : List.of("FITPULSE_STORAGE", "FITPULSE_DB_URL", "FITPULSE_DB_USER", "FITPULSE_DB_PASSWORD",
                "FITPULSE_DB_NO_PASSWORD")) {
            String value = environment.getProperty(key);
            if (value != null) {
                settings.put(key, value);
            }
        }
        return IwfcBootstrap.configured(settings);
    }

    /** Lets the React dev server (Vite on 5173, or 3000) call the API from the browser. */
    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*");
            }
        };
    }
}
