package com.example.realestate_backend.service;

import com.example.realestate_backend.entity.Property;
import com.example.realestate_backend.entity.Role;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyService propertyService;

    private User user;
    private Property property;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_CUSTOMER)
                .build();

        property = Property.builder()
                .title("Sample Villa")
                .price(250000.0)
                .location("New York")
                .build();
    }

    @Test
    void createProperty_Success() {
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Property created = propertyService.createProperty(property, user);

        assertNotNull(created);
        assertEquals("Sample Villa", created.getTitle());
        assertEquals(user, created.getOwner());
        assertFalse(created.isApproved());
        verify(propertyRepository, times(1)).save(property);
    }
}