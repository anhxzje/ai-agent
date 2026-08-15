package prj.anhzxje.aiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import prj.anhzxje.aiagent.dto.common.ErrorResponse;
import prj.anhzxje.aiagent.security.CustomUserDetailsService;
import prj.anhzxje.aiagent.security.JwtAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    // Mọi request khác cần xác thực
                    .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Xử lý lỗi 401 và 403 trả về JSON thay vì HTML
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        ErrorResponse errorResponse = ErrorResponse.builder()
                                .status(401)
                                .message("Unauthorized: " + authException.getMessage())
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .build();
                        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        ErrorResponse errorResponse = ErrorResponse.builder()
                                .status(403)
                                .message("Forbidden: " + accessDeniedException.getMessage())
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .build();
                        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    }));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
