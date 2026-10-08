package com.example.realestate_backend.service;
import com.example.realestate_backend.entity.Property;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.exception.ResourceNotFoundException;
import com.example.realestate_backend.repository.PropertyRepository;
import com.example.realestate_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public AdminService(UserRepository userRepository, PropertyRepository propertyRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        propertyRepository.deleteAll(propertyRepository.findByOwnerId(userId));
        propertyRepository.flush();
        userRepository.deleteById(userId);
    }

    public List<Property> getPendingProperties() {
        return propertyRepository.findByApprovedFalse();
    }

    public Property approveProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        property.setApproved(true);
        return propertyRepository.save(property);
    }

    public void deleteProperty(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }
        propertyRepository.deleteById(propertyId);
    }
}
