package com.rehan.jobtracker.service;

import com.rehan.jobtracker.dto.AuthResponse;
import com.rehan.jobtracker.dto.LoginRequest;
import com.rehan.jobtracker.dto.RegisterRequest;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.exception.DuplicateEmailException;
import com.rehan.jobtracker.repository.UserRepository;
import com.rehan.jobtracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("John Doe", "john@example.com", "encodedPassword123", Role.USER);
        sampleUser.setId(10L);
    }

    @Test
    @DisplayName("register should encode password, save user, and return AuthResponse without password")
    void register_Success_ShouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(10L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());

        // Verify password was encoded before saving
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user -> user.getPassword().equals("encodedPassword123")));
    }

    @Test
    @DisplayName("register should always strictly enforce Role.USER for public registration")
    void register_AlwaysEnforcesRoleUser() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(argThat(user -> user.getRole() == Role.USER));
    }

    @Test
    @DisplayName("register should throw DuplicateEmailException when email already exists")
    void register_DuplicateEmail_ShouldThrowDuplicateEmailException() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("john@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login should authenticate credentials, retrieve user, and return AuthResponse")
    void login_Success_ShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("john@example.com", "password123"));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("mock.login.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.login.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("john@example.com", response.getEmail());

        verify(authenticationManager).authenticate(
                argThat(token -> token.getPrincipal().equals("john@example.com") && token.getCredentials().equals("password123"))
        );
    }

    @Test
    @DisplayName("login should propagate BadCredentialsException when authentication fails")
    void login_InvalidCredentials_ShouldThrowBadCredentialsException() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login should throw UsernameNotFoundException when user is not found in database after authentication")
    void login_UserNotFoundInDb_ShouldThrowUsernameNotFoundException() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("missing@example.com", "password123"));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }
}
