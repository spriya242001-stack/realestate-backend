package com.example.realestate_backend.controller;

import com.example.realestate_backend.entity.Property;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.entity.PropertyType;
import java.time.LocalDateTime;
import com.example.realestate_backend.service.PropertyService;
import com.example.realestate_backend.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyRestController {

    private final PropertyService propertyService;
    private final UserService userService;

    public PropertyRestController(
            PropertyService propertyService,
            UserService userService) {

        this.propertyService = propertyService;
        this.userService = userService;
    }

    public record PropertyResponse(Long id, String title, String description, Double price,
                                   PropertyType type, String location, String imageUrl,
                                   boolean approved, LocalDateTime dateListed) {
        static PropertyResponse from(Property property) {
            return new PropertyResponse(property.getId(), property.getTitle(), property.getDescription(),
                    property.getPrice(), property.getType(), property.getLocation(), property.getImageUrl(),
                    property.isApproved(), property.getDateListed());
        }
    }

    @GetMapping("/my-properties")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(
            Authentication authentication) {

        User currentUser =
                userService.findByEmail(authentication.getName());

        List<Property> properties =
                propertyService.getPropertiesByOwner(
                        currentUser.getId()
                );

        return ResponseEntity.ok(properties.stream().map(PropertyResponse::from).toList());
    }
}