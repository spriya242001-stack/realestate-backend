package com.example.realestate_backend.service;

import com.example.realestate_backend.entity.Role;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.exception.ResourceNotFoundException;
import com.example.realestate_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("S Priya")
                .email("priya@example.com")
                .password("rawPassword123")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByEmail(sampleUser.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(sampleUser);

        assertNotNull(registered);
        assertEquals("encodedPassword123", registered.getPassword());
        assertEquals(Role.ROLE_CUSTOMER, registered.getRole());
        assertNull(registered.getId());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void registerUser_ThrowsException_WhenEmailExists() {
        when(userRepository.existsByEmail(sampleUser.getEmail())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(sampleUser)
        );

        assertEquals("An account with this email already exists.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_Success() {
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(sampleUser));

        User found = userService.findByEmail("priya@example.com");

        assertNotNull(found);
        assertEquals("S Priya", found.getName());
        assertEquals("priya@example.com", found.getEmail());
    }

    @Test
    void findByEmail_ThrowsException_WhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByEmail("missing@example.com")
        );
    }

    @Test
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("S Priya", users.getFirst().getName());
    }
}