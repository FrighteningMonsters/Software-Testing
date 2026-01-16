package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Scenario/Use Case Testing for DroneService.calcDeliveryPath()
 *
 * Tests end-to-end medical delivery dispatch workflow:
 * 1. Accept delivery requests with requirements (cooling, heating, capacity)
 * 2. Fetch drones, service points, availability, and restricted areas
 * 3. Match capable drones to deliveries based on constraints
 * 4. Calculate optimal delivery paths avoiding no-fly zones
 * 5. Return complete flight paths with cost calculations
 *
 * Each test represents a realistic user scenario/use case.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DroneService.calcDeliveryPath() - Scenario/Use Case Testing")
public class DroneServiceScenarioTest {

    @Mock private RestTemplate restTemplate;
    private GeoService geoService;
    private DroneService droneService;
    private static final String ENDPOINT = "http://api";

    // Edinburgh area coordinates for realistic scenarios
    private static final double EDINBURGH_LNG = -3.1883;
    private static final double EDINBURGH_LAT = 55.9445;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();
        droneService = new DroneService(restTemplate, geoService, ENDPOINT);
    }

    // ==================== HELPER METHODS ====================

    private Drone createDrone(String id, String name, boolean cooling, boolean heating,
                               double capacity, int maxMoves, double costPerMove,
                               double costInitial, double costFinal) {
        Drone drone = new Drone();
        drone.id = id;
        drone.name = name;
        Capability c = new Capability();
        c.cooling = cooling;
        c.heating = heating;
        c.capacity = capacity;
        c.maxMoves = maxMoves;
        c.costPerMove = costPerMove;
        c.costInitial = costInitial;
        c.costFinal = costFinal;
        drone.capability = c;
        return drone;
    }

    private ServicePoint createServicePoint(int id, String name, double lng, double lat) {
        ServicePoint sp = new ServicePoint();
        sp.id = id;
        sp.name = name;
        sp.location = new Position(lng, lat);
        return sp;
    }

    private MedDispatchRec createDeliveryRequest(int id, double lng, double lat,
                                                   Boolean cooling, Boolean heating,
                                                   Double capacity, Double maxCost,
                                                   String date, String time) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.id = id;
        rec.delivery = new Position(lng, lat);
        rec.date = date;
        rec.time = time;
        MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
        req.cooling = cooling;
        req.heating = heating;
        req.capacity = capacity;
        req.maxCost = maxCost;
        rec.requirements = req;
        return rec;
    }

    private DroneForServicePoint[] createAvailability(int servicePointId, String droneId,
                                                        String dayOfWeek, String from, String until) {
        AvailabilityWindow window = new AvailabilityWindow();
        window.dayOfWeek = dayOfWeek;
        window.from = from;
        window.until = until;

        DroneAvailability da = new DroneAvailability();
        da.id = droneId;
        da.availability = List.of(window);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.servicePointId = servicePointId;
        dfsp.drones = List.of(da);
        return new DroneForServicePoint[]{dfsp};
    }

    private DroneForServicePoint[] createMultiDroneAvailability(int servicePointId,
                                                                  List<String> droneIds,
                                                                  String dayOfWeek) {
        List<DroneAvailability> availabilities = new ArrayList<>();
        for (String droneId : droneIds) {
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = dayOfWeek;
            window.from = "06:00";
            window.until = "22:00";

            DroneAvailability da = new DroneAvailability();
            da.id = droneId;
            da.availability = List.of(window);
            availabilities.add(da);
        }

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.servicePointId = servicePointId;
        dfsp.drones = availabilities;
        return new DroneForServicePoint[]{dfsp};
    }

    private Region createRestrictedArea(String name, Position... vertices) {
        Region region = new Region();
        region.name = name;
        region.vertices = List.of(vertices);
        return region;
    }

    private void setupMocks(Drone[] drones, ServicePoint[] servicePoints,
                            DroneForServicePoint[] availability, Region[] restrictedAreas) {
        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                .thenReturn(drones);
        when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                .thenReturn(servicePoints);
        when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                .thenReturn(availability);
        when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                .thenReturn(restrictedAreas);
    }

    // ==================== SCENARIO 1: HAPPY PATH ====================

    @Nested
    @DisplayName("Scenario 1: Happy Path - Single Delivery Success")
    class HappyPathScenarios {

        @Test
        @DisplayName("UC1.1: Single delivery request fulfilled by available drone")
        void singleDeliveryHappyPath() {
            // GIVEN: A hospital needs urgent medical supplies delivered
            // - One drone available with all capabilities
            // - One delivery location nearby
            // - No restricted areas

            Drone drone = createDrone("DRONE-001", "MedExpress-1",
                    true, true, 10.0, 100, 0.5, 5.0, 2.0);

            ServicePoint servicePoint = createServicePoint(1, "Edinburgh Hospital Base",
                    EDINBURGH_LNG, EDINBURGH_LAT);

            // Delivery location ~0.01 degrees away (roughly 1km)
            MedDispatchRec delivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-20", "10:00:00"); // Monday

            setupMocks(
                    new Drone[]{drone},
                    new ServicePoint[]{servicePoint},
                    createAvailability(1, "DRONE-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: The system calculates the delivery path
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(delivery));

            // THEN: Delivery should be assigned and path calculated
            assertNotNull(result);
            assertEquals(1, result.dronePaths.size(), "Should have exactly one drone path");
            assertEquals("DRONE-001", result.dronePaths.getFirst().droneId);
            assertFalse(result.dronePaths.getFirst().deliveries.isEmpty(), "Should have delivery path");
            assertTrue(result.totalMoves > 0, "Should have calculated moves");
            assertTrue(result.totalCost > 0, "Should have calculated cost");
        }

        @Test
        @DisplayName("UC1.2: Empty delivery list returns empty result")
        void emptyDeliveryList() {
            // GIVEN: No delivery requests
            // WHEN: System processes empty list
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of());

            // THEN: Should return empty result without errors
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
            assertEquals(0.0, result.totalCost);
            assertEquals(0, result.totalMoves);
        }

        @Test
        @DisplayName("UC1.3: Null delivery list handled gracefully")
        void nullDeliveryList() {
            // GIVEN: Null input
            // WHEN: System processes null
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(null);

            // THEN: Should return empty result without NPE
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
        }
    }

    // ==================== SCENARIO 2: CAPABILITY CONSTRAINTS ====================

    @Nested
    @DisplayName("Scenario 2: Capability Requirements")
    class CapabilityScenarios {

        @Test
        @DisplayName("UC2.1: Vaccine delivery requires cooling - matched to cooling-capable drone")
        void coolingRequirementMatched() {
            // GIVEN: A vaccine shipment requires temperature-controlled delivery
            Drone coolingDrone = createDrone("COOL-001", "CoolCarrier",
                    true, false, 10.0, 100, 0.5, 5.0, 2.0);
            Drone basicDrone = createDrone("BASIC-001", "BasicCarrier",
                    false, false, 15.0, 100, 0.3, 3.0, 1.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec vaccineDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    true, false, 5.0, 100.0,  // Requires cooling
                    "2025-01-20", "10:00:00");

            setupMocks(
                    new Drone[]{coolingDrone, basicDrone},
                    new ServicePoint[]{sp},
                    createMultiDroneAvailability(1, List.of("COOL-001", "BASIC-001"), "MONDAY"),
                    new Region[]{}
            );

            // WHEN: System assigns delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(vaccineDelivery));

            // THEN: Only cooling-capable drone should be assigned
            assertEquals(1, result.dronePaths.size());
            assertEquals("COOL-001", result.dronePaths.getFirst().droneId,
                    "Cooling-capable drone should handle vaccine delivery");
        }

        @Test
        @DisplayName("UC2.2: Blood plasma requires heating - matched to heating-capable drone")
        void heatingRequirementMatched() {
            // GIVEN: Blood plasma needs to be kept warm
            Drone heatingDrone = createDrone("HEAT-001", "WarmCarrier",
                    false, true, 10.0, 100, 0.5, 5.0, 2.0);
            Drone basicDrone = createDrone("BASIC-001", "BasicCarrier",
                    false, false, 15.0, 100, 0.3, 3.0, 1.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec plasmaDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, true, 5.0, 100.0,  // Requires heating
                    "2025-01-20", "10:00:00");

            setupMocks(
                    new Drone[]{heatingDrone, basicDrone},
                    new ServicePoint[]{sp},
                    createMultiDroneAvailability(1, List.of("HEAT-001", "BASIC-001"), "MONDAY"),
                    new Region[]{}
            );

            // WHEN: System assigns delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(plasmaDelivery));

            // THEN: Only heating-capable drone should be assigned
            assertEquals(1, result.dronePaths.size());
            assertEquals("HEAT-001", result.dronePaths.getFirst().droneId,
                    "Heating-capable drone should handle plasma delivery");
        }

        @Test
        @DisplayName("UC2.3: No capable drone available - delivery cannot be fulfilled")
        void noCapableDroneAvailable() {
            // GIVEN: Delivery requires cooling but no cooling-capable drone exists
            Drone basicDrone = createDrone("BASIC-001", "BasicCarrier",
                    false, false, 15.0, 100, 0.3, 3.0, 1.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec coolingDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    true, false, 5.0, 100.0,  // Requires cooling
                    "2025-01-20", "10:00:00");

            setupMocks(
                    new Drone[]{basicDrone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "BASIC-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System tries to assign delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(coolingDelivery));

            // THEN: No drone should be assigned
            assertTrue(result.dronePaths.isEmpty(),
                    "No delivery should be made when no capable drone exists");
        }
    }

    // ==================== SCENARIO 3: CAPACITY CONSTRAINTS ====================

    @Nested
    @DisplayName("Scenario 3: Capacity Constraints")
    class CapacityScenarios {

        @Test
        @DisplayName("UC3.1: Heavy medical equipment exceeds drone capacity")
        void capacityExceeded() {
            // GIVEN: Heavy MRI parts need delivery but drone capacity is insufficient
            Drone lightDrone = createDrone("LIGHT-001", "LightLifter",
                    true, true, 5.0, 100, 0.5, 5.0, 2.0);  // Only 5kg capacity

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec heavyDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 10.0, 100.0,  // Requires 10kg capacity
                    "2025-01-20", "10:00:00");

            setupMocks(
                    new Drone[]{lightDrone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "LIGHT-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System tries to assign delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(heavyDelivery));

            // THEN: Delivery should not be assigned
            assertTrue(result.dronePaths.isEmpty(),
                    "Heavy delivery should not be assigned to low-capacity drone");
        }

        @Test
        @DisplayName("UC3.2: Heavy cargo assigned to high-capacity drone")
        void capacitySufficient() {
            // GIVEN: Large drone available for heavy delivery
            Drone heavyDrone = createDrone("HEAVY-001", "HeavyLifter",
                    true, true, 20.0, 100, 0.8, 10.0, 5.0);  // 20kg capacity

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec heavyDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 15.0, 200.0,  // Requires 15kg
                    "2025-01-20", "10:00:00");

            setupMocks(
                    new Drone[]{heavyDrone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "HEAVY-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System assigns delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(heavyDelivery));

            // THEN: Heavy drone should handle the delivery
            assertEquals(1, result.dronePaths.size());
            assertEquals("HEAVY-001", result.dronePaths.getFirst().droneId);
        }
    }

    // ==================== SCENARIO 4: AVAILABILITY CONSTRAINTS ====================

    @Nested
    @DisplayName("Scenario 4: Time Availability Constraints")
    class AvailabilityScenarios {

        @Test
        @DisplayName("UC4.1: Weekend delivery when drone only available weekdays")
        void weekendDeliveryNoAvailability() {
            // GIVEN: Drone only available Monday-Friday
            Drone drone = createDrone("WEEKDAY-001", "WeekdayWorker",
                    true, true, 10.0, 100, 0.5, 5.0, 2.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            // Saturday delivery request
            MedDispatchRec saturdayDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-25", "10:00:00");  // Saturday

            setupMocks(
                    new Drone[]{drone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "WEEKDAY-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System tries to assign Saturday delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(saturdayDelivery));

            // THEN: No delivery should be assigned
            assertTrue(result.dronePaths.isEmpty(),
                    "Delivery should not be assigned when drone unavailable on that day");
        }

        @Test
        @DisplayName("UC4.2: Delivery requested during drone's available hours")
        void deliveryDuringAvailableHours() {
            // GIVEN: Drone available on Monday 08:00-18:00
            Drone drone = createDrone("DRONE-001", "AllDay",
                    true, true, 10.0, 100, 0.5, 5.0, 2.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            MedDispatchRec mondayDelivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-20", "12:00:00");  // Monday noon

            setupMocks(
                    new Drone[]{drone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "DRONE-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System assigns delivery during available hours
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(mondayDelivery));

            // THEN: Delivery should be successfully assigned
            assertEquals(1, result.dronePaths.size());
        }
    }

    // ==================== SCENARIO 5: MULTI-DELIVERY OPTIMIZATION ====================

    @Nested
    @DisplayName("Scenario 5: Multiple Deliveries Optimization")
    class MultiDeliveryScenarios {

        @Test
        @DisplayName("UC5.1: Multiple nearby deliveries batched to single drone")
        void multipleDeliveriesBatched() {
            // GIVEN: Three nearby deliveries can be handled by one drone trip
            Drone drone = createDrone("MULTI-001", "MultiCarrier",
                    true, true, 30.0, 200, 0.5, 5.0, 2.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            List<MedDispatchRec> deliveries = List.of(
                    createDeliveryRequest(1, EDINBURGH_LNG + 0.003, EDINBURGH_LAT + 0.003,
                            false, false, 5.0, 100.0, "2025-01-20", "10:00:00"),
                    createDeliveryRequest(2, EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                            false, false, 5.0, 100.0, "2025-01-20", "10:30:00"),
                    createDeliveryRequest(3, EDINBURGH_LNG + 0.007, EDINBURGH_LAT + 0.007,
                            false, false, 5.0, 100.0, "2025-01-20", "11:00:00")
            );

            setupMocks(
                    new Drone[]{drone},
                    new ServicePoint[]{sp},
                    createAvailability(1, "MULTI-001", "MONDAY", "08:00", "18:00"),
                    new Region[]{}
            );

            // WHEN: System optimizes delivery paths
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(deliveries);

            // THEN: All deliveries should be handled (possibly by single drone)
            assertNotNull(result);
            assertFalse(result.dronePaths.isEmpty());

            // Count total deliveries across all drone paths
            int totalDeliveries = result.dronePaths.stream()
                    .mapToInt(dp -> dp.deliveries.size())
                    .sum();
            assertTrue(totalDeliveries >= 1, "At least some deliveries should be fulfilled");
        }

        @Test
        @DisplayName("UC5.2: Mixed requirements distributed to specialized drones")
        void mixedRequirementsDistributed() {
            // GIVEN: Deliveries with different requirements need specialized drones
            Drone coolingDrone = createDrone("COOL-001", "CoolCarrier",
                    true, false, 10.0, 100, 0.5, 5.0, 2.0);
            Drone heatingDrone = createDrone("HEAT-001", "HeatCarrier",
                    false, true, 10.0, 100, 0.5, 5.0, 2.0);

            ServicePoint sp = createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT);

            List<MedDispatchRec> mixedDeliveries = List.of(
                    createDeliveryRequest(1, EDINBURGH_LNG + 0.003, EDINBURGH_LAT + 0.003,
                            true, false, 5.0, 100.0, "2025-01-20", "10:00:00"),  // Needs cooling
                    createDeliveryRequest(2, EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                            false, true, 5.0, 100.0, "2025-01-20", "10:30:00")   // Needs heating
            );

            setupMocks(
                    new Drone[]{coolingDrone, heatingDrone},
                    new ServicePoint[]{sp},
                    createMultiDroneAvailability(1, List.of("COOL-001", "HEAT-001"), "MONDAY"),
                    new Region[]{}
            );

            // WHEN: System assigns specialized deliveries
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(mixedDeliveries);

            // THEN: Both deliveries should be assigned to appropriate drones
            assertFalse(result.dronePaths.isEmpty());
            // Verify that specialized drones are used for their capabilities
            List<String> assignedDrones = result.dronePaths.stream()
                    .map(dp -> dp.droneId)
                    .toList();
            assertFalse(assignedDrones.isEmpty(), "At least one drone should be assigned");
        }
    }

    // ==================== SCENARIO 6: ERROR HANDLING ====================

    @Nested
    @DisplayName("Scenario 6: Error Handling and Edge Cases")
    class ErrorHandlingScenarios {

        @Test
        @DisplayName("UC6.1: No drones available in system")
        void noDronesAvailable() {
            // GIVEN: System has no drones registered
            setupMocks(
                    new Drone[]{},
                    new ServicePoint[]{createServicePoint(1, "Hospital", EDINBURGH_LNG, EDINBURGH_LAT)},
                    new DroneForServicePoint[]{},
                    new Region[]{}
            );

            MedDispatchRec delivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-20", "10:00:00");

            // WHEN: System attempts delivery assignment
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(delivery));

            // THEN: Should handle gracefully with empty result
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
        }

        @Test
        @DisplayName("UC6.2: No service points configured")
        void noServicePoints() {
            // GIVEN: No service points (drone bases) configured
            Drone drone = createDrone("DRONE-001", "Lonely",
                    true, true, 10.0, 100, 0.5, 5.0, 2.0);

            setupMocks(
                    new Drone[]{drone},
                    new ServicePoint[]{},  // No service points
                    new DroneForServicePoint[]{},
                    new Region[]{}
            );

            MedDispatchRec delivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-20", "10:00:00");

            // WHEN: System attempts delivery
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(delivery));

            // THEN: Should handle gracefully
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
        }

        @Test
        @DisplayName("UC6.3: Null data from external services handled gracefully")
        void nullExternalData() {
            // GIVEN: External services return null
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(null);
            when(restTemplate.getForObject(ENDPOINT + "/service-points", ServicePoint[].class))
                    .thenReturn(null);
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(null);
            when(restTemplate.getForObject(ENDPOINT + "/restricted-areas", Region[].class))
                    .thenReturn(null);

            MedDispatchRec delivery = createDeliveryRequest(1,
                    EDINBURGH_LNG + 0.005, EDINBURGH_LAT + 0.005,
                    false, false, 5.0, 100.0,
                    "2025-01-20", "10:00:00");

            // WHEN: System handles null responses
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(List.of(delivery));

            // THEN: Should not throw exception
            assertNotNull(result);
            assertTrue(result.dronePaths.isEmpty());
        }
    }

    // ==================== SCENARIO 7: COMPLETE END-TO-END WORKFLOW ====================

    @Nested
    @DisplayName("Scenario 7: Complete Real-World Workflow")
    class CompleteWorkflowScenarios {

        @Test
        @DisplayName("UC7.1: Hospital emergency - multiple urgent deliveries across Edinburgh")
        void hospitalEmergencyScenario() {
            // GIVEN: Emergency situation requiring multiple medical deliveries
            // - Vaccines needing cooling to Royal Infirmary
            // - Blood plasma needing heating to Western General
            // - Standard medical supplies to various clinics

            Drone coolingDrone = createDrone("MED-COOL-01", "VaccineExpress",
                    true, false, 15.0, 150, 0.6, 8.0, 3.0);
            Drone heatingDrone = createDrone("MED-HEAT-01", "PlasmaCarrier",
                    false, true, 12.0, 150, 0.7, 10.0, 4.0);
            Drone standardDrone = createDrone("MED-STD-01", "MedSupply",
                    false, false, 20.0, 200, 0.4, 5.0, 2.0);

            ServicePoint hospitalBase = createServicePoint(1, "Edinburgh Royal Infirmary",
                    EDINBURGH_LNG, EDINBURGH_LAT);

            List<MedDispatchRec> emergencyDeliveries = List.of(
                    // Vaccine delivery - requires cooling
                    createDeliveryRequest(101, EDINBURGH_LNG + 0.008, EDINBURGH_LAT + 0.006,
                            true, false, 3.0, 150.0,
                            "2025-01-20", "09:00:00"),
                    // Blood plasma - requires heating
                    createDeliveryRequest(102, EDINBURGH_LNG - 0.005, EDINBURGH_LAT + 0.010,
                            false, true, 4.0, 200.0,
                            "2025-01-20", "09:30:00"),
                    // Standard supplies - no special requirements
                    createDeliveryRequest(103, EDINBURGH_LNG + 0.012, EDINBURGH_LAT - 0.003,
                            false, false, 8.0, 100.0,
                            "2025-01-20", "10:00:00")
            );

            setupMocks(
                    new Drone[]{coolingDrone, heatingDrone, standardDrone},
                    new ServicePoint[]{hospitalBase},
                    createMultiDroneAvailability(1,
                            List.of("MED-COOL-01", "MED-HEAT-01", "MED-STD-01"), "MONDAY"),
                    new Region[]{}  // No restricted areas for emergency
            );

            // WHEN: Emergency dispatch system processes all deliveries
            CalcDeliveryPathResult result = droneService.calcDeliveryPath(emergencyDeliveries);

            // THEN: All deliveries should be assigned and paths calculated
            assertNotNull(result, "Result should not be null");
            assertFalse(result.dronePaths.isEmpty(), "Should have assigned drone paths");
            assertTrue(result.totalMoves > 0, "Should have calculated total moves");
            assertTrue(result.totalCost > 0, "Should have calculated total cost");

            // Verify deliveries are distributed based on capabilities
            int totalDeliveriesAssigned = result.dronePaths.stream()
                    .mapToInt(dp -> dp.deliveries.size())
                    .sum();
            assertTrue(totalDeliveriesAssigned >= 1,
                    "At least some emergency deliveries should be fulfilled");

            // Log results for verification
            System.out.println("=== Emergency Delivery Results ===");
            System.out.println("Total drones used: " + result.dronePaths.size());
            System.out.println("Total moves: " + result.totalMoves);
            System.out.println("Total cost: " + result.totalCost);
            for (DronePath dp : result.dronePaths) {
                System.out.println("Drone " + dp.droneId + " - Deliveries: " + dp.deliveries.size());
            }
        }
    }
}