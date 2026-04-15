package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DemoController.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigService configService;

    @Test
    void demo_returnsEnabled_whenFeatureOn() throws Exception {
        when(configService.isFeatureEnabled("feature.newCheckout", "dave")).thenReturn(true);

        mockMvc.perform(get("/api/demo").param("userId", "dave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("dave"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.result").value("NEW checkout flow"));
    }

    @Test
    void demo_returnsDisabled_whenFeatureOff() throws Exception {
        when(configService.isFeatureEnabled("feature.newCheckout", "priya")).thenReturn(false);

        mockMvc.perform(get("/api/demo").param("userId", "priya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("priya"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.result").value("OLD checkout flow"));
    }

    @Test
    void demo_handlesNullUserId() throws Exception {
        when(configService.isFeatureEnabled("feature.newCheckout", null)).thenReturn(false);

        mockMvc.perform(get("/api/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void demo_allowsArbitraryFeatureKey() throws Exception {
        when(configService.isFeatureEnabled("feature.checkout1", "dave")).thenReturn(true);

        mockMvc.perform(get("/api/demo").param("userId", "dave").param("feature", "feature.checkout1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature").value("feature.checkout1"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void demo_rejectsNonFeatureKeys() throws Exception {
        mockMvc.perform(get("/api/demo").param("userId", "dave").param("feature", "payments.timeoutMs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
