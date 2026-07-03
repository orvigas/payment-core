package com.payment.controllers;

import com.payment.contracts.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testLoginSuccess() throws Exception {
    LoginRequest request = new LoginRequest("test_user");

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
        .andExpect(jsonPath("$.expiresIn", equalTo(3600)));
  }

  private String asJsonString(Object obj) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}