package com.payment;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for {@link PaymentCoreApplication}.
 *
 * @author orvigas@gmail.com
 */
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

  @Test
  void mainDelegatesToSpringApplication() {
    // Stub the static launcher so main() is covered without booting a second context.
    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      springApplication
          .when(() -> SpringApplication.run(eq(PaymentCoreApplication.class), any(String[].class)))
          .thenReturn(mock(ConfigurableApplicationContext.class));

      PaymentCoreApplication.main(new String[]{});

      springApplication.verify(() -> SpringApplication.run(eq(PaymentCoreApplication.class), any(String[].class)));
    }
  }
}
