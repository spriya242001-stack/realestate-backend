package com.example.realestate_backend.service;

import com.example.realestate_backend.entity.Role;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.exception.ResourceNotFoundException;
import com.example.realestate_backend.exception.DuplicateEmailException;
import com.example.realestate_backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with an encoded password and default ROLE_CUSTOMER role.
     */
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException();
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setId(null);
        user.setRole(Role.ROLE_CUSTOMER);
        return userRepository.save(user);
    }

    /**
     * Finds a user by email address.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Finds a user by ID.
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Retrieves all registered users.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Checks if an email is already registered.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    public User getUserByEmail(String email) {
        return findByEmail(email);
    }
}