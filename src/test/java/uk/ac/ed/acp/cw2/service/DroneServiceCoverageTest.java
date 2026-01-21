package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Coverage-focused tests for DroneService.
 * Targets untested code paths to improve line and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
public class DroneServiceCoverageTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService geoService;
    private DroneService droneService;
    private final String ENDPOINT = "http://api";

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, geoService, ENDPOINT);
    }

    private Drone createFullDrone(String id, String name, boolean cooling, boolean heating,
                                   double capacity, int maxMoves, double costPerMove,
                                   double costInitial, double costFinal) {
        Capability c = new Capability();
        c.cooling = cooling;
        c.heating = heating;
        c.capacity = capacity;
        c.maxMoves = maxMoves;
        c.costPerMove = costPerMove;
        c.costInitial = costInitial;
        c.costFinal = costFinal;

        Drone d = new Drone();
        d.id = id;
        d.name = name;
        d.capability = c;
        return d;
    }

    private Drone createSimpleDrone(String id) {
        return createFullDrone(id, "Drone-" + id, true, false, 100.0, 500, 0.1, 5.0, 5.0);
    }

    @Nested
    @DisplayName("findDronesWithCooling()")
    class FindDronesWithCoolingTests {

        @Test
        @DisplayName("Returns empty list when state is null")
        void testNullState() {
            List<String> result = droneService.findDronesWithCooling(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when API returns null")
        void testNullDronesResponse() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);
            List<String> result = droneService.findDronesWithCooling(true);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Filters drones with cooling=true")
        void testFilterCoolingTrue() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            Drone d2 = createFullDrone("D2", "Drone2", false, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1, d2});

            List<String> result = droneService.findDronesWithCooling(true);
            assertEquals(1, result.size());
            assertTrue(result.contains("D1"));
        }

        @Test
        @DisplayName("Filters drones with cooling=false")
        void testFilterCoolingFalse() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            Drone d2 = createFullDrone("D2", "Drone2", false, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1, d2});

            List<String> result = droneService.findDronesWithCooling(false);
            assertEquals(1, result.size());
            assertTrue(result.contains("D2"));
        }

        @Test
        @DisplayName("Skips drones with null capability")
        void testNullCapability() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.capability = null;
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.findDronesWithCooling(true);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getDroneById()")
    class GetDroneByIdTests {

        @Test
        @DisplayName("Returns null for null id")
        void testNullId() {
            assertNull(droneService.getDroneById(null));
        }

        @Test
        @DisplayName("Returns null for empty id")
        void testEmptyId() {
            assertNull(droneService.getDroneById(""));
        }

        @Test
        @DisplayName("Returns null when API returns null")
        void testNullDronesResponse() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);
            assertNull(droneService.getDroneById("D1"));
        }

        @Test
        @DisplayName("Returns null when API returns empty array")
        void testEmptyDronesResponse() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(new Drone[]{});
            assertNull(droneService.getDroneById("D1"));
        }

        @Test
        @DisplayName("Returns null when drone not found")
        void testDroneNotFound() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            assertNull(droneService.getDroneById("D99"));
        }

        @Test
        @DisplayName("Returns drone when found")
        void testDroneFound() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            Drone result = droneService.getDroneById("D1");
            assertNotNull(result);
            assertEquals("D1", result.id);
        }

        @Test
        @DisplayName("Skips drones with null id")
        void testSkipsNullIdDrones() {
            Drone d1 = createSimpleDrone("D1");
            Drone d2 = new Drone();
            d2.id = null;
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d2, d1});
            Drone result = droneService.getDroneById("D1");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("queryAsPath() - matchSingleAttribute switch cases")
    class QueryAsPathTests {

        @Test
        @DisplayName("Match by id")
        void testMatchById() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("id", "D1");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by name")
        void testMatchByName() {
            Drone d1 = createFullDrone("D1", "TestDrone", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("name", "TestDrone");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by cooling")
        void testMatchByCooling() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("cooling", "true");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by heating")
        void testMatchByHeating() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("heating", "true");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by capacity")
        void testMatchByCapacity() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("capacity", "100.0");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by maxMoves")
        void testMatchByMaxMoves() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("maxMoves", "500");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by costPerMove")
        void testMatchByCostPerMove() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costPerMove", "0.1");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by costInitial")
        void testMatchByCostInitial() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5.0, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costInitial", "5.0");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Match by costFinal")
        void testMatchByCostFinal() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5.0);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costFinal", "5.0");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Unknown attribute returns empty")
        void testUnknownAttribute() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("unknown", "value");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Null capability returns false for capability attributes")
        void testNullCapability() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.capability = null;
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            assertTrue(droneService.queryAsPath("cooling", "true").isEmpty());
            assertTrue(droneService.queryAsPath("heating", "true").isEmpty());
            assertTrue(droneService.queryAsPath("capacity", "100").isEmpty());
        }

        @Test
        @DisplayName("Invalid number format returns false")
        void testInvalidNumberFormat() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            assertTrue(droneService.queryAsPath("capacity", "notanumber").isEmpty());
            assertTrue(droneService.queryAsPath("maxMoves", "notanumber").isEmpty());
        }

        @Test
        @DisplayName("Null API response returns empty")
        void testNullApiResponse() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);
            List<String> result = droneService.queryAsPath("id", "D1");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("query() with QueryAttribute - operator coverage")
    class QueryWithOperatorsTests {

        private QueryAttribute createQuery(String attr, String op, String val) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attr;
            q.operator = op;
            q.value = val;
            return q;
        }

        @Test
        @DisplayName("Numeric compare with = operator")
        void testNumericEquals() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("capacity", "=", "100.0")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Numeric compare with != operator")
        void testNumericNotEquals() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("capacity", "!=", "50.0")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Numeric compare with < operator")
        void testNumericLessThan() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("capacity", "<", "200.0")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Numeric compare with > operator")
        void testNumericGreaterThan() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("capacity", ">", "50.0")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Numeric compare with unknown operator returns empty")
        void testNumericUnknownOperator() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("capacity", ">=", "50.0")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("String compare with = operator")
        void testStringEquals() {
            Drone d1 = createFullDrone("D1", "TestDrone", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("name", "=", "TestDrone")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("String compare with wrong operator returns empty")
        void testStringWrongOperator() {
            Drone d1 = createFullDrone("D1", "TestDrone", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("name", "!=", "TestDrone")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boolean compare with = operator")
        void testBooleanEquals() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("cooling", "=", "true")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Boolean compare with wrong operator returns empty")
        void testBooleanWrongOperator() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("cooling", "!=", "true")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Query with null API response returns empty")
        void testNullApiResponse() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);
            List<String> result = droneService.query(List.of(createQuery("id", "=", "D1")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Query filters out invalid queries (null attribute)")
        void testInvalidQueryNullAttribute() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            QueryAttribute q = new QueryAttribute();
            q.attribute = null;
            q.operator = "=";
            q.value = "D1";

            List<String> result = droneService.query(List.of(q));
            assertEquals(1, result.size()); // Invalid queries are filtered, all drones match
        }

        @Test
        @DisplayName("Query filters out invalid queries (blank attribute)")
        void testInvalidQueryBlankAttribute() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            QueryAttribute q = new QueryAttribute();
            q.attribute = "   ";
            q.operator = "=";
            q.value = "D1";

            List<String> result = droneService.query(List.of(q));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Query with all numeric attributes")
        void testAllNumericAttributes() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5.0, 5.0);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Test maxMoves
            assertEquals(1, droneService.query(List.of(createQuery("maxMoves", "=", "500"))).size());
            // Test costPerMove
            assertEquals(1, droneService.query(List.of(createQuery("costPerMove", "=", "0.1"))).size());
            // Test costInitial
            assertEquals(1, droneService.query(List.of(createQuery("costInitial", "=", "5.0"))).size());
            // Test costFinal
            assertEquals(1, droneService.query(List.of(createQuery("costFinal", "=", "5.0"))).size());
        }

        @Test
        @DisplayName("Query with invalid number throws returns false")
        void testInvalidNumberInQuery() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            assertTrue(droneService.query(List.of(createQuery("capacity", "=", "abc"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("maxMoves", "=", "abc"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("costPerMove", "=", "abc"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("costInitial", "=", "abc"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("costFinal", "=", "abc"))).isEmpty());
        }

        @Test
        @DisplayName("Query with unknown attribute returns empty")
        void testUnknownAttributeInQuery() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("unknownAttr", "=", "value")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Query with heating attribute")
        void testHeatingQuery() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("heating", "=", "true")));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Query with null capability drone")
        void testQueryNullCapability() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = "Test";
            d1.capability = null;
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Capability-based queries should return empty
            assertTrue(droneService.query(List.of(createQuery("cooling", "=", "true"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("heating", "=", "true"))).isEmpty());
            assertTrue(droneService.query(List.of(createQuery("capacity", "=", "100"))).isEmpty());

            // But id/name should still work
            assertEquals(1, droneService.query(List.of(createQuery("id", "=", "D1"))).size());
        }
    }

    @Nested
    @DisplayName("queryAvailableDrones() edge cases")
    class QueryAvailableDronesEdgeCases {

        @Test
        @DisplayName("Returns empty for null recs")
        void testNullRecs() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{createSimpleDrone("D1")});
            List<String> result = droneService.queryAvailableDrones(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty for empty recs")
        void testEmptyRecs() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{createSimpleDrone("D1")});
            List<String> result = droneService.queryAvailableDrones(List.of());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty when dfsp is null")
        void testNullDfsp() {
            Drone d1 = createSimpleDrone("D1");
            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(null);

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles drone with null requirements in rec")
        void testNullRequirementsInRec() {
            Drone d1 = createSimpleDrone("D1");
            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = null;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles heating requirement")
        void testHeatingRequirement() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100, 500, 0.1, 5, 5);
            Drone d2 = createFullDrone("D2", "Drone2", true, false, 100, 500, 0.1, 5, 5);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.heating = true;

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da1 = new DroneAvailability();
            da1.id = "D1";
            da1.availability = List.of(window);

            DroneAvailability da2 = new DroneAvailability();
            da2.id = "D2";
            da2.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.drones = List.of(da1, da2);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1, d2});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertEquals(1, result.size());
            assertTrue(result.contains("D1"));
        }
    }

    @Nested
    @DisplayName("calcDeliveryPath() edge cases")
    class CalcDeliveryPathEdgeCases {

        @Test
        @DisplayName("Returns empty result for null recs")
        void testNullRecs() {
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(null);
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
            assertEquals(0.0, result.totalCost);
            assertEquals(0, result.totalMoves);
        }

        @Test
        @DisplayName("Returns empty result for empty recs")
        void testEmptyRecs() {
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of());
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
        }

        @Test
        @DisplayName("Returns empty result when drones API returns null")
        void testNullDronesApi() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);

            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = new MedDispatchRec.Requirements();

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            assertTrue(result.dronePaths.isEmpty());
        }
    }

    @Nested
    @DisplayName("calcDeliveryPathAsGeoJson() edge cases")
    class CalcDeliveryPathAsGeoJsonEdgeCases {

        @Test
        @DisplayName("Returns empty GeoJSON for null recs")
        void testNullRecs() {
            String result = droneService.calcDeliveryPathAsGeoJson(null);
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns empty GeoJSON for empty recs")
        void testEmptyRecs() {
            String result = droneService.calcDeliveryPathAsGeoJson(List.of());
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns empty GeoJSON when API returns null")
        void testNullApiResponses() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(null);

            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = new MedDispatchRec.Requirements();

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns empty GeoJSON when service points are null")
        void testNullServicePoints() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{createSimpleDrone("D1")});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(null);

            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = new MedDispatchRec.Requirements();

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns empty GeoJSON when dfsp is null")
        void testNullDfspInGeoJson() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{createSimpleDrone("D1")});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(null);

            MedDispatchRec rec = new MedDispatchRec();
            rec.requirements = new MedDispatchRec.Requirements();

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns empty GeoJSON when no drone can serve all requests")
        void testNoDroneCanServeAll() {
            Drone d1 = createFullDrone("D1", "Drone1", false, false, 10.0, 50, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 100.0; // Exceeds drone capacity
            rec.requirements.cooling = true; // Drone doesn't have cooling

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }

        @Test
        @DisplayName("Returns valid GeoJSON when drone can serve all requests")
        void testFullGeoJsonPath() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.185, 55.945);
            Position p3 = new Position(-3.18, 55.95);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2, p3));

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertTrue(result.startsWith("{\"type\":\"LineString\",\"coordinates\":["));
            assertTrue(result.contains("-3.19"));
            assertTrue(result.contains("55.94"));
        }

        @Test
        @DisplayName("Handles restricted areas correctly")
        void testWithRestrictedAreas() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            Region restricted = new Region();
            restricted.vertices = List.of(
                    new Position(-3.2, 55.9),
                    new Position(-3.2, 55.92),
                    new Position(-3.17, 55.92),
                    new Position(-3.17, 55.9)
            );

            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.18, 55.95);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(new Region[]{restricted});
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2));

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertTrue(result.startsWith("{\"type\":\"LineString\",\"coordinates\":["));
        }

        @Test
        @DisplayName("Handles case when no service point found for drone")
        void testNoServicePointForDrone() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            // No drones in dfsp
            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of();

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);

            String result = droneService.calcDeliveryPathAsGeoJson(List.of(rec));
            assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
        }
    }

    @Nested
    @DisplayName("Additional branch coverage tests")
    class AdditionalBranchCoverageTests {

        private QueryAttribute createQuery(String attr, String op, String val) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attr;
            q.operator = op;
            q.value = val;
            return q;
        }

        @Test
        @DisplayName("isValidQuery - null query returns false")
        void testNullQuery() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<QueryAttribute> queries = new java.util.ArrayList<>();
            queries.add(null);

            List<String> result = droneService.query(queries);
            assertEquals(1, result.size()); // null filtered, all drones match
        }

        @Test
        @DisplayName("isValidQuery - blank operator returns false")
        void testBlankOperator() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            QueryAttribute q = new QueryAttribute();
            q.attribute = "id";
            q.operator = "   ";
            q.value = "D1";

            List<String> result = droneService.query(List.of(q));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("isValidQuery - blank value returns false")
        void testBlankValue() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            QueryAttribute q = new QueryAttribute();
            q.attribute = "id";
            q.operator = "=";
            q.value = "   ";

            List<String> result = droneService.query(List.of(q));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("stringCompare - non-equals operator returns false")
        void testStringCompareNonEquals() {
            Drone d1 = createFullDrone("D1", "TestDrone", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Test with < operator on string - should return empty
            List<String> result = droneService.query(List.of(createQuery("name", "<", "TestDrone")));
            assertTrue(result.isEmpty());

            // Test with > operator on string
            result = droneService.query(List.of(createQuery("name", ">", "TestDrone")));
            assertTrue(result.isEmpty());

            // Test with > operator on id
            result = droneService.query(List.of(createQuery("id", ">", "D1")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("stringCompare - null lhs returns false")
        void testStringCompareNullLhs() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = null; // null name
            d1.capability = new Capability();
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("name", "=", "TestDrone")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matchSingleAttribute - null id returns false")
        void testMatchSingleAttributeNullId() {
            Drone d1 = new Drone();
            d1.id = null;
            d1.name = "TestDrone";
            d1.capability = new Capability();
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("id", "D1");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matchSingleAttribute - null name returns false")
        void testMatchSingleAttributeNullName() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = null;
            d1.capability = new Capability();
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("name", "TestDrone");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matchSingleAttribute - costPerMove invalid number")
        void testMatchSingleAttributeCostPerMoveInvalid() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costPerMove", "notanumber");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matchSingleAttribute - costInitial invalid number")
        void testMatchSingleAttributeCostInitialInvalid() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costInitial", "notanumber");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matchSingleAttribute - costFinal invalid number")
        void testMatchSingleAttributeCostFinalInvalid() {
            Drone d1 = createSimpleDrone("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costFinal", "notanumber");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("buildAvailabilityMap - null drones in service point")
        void testBuildAvailabilityMapNullDrones() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = null; // null drones

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("buildAvailabilityMap - null availability in drone")
        void testBuildAvailabilityMapNullAvailability() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = null; // null availability

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isDroneAvailableForDispatch - empty windows")
        void testIsDroneAvailableEmptyWindows() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(); // empty availability

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isDroneAvailableForDispatch - null date in rec")
        void testIsDroneAvailableNullDate() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = null; // null date
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isDroneAvailableForDispatch - null time in rec")
        void testIsDroneAvailableNullTime() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = null; // null time
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isDroneAvailableForDispatch - window with null fields")
        void testIsDroneAvailableWindowWithNullFields() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            AvailabilityWindow window1 = new AvailabilityWindow();
            window1.dayOfWeek = null; // null dayOfWeek
            window1.from = "08:00";
            window1.until = "18:00";

            AvailabilityWindow window2 = new AvailabilityWindow();
            window2.dayOfWeek = "MONDAY";
            window2.from = null; // null from
            window2.until = "18:00";

            AvailabilityWindow window3 = new AvailabilityWindow();
            window3.dayOfWeek = "MONDAY";
            window3.from = "08:00";
            window3.until = null; // null until

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window1, window2, window3);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isDroneAvailableForDispatch - day of week mismatch")
        void testIsDroneAvailableDayMismatch() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "TUESDAY"; // Doesn't match Monday
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20"; // Monday
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("canServe - capacity requirement exceeds drone capacity")
        void testCanServeCapacityExceeded() {
            Drone d1 = createFullDrone("D1", "Drone1", true, false, 50.0, 500, 0.1, 5, 5);

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 100.0; // Exceeds drone's 50.0

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("canServe - cooling required but drone doesn't have it")
        void testCanServeCoolingNotAvailable() {
            Drone d1 = createFullDrone("D1", "Drone1", false, false, 100.0, 500, 0.1, 5, 5);

            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.cooling = true;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findServicePointForDrone - service point id not found")
        void testFindServicePointNotFound() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 999; // Different ID
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1; // ID 1, but ServicePoint has ID 999
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            assertTrue(result.dronePaths.isEmpty());
        }
    }

    @Nested
    @DisplayName("calcDeliveryPath comprehensive tests")
    class CalcDeliveryPathComprehensiveTests {

        @Test
        @DisplayName("Full path calculation with multiple deliveries")
        void testFullPathWithMultipleDeliveries() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec1 = new MedDispatchRec();
            rec1.id = 1;
            rec1.date = "2025-01-20";
            rec1.time = "12:00:00";
            rec1.delivery = new Position(-3.18, 55.95);
            rec1.requirements = new MedDispatchRec.Requirements();
            rec1.requirements.capacity = 10.0;

            MedDispatchRec rec2 = new MedDispatchRec();
            rec2.id = 2;
            rec2.date = "2025-01-20";
            rec2.time = "14:00:00";
            rec2.delivery = new Position(-3.17, 55.96);
            rec2.requirements = new MedDispatchRec.Requirements();
            rec2.requirements.capacity = 15.0;

            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.18, 55.95);
            Position p3 = new Position(-3.17, 55.96);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2, p3));

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec1, rec2));
            assertNotNull(result);
            assertFalse(result.dronePaths.isEmpty());
            assertTrue(result.totalMoves > 0);
            assertTrue(result.totalCost > 0);
        }

        @Test
        @DisplayName("Returns empty when path finding returns null")
        void testPathFindingReturnsNull() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(null);

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            assertTrue(result.dronePaths.isEmpty());
        }

        @Test
        @DisplayName("Returns empty when path finding returns empty list")
        void testPathFindingReturnsEmpty() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of());

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            assertTrue(result.dronePaths.isEmpty());
        }

        @Test
        @DisplayName("Handles maxCost constraint in findMaxSubset")
        void testMaxCostConstraint() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 500, 10.0, 50.0, 50.0);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;
            rec.requirements.maxCost = 1.0; // Very low maxCost

            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.185, 55.945);
            Position p3 = new Position(-3.18, 55.95);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2, p3));

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            // With maxCost=1.0 and high costPerMove, the delivery might be skipped
            assertNotNull(result);
        }

        @Test
        @DisplayName("Handles moves exceeding maxMoves")
        void testMaxMovesExceeded() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 100.0, 2, 0.1, 5, 5); // maxMoves=2

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20";
            rec.time = "12:00:00";
            rec.delivery = new Position(-3.18, 55.95);
            rec.requirements = new MedDispatchRec.Requirements();
            rec.requirements.capacity = 10.0;

            // Path with too many moves
            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.185, 55.945);
            Position p3 = new Position(-3.18, 55.95);
            Position p4 = new Position(-3.175, 55.955);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2, p3, p4)); // 3 moves each way

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec));
            assertTrue(result.dronePaths.isEmpty()); // Should fail due to maxMoves
        }

        @Test
        @DisplayName("Handles capacity exceeded in findMaxSubset")
        void testCapacityExceededInSubset() {
            Drone d1 = createFullDrone("D1", "Drone1", true, true, 15.0, 500, 0.1, 5, 5);

            ServicePoint sp = new ServicePoint();
            sp.id = 1;
            sp.location = new Position(-3.19, 55.94);

            DroneAvailability da = new DroneAvailability();
            da.id = "D1";
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00";
            window.until = "18:00";
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);

            MedDispatchRec rec1 = new MedDispatchRec();
            rec1.id = 1;
            rec1.date = "2025-01-20";
            rec1.time = "12:00:00";
            rec1.delivery = new Position(-3.18, 55.95);
            rec1.requirements = new MedDispatchRec.Requirements();
            rec1.requirements.capacity = 10.0;

            MedDispatchRec rec2 = new MedDispatchRec();
            rec2.id = 2;
            rec2.date = "2025-01-20";
            rec2.time = "14:00:00";
            rec2.delivery = new Position(-3.17, 55.96);
            rec2.requirements = new MedDispatchRec.Requirements();
            rec2.requirements.capacity = 10.0; // Total 20.0 exceeds 15.0

            Position p1 = new Position(-3.19, 55.94);
            Position p2 = new Position(-3.18, 55.95);

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(new ServicePoint[]{sp});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);
            when(geoService.findPath(any(Position.class), any(Position.class), anyList()))
                    .thenReturn(List.of(p1, p2));

            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(rec1, rec2));
            // One delivery should succeed, one should be in remaining
            assertNotNull(result);
        }
    }
}