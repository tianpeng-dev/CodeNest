package com.codenest.backend.common;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void mapsValidationErrorsToBadRequestResponseContract() throws Exception {
    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000))
        .andExpect(jsonPath("$.message").value("Bad request"))
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  @Test
  void mapsBusinessExceptionToApiResponseContract() throws Exception {
    mockMvc
        .perform(get("/test/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(40004))
        .andExpect(jsonPath("$.message").value("missing"))
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  @RestController
  static class TestController {
    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestRequest request) {}

    @GetMapping("/test/business-error")
    void businessError() {
      throw new BusinessException(ErrorCode.NOT_FOUND, "missing");
    }
  }

  record TestRequest(@NotBlank String name) {}
}
