package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Starts the REST version of the prototype. Spring lives only in this package: the same
 * {@link IwfcFacade} that the console uses is exposed as a bean, so nothing inside it knows about Spring.
 */
@SpringBootApplication
public class IwfcApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IwfcApiApplication.class, args);
    }

    @Bean
    IwfcFacade iwfcFacade() {
        return IwfcBootstrap.seededSecure();
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
