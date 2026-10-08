package com.example.realestate_backend;

import com.example.realestate_backend.entity.*;
import com.example.realestate_backend.repository.*;
import com.example.realestate_backend.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndToEndFlowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PropertyRepository properties;
    @Autowired PasswordEncoder encoder;
    @MockitoBean CloudinaryService cloudinary;

    @Test void registrationLoginListingApprovalSearchAndDeletion() throws Exception {
        String email = "flow@example.com";
        String password = "test-password";
        mvc.perform(post("/register").with(csrf()).param("name", "Flow Customer")
                .param("email", email).param("password", password).param("role", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/login?registered"));
        User customer = users.findByEmail(email).orElseThrow();
        assertEquals(Role.ROLE_CUSTOMER, customer.getRole());
        assertTrue(encoder.matches(password, customer.getPassword()));
        mvc.perform(post("/register").with(csrf()).param("name", "Duplicate")
                .param("email", email).param("password", password))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("user", "email"));

        MockHttpSession customerSession = login(email, password);
        mvc.perform(get("/dashboard").session(customerSession)).andExpect(status().isOk());
        mvc.perform(post("/properties/create").session(customerSession).with(csrf())
                .param("title", "Invalid").param("price", "-1").param("type", "SALE").param("location", "Chennai"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("newProperty", "price"));
        when(cloudinary.uploadImage(any())).thenReturn("https://example.com/test-house.png");
        mvc.perform(multipart("/properties/create")
                .file(new MockMultipartFile("imageFile", "house.png", "image/png", new byte[]{1,2,3}))
                .session(customerSession).with(csrf()).param("title", "Flow House").param("price", "1000")
                .param("type", "SALE").param("location", "Chennai"))
                .andExpect(redirectedUrl("/dashboard?created"));
        Property property = properties.findByOwnerId(customer.getId()).getFirst();
        assertFalse(property.isApproved());
        assertEquals("https://example.com/test-house.png", property.getImageUrl());
        mvc.perform(get("/properties/" + property.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/properties/" + property.getId()).session(customerSession)).andExpect(status().isOk());
        mvc.perform(get("/").param("keyword", "Flow House"))
                .andExpect(model().attribute("properties", org.hamcrest.Matchers.empty()));
        mvc.perform(post("/admin/approve/" + property.getId()).session(customerSession).with(csrf()))
                .andExpect(status().isForbidden());

        users.saveAndFlush(User.builder().name("Flow Admin").email("admin-flow@example.com")
                .password(encoder.encode(password)).role(Role.ROLE_ADMIN).build());
        MockHttpSession adminSession = login("admin-flow@example.com", password);
        mvc.perform(get("/admin").session(adminSession)).andExpect(status().isOk());
        mvc.perform(post("/admin/approve/" + property.getId()).session(adminSession).with(csrf()))
                .andExpect(redirectedUrl("/admin?approved"));
        mvc.perform(get("/").param("keyword", "Flow House").param("type", "SALE")
                .param("minPrice", "500").param("maxPrice", "1500"))
                .andExpect(status().isOk()).andExpect(model().attribute("properties", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/properties/" + property.getId())).andExpect(status().isOk());
        mvc.perform(post("/logout").session(customerSession).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        mvc.perform(post("/admin/users/delete/" + customer.getId()).session(adminSession).with(csrf()))
                .andExpect(redirectedUrl("/admin?userDeleted"));
        users.flush();
        assertFalse(users.existsById(customer.getId()));
        assertFalse(properties.existsById(property.getId()));
    }

    @Test void apiLoginIssuesUsableTokenAndRejectsInvalidCredentials() throws Exception {
        String email = "jwt-flow@example.com";
        users.saveAndFlush(User.builder().name("JWT Customer").email(email)
                .password(encoder.encode("test-password")).role(Role.ROLE_CUSTOMER).build());
        String response = mvc.perform(post("/api/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"jwt-flow@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(email))
                .andExpect(jsonPath("token").isString()).andReturn().getResponse().getContentAsString();
        String token = response.split("\"token\":\"")[1].split("\"")[0];
        mvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"jwt-flow@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("error").value("Invalid email or password"));
    }

    @Test void apiLoginRejectsMissingInvalidAndMalformedFields() throws Exception {
        for (String body : new String[]{"{}", "null", "{", "{\"email\":\"invalid\",\"password\":\"secret\"}",
                "{\"email\":\"user@example.com\",\"password\":\" \"}"}) {
            mvc.perform(post("/api/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("error").exists());
        }
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", email).param("password", password))
                .andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession(false);
    }
}
