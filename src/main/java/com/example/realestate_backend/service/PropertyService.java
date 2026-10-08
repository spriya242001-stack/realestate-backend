package com.example.realestate_backend.service;

import com.example.realestate_backend.entity.Property;
import com.example.realestate_backend.entity.PropertyType;
import com.example.realestate_backend.entity.Role;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.exception.ResourceNotFoundException;
import com.example.realestate_backend.repository.PropertyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public Property createProperty(Property property, User owner) {
        property.setId(null);
        property.setOwner(owner);
        property.setApproved(false);
        return propertyRepository.save(property);
    }

    public List<Property> searchProperties(String location, Double minPrice, Double maxPrice, PropertyType type, String keyword) {
        return propertyRepository.searchApprovedProperties(location, minPrice, maxPrice, type, keyword);
    }

    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    public List<Property> getPropertiesByOwner(Long ownerId) {
        return propertyRepository.findByOwnerId(ownerId);
    }
    public Property updateProperty(
            Long propertyId,
            Property updatedProperty,
            User currentUser) {

        Property existingProperty = getPropertyById(propertyId);

        boolean isOwner =
                existingProperty.getOwner().getId().equals(currentUser.getId());

        boolean isAdmin =
                currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You can only edit your own properties."
            );
        }

        existingProperty.setTitle(updatedProperty.getTitle());
        existingProperty.setDescription(updatedProperty.getDescription());
        existingProperty.setPrice(updatedProperty.getPrice());
        existingProperty.setType(updatedProperty.getType());
        existingProperty.setLocation(updatedProperty.getLocation());

        // Edited property must be approved again by admin
        existingProperty.setApproved(false);

        return propertyRepository.save(existingProperty);
    }


    public void deleteProperty(
            Long propertyId,
            User currentUser) {

        Property existingProperty = getPropertyById(propertyId);

        boolean isOwner =
                existingProperty.getOwner().getId().equals(currentUser.getId());

        boolean isAdmin =
                currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You can only delete your own properties."
            );
        }

        propertyRepository.delete(existingProperty);
    }}
