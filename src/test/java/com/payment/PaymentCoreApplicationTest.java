package com.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "spring.main.lazy-initialization=true")
class PaymentCoreApplicationTest {

  @Test
  void contextLoads() {
    assertTrue(true);
  }

  @Test
  void applicationClassExists() {
    assertNotNull(PaymentCoreApplication.class);
  }
}
