package prj.anhzxje.aiagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import prj.anhzxje.aiagent.dto.auth.AuthResponse;
import prj.anhzxje.aiagent.dto.auth.LoginRequest;
import prj.anhzxje.aiagent.dto.auth.RegisterRequest;
import prj.anhzxje.aiagent.entity.User;
import prj.anhzxje.aiagent.enums.Role;
import prj.anhzxje.aiagent.repository.UserRepository;
import prj.anhzxje.aiagent.security.CustomUserDetails;
import prj.anhzxje.aiagent.security.JwtTokenProvider;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Đăng ký tài khoản mới: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Username đã tồn tại")
                            .build());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Email đã được sử dụng")
                            .build());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        log.info("Đăng ký thành công cho user: {}", user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .message("Đăng ký thành công")
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Đăng nhập: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        log.info("Đăng nhập thành công cho user: {}", user.getUsername());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .message("Đăng nhập thành công")
                        .build());
    }
}
