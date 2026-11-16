package com.brandshield.auth.service;

import com.brandshield.auth.dto.AuthResponse;
import com.brandshield.auth.dto.LoginRequest;
import com.brandshield.auth.dto.RegisterRequest;
import com.brandshield.auth.dto.UserResponse;

import com.brandshield.security.CustomUserDetails;
import com.brandshield.security.jwt.JwtService;

import com.brandshield.user.entity.Role;
import com.brandshield.user.entity.User;
import com.brandshield.user.repository.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = new User();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        // Never accept the role from the registration request.
        user.setRole(Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails =
                new CustomUserDetails(savedUser);

        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                    )
                );

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new IllegalArgumentException("User not found")
                );

        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(
            String token,
            User user) {

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                "Bearer",
                86400,
                userResponse
        );
    }
}
