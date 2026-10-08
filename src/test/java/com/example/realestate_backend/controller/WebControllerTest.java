package com.example.realestate_backend.controller;

import com.example.realestate_backend.config.SecurityConfig;
import com.example.realestate_backend.entity.*;
import com.example.realestate_backend.repository.UserRepository;
import com.example.realestate_backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(WebController.class)
@Import({SecurityConfig.class, com.example.realestate_backend.security.JwtAuthenticationFilter.class,
        com.example.realestate_backend.security.JwtService.class})
class WebControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PropertyService propertyService;
    @MockitoBean UserService userService;
    @MockitoBean AdminService adminService;
    @MockitoBean CloudinaryService cloudinaryService;
    @MockitoBean UserRepository userRepository;

    @Test void rejectsInvalidParametersAndMethods() throws Exception {
        mvc.perform(get("/").param("minPrice", "bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/").param("type", "bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/properties/bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/").param("minPrice", "100").param("maxPrice", "1"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/register").with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    @Test void creationRequiresLoginAndCsrf() throws Exception {
        mvc.perform(post("/properties/create").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/properties/create").with(user("customer")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(propertyService, cloudinaryService);
    }

    @Test void customerCannotUseAdminEndpoints() throws Exception {
        mvc.perform(get("/admin").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/approve/1").with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminService);
    }

    @Test void registrationIgnoresPrivilegedFields() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name", "Test")
                .param("email", "test@example.com").param("password", "secret")
                .param("role", "ROLE_ADMIN").param("id", "42"))
                .andExpect(redirectedUrl("/login?registered"));
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userService).registerUser(captor.capture());
        assertNull(captor.getValue().getId());
        assertNull(captor.getValue().getRole());
    }

    @Test void propertyCreationIgnoresIdsAndApproval() throws Exception {
        User owner = User.builder().id(7L).email("test@example.com").build();
        when(userService.findByEmail(owner.getEmail())).thenReturn(owner);
        mvc.perform(post("/properties/create").with(user(owner.getEmail())).with(csrf())
                .param("title", "House").param("price", "100").param("type", "SALE")
                .param("location", "Chennai").param("id", "42").param("approved", "true")
                .param("owner.id", "99").param("imageUrl", "untrusted"))
                .andExpect(redirectedUrl("/dashboard?created"));
        var captor = org.mockito.ArgumentCaptor.forClass(Property.class);
        verify(propertyService).createProperty(captor.capture(), same(owner));
        assertNull(captor.getValue().getId());
        assertNull(captor.getValue().getOwner());
        assertNull(captor.getValue().getImageUrl());
        assertFalse(captor.getValue().isApproved());
    }

    @Test void unapprovedPropertyIsHiddenFromAnonymousVisitors() throws Exception {
        Property property = Property.builder().owner(User.builder().email("owner@example.com").build()).build();
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        mvc.perform(get("/properties/1")).andExpect(status().isNotFound());
        mvc.perform(get("/properties/1").with(user("owner@example.com"))).andExpect(status().isOk());
    }

    @Test void assetsAndAnonymousNavigationWork() throws Exception {
        mvc.perform(get("/css.css")).andExpect(status().isOk());
        mvc.perform(get("/js.js")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Logout"))));
    }
    @Test void missingResourcesAndInvalidAdminIdsReturnExpectedErrors() throws Exception {
        when(propertyService.getPropertyById(999L)).thenThrow(
                new com.example.realestate_backend.exception.ResourceNotFoundException("Property not found"));
        mvc.perform(get("/properties/999")).andExpect(status().isNotFound());
        mvc.perform(get("/properties/create").with(user("customer")))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/admin/approve/bad").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/users/delete/bad").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminService);
    }

    @Test void imageProviderFailureReturnsUsableForm() throws Exception {
        User owner = User.builder().id(7L).email("test@example.com").build();
        when(userService.findByEmail(owner.getEmail())).thenReturn(owner);
        when(cloudinaryService.uploadImage(any())).thenThrow(new java.io.IOException("Provider unavailable"));
        mvc.perform(multipart("/properties/create")
                .file(new org.springframework.mock.web.MockMultipartFile("imageFile", "house.png", "image/png", new byte[]{1}))
                .with(user(owner.getEmail())).with(csrf())
                .param("title", "House").param("price", "100").param("type", "SALE").param("location", "Chennai"))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Image upload failed")));
        verify(propertyService, never()).createProperty(any(), any());
    }

    @Test void registrationValidationErrorsAreVisible() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name", "Test")
                .param("email", "invalid-email").param("password", "secret"))
                .andExpect(status().isOk()).andExpect(view().name("register"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid email address")));
        verifyNoInteractions(userService);
    }
}
