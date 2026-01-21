package uk.ac.ed.acp.cw2.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IlpRestServiceConfig.
 *
 * These tests verify the bean creation logic works correctly.
 * Uses system-stubs for environment variable manipulation.
 */
@ExtendWith(SystemStubsExtension.class)
class IlpRestServiceConfigTest {

    private final IlpRestServiceConfig config = new IlpRestServiceConfig();

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    @DisplayName("restTemplate() creates a non-null RestTemplate bean")
    void testRestTemplateCreation() {
        RestTemplate restTemplate = config.restTemplate();
        assertNotNull(restTemplate, "RestTemplate bean should not be null");
    }

    @Test
    @DisplayName("ilpEndpoint() returns default URL when environment variable is not set")
    void testIlpEndpointDefaultWhenNull() {
        environmentVariables.remove("ILP_ENDPOINT");

        String endpoint = config.ilpEndpoint();

        assertNotNull(endpoint);
        assertEquals("https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/", endpoint);
    }

    @Test
    @DisplayName("ilpEndpoint() returns default URL when environment variable is blank")
    void testIlpEndpointDefaultWhenBlank() {
        environmentVariables.set("ILP_ENDPOINT", "   ");

        String endpoint = config.ilpEndpoint();

        assertNotNull(endpoint);
        assertEquals("https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/", endpoint);
    }

    @Test
    @DisplayName("ilpEndpoint() returns environment variable value when set")
    void testIlpEndpointFromEnv() {
        String customEndpoint = "https://custom-endpoint.example.com/";
        environmentVariables.set("ILP_ENDPOINT", customEndpoint);

        String endpoint = config.ilpEndpoint();

        assertEquals(customEndpoint, endpoint);
    }
}