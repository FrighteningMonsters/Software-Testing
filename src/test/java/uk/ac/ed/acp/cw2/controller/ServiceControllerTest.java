package uk.ac.ed.acp.cw2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.service.DroneService;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ServiceController using @WebMvcTest.
 *
 * Testing techniques demonstrated:
 * - Equivalence Partitioning: Testing valid vs null/invalid inputs
 * - Boundary Value Analysis: Testing empty vs populated collections
 * - HTTP Layer Integration Testing: Using MockMvc for realistic HTTP testing
 * - Mock-based testing: Isolating controller from service dependencies
 */
@WebMvcTest(ServiceController.class)
@Import(ServiceControllerTest.TestConfig.class)
class ServiceControllerTest {

    /**
     * Test configuration to provide the ilpEndpoint bean.
     */
    static class TestConfig {
        @Bean
        public String ilpEndpoint() {
            return "http://test-ilp-endpoint.example.com";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeoService geoService;

    @MockBean
    private DroneService droneService;

    // ========================================================================
    // Static Endpoint Tests
    // ========================================================================

    @Nested
    @DisplayName("GET / - Welcome Page")
    class IndexEndpointTests {

        @Test
        @DisplayName("Should return HTML welcome page")
        void shouldReturnWelcomePage() throws Exception {
            mockMvc.perform(get("/api/v1/"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Welcome from ILP")))
                    .andExpect(content().string(containsString("ILP-REST-Service-URL")));
        }
    }

    @Nested
    @DisplayName("GET /uid - Student UID")
    class UidEndpointTests {

        @Test
        @DisplayName("Should return correct student UID")
        void shouldReturnStudentUid() throws Exception {
            mockMvc.perform(get("/api/v1/uid"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("s2518554"));
        }
    }

    // ========================================================================
    // GeoService Endpoint Tests
    // ========================================================================

    @Nested
    @DisplayName("POST /distanceTo - Distance Calculation")
    class DistanceToEndpointTests {

        /**
         * Equivalence Partitioning: Valid request with two positions
         */
        @Test
        @DisplayName("Should calculate distance for valid positions")
        void shouldCalculateDistanceForValidPositions() throws Exception {
            DistanceRequest request = new DistanceRequest();
            request.position1 = new Position(-3.188267, 55.944425);
            request.position2 = new Position(-3.186874, 55.942617);

            when(geoService.calculateDistance(any(Position.class), any(Position.class)))
                    .thenReturn(0.002134);

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0.002134"));

            verify(geoService).calculateDistance(any(Position.class), any(Position.class));
        }

        /**
         * Equivalence Partitioning: Null positions should return null distance
         * Note: ResponseEntity.ok(null) returns an empty body
         */
        @Test
        @DisplayName("Should return empty body when positions are invalid")
        void shouldReturnNullForInvalidPositions() throws Exception {
            DistanceRequest request = new DistanceRequest();
            request.position1 = new Position(-3.188267, 55.944425);
            request.position2 = new Position(null, null);

            when(geoService.calculateDistance(any(Position.class), any(Position.class)))
                    .thenReturn(null);

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }
    }

    @Nested
    @DisplayName("POST /isCloseTo - Proximity Check")
    class IsCloseToEndpointTests {

        /**
         * Equivalence Partitioning: Positions that are close
         */
        @Test
        @DisplayName("Should return true when positions are close")
        void shouldReturnTrueWhenPositionsAreClose() throws Exception {
            DistanceRequest request = new DistanceRequest();
            request.position1 = new Position(-3.188267, 55.944425);
            request.position2 = new Position(-3.188268, 55.944426);

            when(geoService.isCloseTo(any(Position.class), any(Position.class)))
                    .thenReturn(true);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        /**
         * Equivalence Partitioning: Positions that are far apart
         */
        @Test
        @DisplayName("Should return false when positions are not close")
        void shouldReturnFalseWhenPositionsAreNotClose() throws Exception {
            DistanceRequest request = new DistanceRequest();
            request.position1 = new Position(-3.188267, 55.944425);
            request.position2 = new Position(-3.186874, 55.942617);

            when(geoService.isCloseTo(any(Position.class), any(Position.class)))
                    .thenReturn(false);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        /**
         * Equivalence Partitioning: Invalid positions
         * Note: ResponseEntity.ok(null) returns an empty body
         */
        @Test
        @DisplayName("Should return empty body for invalid positions")
        void shouldReturnNullForInvalidPositions() throws Exception {
            DistanceRequest request = new DistanceRequest();
            request.position1 = null;
            request.position2 = new Position(-3.186874, 55.942617);

            when(geoService.isCloseTo(any(), any())).thenReturn(null);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }
    }

    @Nested
    @DisplayName("POST /nextPosition - Position Calculation")
    class NextPositionEndpointTests {

        /**
         * Equivalence Partitioning: Valid position and legal angle
         */
        @Test
        @DisplayName("Should calculate next position for valid inputs")
        void shouldCalculateNextPositionForValidInputs() throws Exception {
            NextPositionRequest request = new NextPositionRequest();
            request.start = new Position(-3.188267, 55.944425);
            request.angle = 90.0;

            Position expectedResult = new Position(-3.188267, 55.94457);
            when(geoService.nextPosition(any(Position.class), eq(90.0)))
                    .thenReturn(expectedResult);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lng").value(-3.188267))
                    .andExpect(jsonPath("$.lat").value(55.94457));
        }

        /**
         * Equivalence Partitioning: Invalid/illegal angle
         * Note: ResponseEntity.ok(null) returns an empty body
         */
        @Test
        @DisplayName("Should return empty body for illegal angle")
        void shouldReturnNullForIllegalAngle() throws Exception {
            NextPositionRequest request = new NextPositionRequest();
            request.start = new Position(-3.188267, 55.944425);
            request.angle = 33.0; // Not a legal angle

            when(geoService.nextPosition(any(Position.class), eq(33.0)))
                    .thenReturn(null);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        /**
         * Boundary Value Analysis: Test with angle 0 degrees
         */
        @Test
        @DisplayName("Should handle boundary angle of 0 degrees")
        void shouldHandleBoundaryAngleZero() throws Exception {
            NextPositionRequest request = new NextPositionRequest();
            request.start = new Position(-3.188267, 55.944425);
            request.angle = 0.0;

            Position expectedResult = new Position(-3.188117, 55.944425);
            when(geoService.nextPosition(any(Position.class), eq(0.0)))
                    .thenReturn(expectedResult);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lng").value(-3.188117));
        }
    }

    @Nested
    @DisplayName("POST /isInRegion - Region Check")
    class IsInRegionEndpointTests {

        /**
         * Equivalence Partitioning: Position inside region
         */
        @Test
        @DisplayName("Should return true when position is inside region")
        void shouldReturnTrueWhenPositionInsideRegion() throws Exception {
            IsInRegionRequest request = new IsInRegionRequest();
            request.position = new Position(-3.1878, 55.9445);
            request.region = createValidRegion();

            when(geoService.isInRegion(any(Position.class), any(Region.class)))
                    .thenReturn(true);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        /**
         * Equivalence Partitioning: Position outside region
         */
        @Test
        @DisplayName("Should return false when position is outside region")
        void shouldReturnFalseWhenPositionOutsideRegion() throws Exception {
            IsInRegionRequest request = new IsInRegionRequest();
            request.position = new Position(-3.0, 55.0);
            request.region = createValidRegion();

            when(geoService.isInRegion(any(Position.class), any(Region.class)))
                    .thenReturn(false);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        /**
         * Equivalence Partitioning: Invalid region (null)
         * Note: ResponseEntity.ok(null) returns an empty body
         */
        @Test
        @DisplayName("Should return empty body for invalid region")
        void shouldReturnNullForInvalidRegion() throws Exception {
            IsInRegionRequest request = new IsInRegionRequest();
            request.position = new Position(-3.1878, 55.9445);
            request.region = null;

            when(geoService.isInRegion(any(), any())).thenReturn(null);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        private Region createValidRegion() {
            Region region = new Region();
            region.name = "TestRegion";
            region.vertices = Arrays.asList(
                    new Position(-3.19, 55.94),
                    new Position(-3.18, 55.94),
                    new Position(-3.18, 55.95),
                    new Position(-3.19, 55.95),
                    new Position(-3.19, 55.94)
            );
            return region;
        }
    }

    // ========================================================================
    // DroneService Endpoint Tests
    // ========================================================================

    @Nested
    @DisplayName("GET /dronesWithCooling/{state} - Drones by Cooling")
    class DronesWithCoolingEndpointTests {

        /**
         * Equivalence Partitioning: Cooling enabled (true)
         */
        @Test
        @DisplayName("Should return drones with cooling enabled")
        void shouldReturnDronesWithCoolingEnabled() throws Exception {
            when(droneService.findDronesWithCooling(true))
                    .thenReturn(Arrays.asList("drone1", "drone2"));

            mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0]").value("drone1"))
                    .andExpect(jsonPath("$[1]").value("drone2"));
        }

        /**
         * Equivalence Partitioning: Cooling disabled (false)
         */
        @Test
        @DisplayName("Should return drones without cooling")
        void shouldReturnDronesWithoutCooling() throws Exception {
            when(droneService.findDronesWithCooling(false))
                    .thenReturn(Arrays.asList("drone3", "drone4", "drone5"));

            mockMvc.perform(get("/api/v1/dronesWithCooling/false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        /**
         * Boundary Value Analysis: Empty result
         */
        @Test
        @DisplayName("Should return empty list when no matching drones")
        void shouldReturnEmptyListWhenNoMatchingDrones() throws Exception {
            when(droneService.findDronesWithCooling(true))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /droneDetails/{id} - Drone Details")
    class DroneDetailsEndpointTests {

        /**
         * Equivalence Partitioning: Valid drone ID
         */
        @Test
        @DisplayName("Should return drone details for valid ID")
        void shouldReturnDroneDetailsForValidId() throws Exception {
            Drone drone = new Drone();
            drone.id = "drone123";
            drone.name = "TestDrone";

            when(droneService.getDroneById("drone123")).thenReturn(drone);

            mockMvc.perform(get("/api/v1/droneDetails/drone123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("drone123"))
                    .andExpect(jsonPath("$.name").value("TestDrone"));
        }

        /**
         * Equivalence Partitioning: Invalid drone ID - returns 404
         */
        @Test
        @DisplayName("Should return 404 for non-existent drone ID")
        void shouldReturn404ForNonExistentDroneId() throws Exception {
            when(droneService.getDroneById("nonexistent")).thenReturn(null);

            mockMvc.perform(get("/api/v1/droneDetails/nonexistent"))
                    .andExpect(status().isNotFound());
        }

        /**
         * Boundary Value Analysis: Empty string ID
         */
        @Test
        @DisplayName("Should return 404 for empty drone ID result")
        void shouldReturn404ForEmptyDroneIdResult() throws Exception {
            when(droneService.getDroneById("")).thenReturn(null);

            mockMvc.perform(get("/api/v1/droneDetails/"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /queryAsPath/{attribute}/{value} - Query by Path")
    class QueryAsPathEndpointTests {

        /**
         * Equivalence Partitioning: Valid attribute query
         */
        @Test
        @DisplayName("Should return drones matching attribute query")
        void shouldReturnDronesMatchingAttributeQuery() throws Exception {
            when(droneService.queryAsPath("cooling", "true"))
                    .thenReturn(Arrays.asList("drone1", "drone2"));

            mockMvc.perform(get("/api/v1/queryAsPath/cooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0]").value("drone1"));
        }

        /**
         * Equivalence Partitioning: Query by name
         */
        @Test
        @DisplayName("Should return drones matching name query")
        void shouldReturnDronesMatchingNameQuery() throws Exception {
            when(droneService.queryAsPath("name", "Alpha"))
                    .thenReturn(Collections.singletonList("droneAlpha"));

            mockMvc.perform(get("/api/v1/queryAsPath/name/Alpha"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]").value("droneAlpha"));
        }

        /**
         * Boundary Value Analysis: No matches
         */
        @Test
        @DisplayName("Should return empty list for no matches")
        void shouldReturnEmptyListForNoMatches() throws Exception {
            when(droneService.queryAsPath("capacity", "9999"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/queryAsPath/capacity/9999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /query - Query with Multiple Attributes")
    class QueryEndpointTests {

        /**
         * Equivalence Partitioning: Valid query list
         */
        @Test
        @DisplayName("Should return drones matching multiple query attributes")
        void shouldReturnDronesMatchingMultipleAttributes() throws Exception {
            List<QueryAttribute> queries = new ArrayList<>();
            QueryAttribute q1 = new QueryAttribute();
            q1.attribute = "cooling";
            q1.operator = "=";
            q1.value = "true";
            queries.add(q1);

            QueryAttribute q2 = new QueryAttribute();
            q2.attribute = "capacity";
            q2.operator = ">";
            q2.value = "5.0";
            queries.add(q2);

            when(droneService.query(any())).thenReturn(Arrays.asList("drone1", "drone2"));

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(queries)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        /**
         * Boundary Value Analysis: Empty query list
         */
        @Test
        @DisplayName("Should handle empty query list")
        void shouldHandleEmptyQueryList() throws Exception {
            List<QueryAttribute> queries = Collections.emptyList();

            when(droneService.query(any())).thenReturn(Arrays.asList("drone1", "drone2", "drone3"));

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(queries)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        /**
         * Equivalence Partitioning: Single query attribute
         */
        @Test
        @DisplayName("Should return drones matching single query attribute")
        void shouldReturnDronesMatchingSingleAttribute() throws Exception {
            List<QueryAttribute> queries = new ArrayList<>();
            QueryAttribute q = new QueryAttribute();
            q.attribute = "id";
            q.operator = "=";
            q.value = "drone1";
            queries.add(q);

            when(droneService.query(any())).thenReturn(Collections.singletonList("drone1"));

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(queries)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]").value("drone1"));
        }
    }

    @Nested
    @DisplayName("POST /queryAvailableDrones - Query Available Drones")
    class QueryAvailableDronesEndpointTests {

        /**
         * Equivalence Partitioning: Valid dispatch records
         */
        @Test
        @DisplayName("Should return available drones for valid records")
        void shouldReturnAvailableDronesForValidRecords() throws Exception {
            List<MedDispatchRec> records = createSampleMedDispatchRecords();

            when(droneService.queryAvailableDrones(any()))
                    .thenReturn(Arrays.asList("drone1", "drone2"));

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(records)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        /**
         * Boundary Value Analysis: Empty records list
         */
        @Test
        @DisplayName("Should handle empty records list")
        void shouldHandleEmptyRecordsList() throws Exception {
            when(droneService.queryAvailableDrones(any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /calcDeliveryPath - Calculate Delivery Path")
    class CalcDeliveryPathEndpointTests {

        /**
         * Equivalence Partitioning: Valid delivery records
         */
        @Test
        @DisplayName("Should calculate delivery path for valid records")
        void shouldCalculateDeliveryPathForValidRecords() throws Exception {
            List<MedDispatchRec> records = createSampleMedDispatchRecords();

            CalcDeliveryPathResult result = new CalcDeliveryPathResult();
            result.totalCost = 15.5;
            result.totalMoves = 100;
            result.dronePaths = new ArrayList<>();

            when(droneService.calcDeliveryPath(any())).thenReturn(result);

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(records)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").value(15.5))
                    .andExpect(jsonPath("$.totalMoves").value(100));
        }

        /**
         * Boundary Value Analysis: Empty records list
         */
        @Test
        @DisplayName("Should return empty result for empty records")
        void shouldReturnEmptyResultForEmptyRecords() throws Exception {
            CalcDeliveryPathResult result = new CalcDeliveryPathResult();
            result.totalCost = 0.0;
            result.totalMoves = 0;
            result.dronePaths = new ArrayList<>();

            when(droneService.calcDeliveryPath(any())).thenReturn(result);

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").value(0.0))
                    .andExpect(jsonPath("$.totalMoves").value(0));
        }
    }

    @Nested
    @DisplayName("POST /calcDeliveryPathAsGeoJson - Calculate Path as GeoJSON")
    class CalcDeliveryPathAsGeoJsonEndpointTests {

        /**
         * Equivalence Partitioning: Valid delivery records
         */
        @Test
        @DisplayName("Should return GeoJSON for valid records")
        void shouldReturnGeoJsonForValidRecords() throws Exception {
            List<MedDispatchRec> records = createSampleMedDispatchRecords();

            String geoJson = "{\"type\":\"LineString\",\"coordinates\":[[-3.188,55.944],[-3.187,55.943]]}";
            when(droneService.calcDeliveryPathAsGeoJson(any())).thenReturn(geoJson);

            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(records)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("LineString"))
                    .andExpect(jsonPath("$.coordinates", hasSize(2)));
        }

        /**
         * Boundary Value Analysis: Empty records list
         */
        @Test
        @DisplayName("Should return empty GeoJSON for empty records")
        void shouldReturnEmptyGeoJsonForEmptyRecords() throws Exception {
            String emptyGeoJson = "{\"type\":\"LineString\",\"coordinates\":[]}";
            when(droneService.calcDeliveryPathAsGeoJson(any())).thenReturn(emptyGeoJson);

            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("\"coordinates\":[]")));
        }
    }

    // ========================================================================
    // Direct Unit Tests for Null Request Branches
    // These tests call controller methods directly to cover null request checks
    // that cannot be triggered via HTTP (Spring throws exception for null body)
    // ========================================================================

    @Nested
    @DisplayName("Direct Unit Tests - Null Request Handling")
    class NullRequestDirectTests {

        @Autowired
        private ServiceController controller;

        /**
         * Direct test to cover null request branch in distanceTo
         */
        @Test
        @DisplayName("distanceTo should return null for null request")
        void distanceToShouldReturnNullForNullRequest() {
            var response = controller.distanceTo(null);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            org.junit.jupiter.api.Assertions.assertNull(response.getBody());
        }

        /**
         * Direct test to cover null request branch in isCloseTo
         */
        @Test
        @DisplayName("isCloseTo should return null for null request")
        void isCloseToShouldReturnNullForNullRequest() {
            var response = controller.isCloseTo(null);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            org.junit.jupiter.api.Assertions.assertNull(response.getBody());
        }

        /**
         * Direct test to cover null request branch in nextPosition
         */
        @Test
        @DisplayName("nextPosition should return null for null request")
        void nextPositionShouldReturnNullForNullRequest() {
            var response = controller.nextPosition(null);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            org.junit.jupiter.api.Assertions.assertNull(response.getBody());
        }

        /**
         * Direct test to cover null request branch in isInRegion
         */
        @Test
        @DisplayName("isInRegion should return null for null request")
        void isInRegionShouldReturnNullForNullRequest() {
            var response = controller.isInRegion(null);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            org.junit.jupiter.api.Assertions.assertNull(response.getBody());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private List<MedDispatchRec> createSampleMedDispatchRecords() {
        List<MedDispatchRec> records = new ArrayList<>();

        MedDispatchRec rec = new MedDispatchRec();
        rec.id = 1;
        rec.date = "2024-01-15";
        rec.time = "10:00:00";
        rec.delivery = new Position(-3.188267, 55.944425);

        MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
        req.capacity = 2.0;
        req.cooling = true;
        req.heating = false;
        req.maxCost = 50.0;
        rec.requirements = req;

        records.add(rec);
        return records;
    }
}
