package com.rehan.jobtracker.service;

import com.rehan.jobtracker.dto.AuthResponse;
import com.rehan.jobtracker.dto.LoginRequest;
import com.rehan.jobtracker.dto.RegisterRequest;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.exception.DuplicateEmailException;
import com.rehan.jobtracker.repository.UserRepository;
import com.rehan.jobtracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user with hashed password and generate an initial JWT token.
     */
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                Role.USER
        );

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    /**
     * Authenticate user credentials and return a signed JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // Performs password hash comparison; throws BadCredentialsException on mismatch
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
