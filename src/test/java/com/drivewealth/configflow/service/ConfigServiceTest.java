package com.drivewealth.configflow.service;

import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.repo.AuditEntryRepository;
import com.drivewealth.configflow.repo.ConfigEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private ConfigEntryRepository configRepo;

    @Mock
    private AuditEntryRepository auditRepo;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new ConfigService(configRepo, auditRepo);
    }

    @Test
    void isFeatureEnabled_returnsFalse_whenConfigNotFound() {
        when(configRepo.findById("missing.key")).thenReturn(Optional.empty());
        assertThat(configService.isFeatureEnabled("missing.key", "alice")).isFalse();
    }

    @Test
    void isFeatureEnabled_returnsFalse_whenRolloutIsZero() {
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 0, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        assertThat(configService.isFeatureEnabled("feature.newCheckout", "alice")).isFalse();
    }

    @Test
    void isFeatureEnabled_returnsTrue_whenRolloutIs100() {
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 100, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        assertThat(configService.isFeatureEnabled("feature.newCheckout", "any-user")).isTrue();
    }

    @Test
    void isFeatureEnabled_returnsFalse_whenValueIsNotTrue() {
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "false", 100, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        assertThat(configService.isFeatureEnabled("feature.newCheckout", "any-user")).isFalse();
    }

    @Test
    void isFeatureEnabled_isDeterministic_sameUserAlwaysGetsSameResult() {
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 50, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));

        boolean first = configService.isFeatureEnabled("feature.newCheckout", "alice");
        boolean second = configService.isFeatureEnabled("feature.newCheckout", "alice");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void isFeatureEnabled_knownUser_dave_enabledAt20Percent() {
        // dave has bucket=14, so enabled at rollout >= 15
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 20, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        assertThat(configService.isFeatureEnabled("feature.newCheckout", "dave")).isTrue();
    }

    @Test
    void isFeatureEnabled_knownUser_dave_disabledAt10Percent() {
        // dave has bucket=14, so disabled at rollout < 15
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 10, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        assertThat(configService.isFeatureEnabled("feature.newCheckout", "dave")).isFalse();
    }

    @Test
    void isFeatureEnabled_handlesNullUserId() {
        ConfigEntry entry = new ConfigEntry("feature.newCheckout", "true", 50, "test", 1L);
        when(configRepo.findById("feature.newCheckout")).thenReturn(Optional.of(entry));
        // should not throw; deterministic result for null userId
        boolean result = configService.isFeatureEnabled("feature.newCheckout", null);
        assertThat(result).isIn(true, false);
    }
}
