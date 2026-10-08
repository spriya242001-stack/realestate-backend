package com.example.realestate_backend;

import com.example.realestate_backend.entity.*;
import com.example.realestate_backend.repository.*;
import com.example.realestate_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiEndpointTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PropertyRepository properties;
    @Autowired JwtService jwt;

    @Test void bearerUploadsDoNotRequireCsrfButSessionUploadsDo() throws Exception {
        User owner = users.saveAndFlush(User.builder().name("Uploader").email("upload-api@example.com")
                .password("hash").role(Role.ROLE_CUSTOMER).build());
        mvc.perform(multipart("/properties/create")
                        .header("Authorization", "Bearer " + jwt.generateToken(owner.getEmail()))
                        .param("title", "Bearer upload").param("price", "1000")
                        .param("type", "SALE").param("location", "Chennai"))
                .andExpect(redirectedUrl("/dashboard?created"));
        assertEquals(1, properties.findByOwnerId(owner.getId()).size());
        for (String token : new String[]{"", "Bearer invalid"}) {
            mvc.perform(multipart("/properties/create").with(user(owner.getEmail()))
                            .header("Authorization", token)
                            .param("title", "Blocked upload").param("price", "1000")
                            .param("type", "SALE").param("location", "Chennai"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(multipart("/properties/create").with(user(owner.getEmail()))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "Browser upload").param("price", "1000")
                        .param("type", "SALE").param("location", "Chennai"))
                .andExpect(redirectedUrl("/dashboard?created"));
        assertEquals(2, properties.findByOwnerId(owner.getId()).size());
    }

    @Test void listingResponseOmitsUserDataAndOtherOwners() throws Exception {
        User owner = users.saveAndFlush(User.builder().name("Owner").email("safe-api@example.com")
                .password("private-password-hash").role(Role.ROLE_CUSTOMER).build());
        User other = users.saveAndFlush(User.builder().name("Other").email("other-api@example.com")
                .password("other-hash").role(Role.ROLE_CUSTOMER).build());
        Property listing = properties.saveAndFlush(Property.builder().title("My home").price(100.0)
                .type(PropertyType.SALE).location("Chennai").owner(owner).build());
        properties.saveAndFlush(Property.builder().title("Other home").price(200.0)
                .type(PropertyType.RENT).location("Chennai").owner(other).build());
        // Exercise the bidirectional graph that previously leaked user data.
        owner.getProperties().add(listing);
        mvc.perform(get("/api/properties/my-properties")
                        .header("Authorization", "Bearer " + jwt.generateToken(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].title").value("My home"))
                .andExpect(jsonPath("$[0].owner").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-password-hash"))));
    }

    @Test void anonymousAndInvalidTokenReturnJsonUnauthorized() throws Exception {
        mvc.perform(get("/api/properties/my-properties"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("error").exists())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/api/properties/my-properties").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("error").exists());
    }

    @Test void missingUserAndServiceErrorsReturnJson() throws Exception {
        mvc.perform(get("/api/properties/my-properties").with(user("missing-api@example.com")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("error").value("Resource not found"));
    }

    @Test void registrationRejectsInvalidFields() throws Exception {
        for (String body : new String[]{"{}", "{\"name\":\"Test\",\"email\":\"test@example.com\"}",
                "{\"name\":\"Test\",\"email\":\"bad\",\"password\":\"secret\"}"}) {
            mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("error").exists());
        }
    }

    @Test void registrationIgnoresPrivilegedFieldsAndNestedListings() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"name\":\"New\",\"email\":\"new-api@example.com\",\"password\":\"secret\",\"id\":999999,\"role\":\"ROLE_ADMIN\",\"properties\":[{\"approved\":true}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("email").value("new-api@example.com"));
        User saved = users.findByEmail("new-api@example.com").orElseThrow();
        assertEquals(Role.ROLE_CUSTOMER, saved.getRole());
        assertNotEquals(999999L, saved.getId());
        assertTrue(properties.findByOwnerId(saved.getId()).isEmpty());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"name\":\"New\",\"email\":\"new-api@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("An account with this email already exists. Please log in or use another email."));
    }

    @Test void registrationExplainsInvalidFieldsAndMalformedJson() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("fields.name").exists())
                .andExpect(jsonPath("fields.email").exists()).andExpect(jsonPath("fields.password").exists());
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value(org.hamcrest.Matchers.containsString("valid JSON object")));
    }
}
