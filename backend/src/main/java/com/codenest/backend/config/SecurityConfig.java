package com.codenest.backend.config;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.security.AdminAuthorizationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectMapper objectMapper,
      AdminAuthorizationManager adminAuthorizationManager)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/posts/**", "/categories", "/users/**")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .access(adminAuthorizationManager)
                    .requestMatchers(
                        "/auth/me",
                        "/auth/sync",
                        "/creator/**",
                        "/messages/**",
                        "/notifications/**",
                        "/uploads/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeError(
                                response,
                                objectMapper,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                ApiResponse.error(ErrorCode.UNAUTHORIZED, "Unauthorized")))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeError(
                                response,
                                objectMapper,
                                HttpServletResponse.SC_FORBIDDEN,
                                ApiResponse.error(ErrorCode.FORBIDDEN, "Forbidden"))))
        .build();
  }

  private void writeError(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      ApiResponse<Void> body)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
