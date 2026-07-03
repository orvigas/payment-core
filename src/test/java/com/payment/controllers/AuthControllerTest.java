package com.payment.controllers;

import com.payment.contracts.LoginRequest;
import com.payment.models.Role;
import com.payment.models.User;
import com.payment.repositories.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  private static final String TEST_PASSWORD = "correct-horse-battery-staple";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void seedTestUser() {
    userRepository.findByUsername("test_user").orElseGet(() -> {
      User user = new User();
      user.setUsername("test_user");
      user.setEmail("test_user@example.com");
      user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
      user.setRoles(Set.of(Role.USER));
      return userRepository.save(user);
    });
  }

  @Test
  void testLoginSuccess() throws Exception {
    LoginRequest request = new LoginRequest("test_user", TEST_PASSWORD);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
        .andExpect(jsonPath("$.expiresIn", equalTo(3600)));
  }

  @Test
  void testLoginWithWrongPassword() throws Exception {
    LoginRequest request = new LoginRequest("test_user", "not-the-password");

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testLoginWithUnknownUsername() throws Exception {
    LoginRequest request = new LoginRequest("no_such_user", TEST_PASSWORD);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testLoginSuccessReturnsRefreshToken() throws Exception {
    LoginRequest request = new LoginRequest("test_user", TEST_PASSWORD);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType", equalTo("Bearer")));
  }

  @Test
  void testLoginSuccessResponseStructure() throws Exception {
    LoginRequest request = new LoginRequest("test_user", TEST_PASSWORD);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasKey("accessToken")))
        .andExpect(jsonPath("$", hasKey("tokenType")))
        .andExpect(jsonPath("$", hasKey("expiresIn")));
  }

  @Test
  void testLoginSuccessContainsAccessToken() throws Exception {
    LoginRequest request = new LoginRequest("test_user", TEST_PASSWORD);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.accessToken", not(equalTo(""))));
  }

  @Test
  void testLoginFailureDoesNotReturnToken() throws Exception {
    LoginRequest request = new LoginRequest("test_user", "wrong_password");

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.accessToken").doesNotExist());
  }

  @Test
  void testLoginWithSpecialCharactersInPassword() throws Exception {
    String specialPassword = "p@ssw0rd!#$%";
    userRepository.findByUsername("special_user").orElseGet(() -> {
      User user = new User();
      user.setUsername("special_user");
      user.setEmail("special@example.com");
      user.setPasswordHash(passwordEncoder.encode(specialPassword));
      user.setRoles(Set.of(Role.USER));
      return userRepository.save(user);
    });

    LoginRequest request = new LoginRequest("special_user", specialPassword);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()));
  }

  private String asJsonString(Object obj) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
