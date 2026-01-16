package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Combinatorial Testing for DroneService.canServe() logic via queryAvailableDrones().
 *
 * This test class demonstrates multiple combinatorial testing techniques:
 * - All-Combinations: Tests every possible combination of parameter values
 * - Pair-wise: Ensures every pair of parameter values appears at least once
 * - Each-Choice: Every parameter value appears in at least one test case
 * - Base-Choice: Select base values and vary one parameter at a time
 *
 * Parameters under test (for canServe logic):
 * 1. drone.capability.cooling: true, false
 * 2. drone.capability.heating: true, false
 * 3. req.cooling: null, false, true
 * 4. req.heating: null, false, true
 * 5. req.capacity comparison: null, less than drone, equal, greater than drone
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DroneService.canServe() - Combinatorial Testing")
public class DroneServiceCombinatorialTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService geoService;
    private DroneService droneService;
    private static final String ENDPOINT = "http://api";
    private static final double DRONE_CAPACITY = 50.0;

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, geoService, ENDPOINT);
    }

    // ==================== HELPER METHODS ====================

    private Drone createDrone(String id, boolean cooling, boolean heating, double capacity) {
        Drone drone = new Drone();
        drone.id = id;
        Capability c = new Capability();
        c.cooling = cooling;
        c.heating = heating;
        c.capacity = capacity;
        drone.capability = c;
        return drone;
    }

    private Drone createDroneWithNullCapability(String id) {
        Drone drone = new Drone();
        drone.id = id;
        drone.capability = null;
        return drone;
    }

    private MedDispatchRec createRecord(Boolean cooling, Boolean heating, Double capacity) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.id = 1;
        rec.date = "2025-01-20"; // Monday
        rec.time = "12:00:00";
        MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
        req.cooling = cooling;
        req.heating = heating;
        req.capacity = capacity;
        rec.requirements = req;
        return rec;
    }

    private MedDispatchRec createRecordWithNullRequirements() {
        MedDispatchRec rec = new MedDispatchRec();
        rec.id = 1;
        rec.date = "2025-01-20";
        rec.time = "12:00:00";
        rec.requirements = null;
        return rec;
    }

    private DroneForServicePoint[] createAvailability(String droneId) {
        AvailabilityWindow window = new AvailabilityWindow();
        window.dayOfWeek = "MONDAY";
        window.from = "08:00";
        window.until = "18:00";

        DroneAvailability da = new DroneAvailability();
        da.id = droneId;
        da.availability = List.of(window);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.drones = List.of(da);
        return new DroneForServicePoint[]{dfsp};
    }

    private boolean canServe(Drone drone, MedDispatchRec rec) {
        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                .thenReturn(new Drone[]{drone});
        when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                .thenReturn(createAvailability(drone.id));

        List<String> result = droneService.queryAvailableDrones(List.of(rec));
        return result.contains(drone.id);
    }

    // ==================== ALL-COMBINATIONS TESTING ====================

    @Nested
    @DisplayName("All-Combinations Testing")
    class AllCombinationsTesting {

        /**
         * All-Combinations test matrix for canServe() logic.
         *
         * Parameters:
         * - droneCooling: true, false (2)
         * - droneHeating: true, false (2)
         * - reqCooling: null, false, true (3)
         * - reqHeating: null, false, true (3)
         * - capacityRelation: null, less, equal, greater (4)
         *
         * Total: 2 x 2 x 3 x 3 x 4 = 144 combinations
         */
        static Stream<Arguments> allCombinationsProvider() {
            return Stream.of(
                // Format: droneCooling, droneHeating, reqCooling, reqHeating, reqCapacity, expected

                // I generated these with python

                // === DRONE: cooling=true, heating=true ===
                Arguments.of(true, true, null, null, null, true),
                Arguments.of(true, true, null, null, 40.0, true),
                Arguments.of(true, true, null, null, 50.0, true),
                Arguments.of(true, true, null, null, 60.0, false),
                Arguments.of(true, true, null, false, null, true),
                Arguments.of(true, true, null, false, 40.0, true),
                Arguments.of(true, true, null, false, 50.0, true),
                Arguments.of(true, true, null, false, 60.0, false),
                Arguments.of(true, true, null, true, null, true),
                Arguments.of(true, true, null, true, 40.0, true),
                Arguments.of(true, true, null, true, 50.0, true),
                Arguments.of(true, true, null, true, 60.0, false),
                Arguments.of(true, true, false, null, null, true),
                Arguments.of(true, true, false, null, 40.0, true),
                Arguments.of(true, true, false, null, 50.0, true),
                Arguments.of(true, true, false, null, 60.0, false),
                Arguments.of(true, true, false, false, null, true),
                Arguments.of(true, true, false, false, 40.0, true),
                Arguments.of(true, true, false, false, 50.0, true),
                Arguments.of(true, true, false, false, 60.0, false),
                Arguments.of(true, true, false, true, null, true),
                Arguments.of(true, true, false, true, 40.0, true),
                Arguments.of(true, true, false, true, 50.0, true),
                Arguments.of(true, true, false, true, 60.0, false),
                Arguments.of(true, true, true, null, null, true),
                Arguments.of(true, true, true, null, 40.0, true),
                Arguments.of(true, true, true, null, 50.0, true),
                Arguments.of(true, true, true, null, 60.0, false),
                Arguments.of(true, true, true, false, null, true),
                Arguments.of(true, true, true, false, 40.0, true),
                Arguments.of(true, true, true, false, 50.0, true),
                Arguments.of(true, true, true, false, 60.0, false),
                Arguments.of(true, true, true, true, null, true),
                Arguments.of(true, true, true, true, 40.0, true),
                Arguments.of(true, true, true, true, 50.0, true),
                Arguments.of(true, true, true, true, 60.0, false),

                // === DRONE: cooling=true, heating=false ===
                Arguments.of(true, false, null, null, null, true),
                Arguments.of(true, false, null, null, 40.0, true),
                Arguments.of(true, false, null, null, 50.0, true),
                Arguments.of(true, false, null, null, 60.0, false),
                Arguments.of(true, false, null, false, null, true),
                Arguments.of(true, false, null, false, 40.0, true),
                Arguments.of(true, false, null, false, 50.0, true),
                Arguments.of(true, false, null, false, 60.0, false),
                Arguments.of(true, false, null, true, null, false),
                Arguments.of(true, false, null, true, 40.0, false),
                Arguments.of(true, false, null, true, 50.0, false),
                Arguments.of(true, false, null, true, 60.0, false),
                Arguments.of(true, false, false, null, null, true),
                Arguments.of(true, false, false, null, 40.0, true),
                Arguments.of(true, false, false, null, 50.0, true),
                Arguments.of(true, false, false, null, 60.0, false),
                Arguments.of(true, false, false, false, null, true),
                Arguments.of(true, false, false, false, 40.0, true),
                Arguments.of(true, false, false, false, 50.0, true),
                Arguments.of(true, false, false, false, 60.0, false),
                Arguments.of(true, false, false, true, null, false),
                Arguments.of(true, false, false, true, 40.0, false),
                Arguments.of(true, false, false, true, 50.0, false),
                Arguments.of(true, false, false, true, 60.0, false),
                Arguments.of(true, false, true, null, null, true),
                Arguments.of(true, false, true, null, 40.0, true),
                Arguments.of(true, false, true, null, 50.0, true),
                Arguments.of(true, false, true, null, 60.0, false),
                Arguments.of(true, false, true, false, null, true),
                Arguments.of(true, false, true, false, 40.0, true),
                Arguments.of(true, false, true, false, 50.0, true),
                Arguments.of(true, false, true, false, 60.0, false),
                Arguments.of(true, false, true, true, null, false),
                Arguments.of(true, false, true, true, 40.0, false),
                Arguments.of(true, false, true, true, 50.0, false),
                Arguments.of(true, false, true, true, 60.0, false),

                // === DRONE: cooling=false, heating=true ===
                Arguments.of(false, true, null, null, null, true),
                Arguments.of(false, true, null, null, 40.0, true),
                Arguments.of(false, true, null, null, 50.0, true),
                Arguments.of(false, true, null, null, 60.0, false),
                Arguments.of(false, true, null, false, null, true),
                Arguments.of(false, true, null, false, 40.0, true),
                Arguments.of(false, true, null, false, 50.0, true),
                Arguments.of(false, true, null, false, 60.0, false),
                Arguments.of(false, true, null, true, null, true),
                Arguments.of(false, true, null, true, 40.0, true),
                Arguments.of(false, true, null, true, 50.0, true),
                Arguments.of(false, true, null, true, 60.0, false),
                Arguments.of(false, true, false, null, null, true),
                Arguments.of(false, true, false, null, 40.0, true),
                Arguments.of(false, true, false, null, 50.0, true),
                Arguments.of(false, true, false, null, 60.0, false),
                Arguments.of(false, true, false, false, null, true),
                Arguments.of(false, true, false, false, 40.0, true),
                Arguments.of(false, true, false, false, 50.0, true),
                Arguments.of(false, true, false, false, 60.0, false),
                Arguments.of(false, true, false, true, null, true),
                Arguments.of(false, true, false, true, 40.0, true),
                Arguments.of(false, true, false, true, 50.0, true),
                Arguments.of(false, true, false, true, 60.0, false),
                Arguments.of(false, true, true, null, null, false),
                Arguments.of(false, true, true, null, 40.0, false),
                Arguments.of(false, true, true, null, 50.0, false),
                Arguments.of(false, true, true, null, 60.0, false),
                Arguments.of(false, true, true, false, null, false),
                Arguments.of(false, true, true, false, 40.0, false),
                Arguments.of(false, true, true, false, 50.0, false),
                Arguments.of(false, true, true, false, 60.0, false),
                Arguments.of(false, true, true, true, null, false),
                Arguments.of(false, true, true, true, 40.0, false),
                Arguments.of(false, true, true, true, 50.0, false),
                Arguments.of(false, true, true, true, 60.0, false),

                // === DRONE: cooling=false, heating=false ===
                Arguments.of(false, false, null, null, null, true),
                Arguments.of(false, false, null, null, 40.0, true),
                Arguments.of(false, false, null, null, 50.0, true),
                Arguments.of(false, false, null, null, 60.0, false),
                Arguments.of(false, false, null, false, null, true),
                Arguments.of(false, false, null, false, 40.0, true),
                Arguments.of(false, false, null, false, 50.0, true),
                Arguments.of(false, false, null, false, 60.0, false),
                Arguments.of(false, false, null, true, null, false),
                Arguments.of(false, false, null, true, 40.0, false),
                Arguments.of(false, false, null, true, 50.0, false),
                Arguments.of(false, false, null, true, 60.0, false),
                Arguments.of(false, false, false, null, null, true),
                Arguments.of(false, false, false, null, 40.0, true),
                Arguments.of(false, false, false, null, 50.0, true),
                Arguments.of(false, false, false, null, 60.0, false),
                Arguments.of(false, false, false, false, null, true),
                Arguments.of(false, false, false, false, 40.0, true),
                Arguments.of(false, false, false, false, 50.0, true),
                Arguments.of(false, false, false, false, 60.0, false),
                Arguments.of(false, false, false, true, null, false),
                Arguments.of(false, false, false, true, 40.0, false),
                Arguments.of(false, false, false, true, 50.0, false),
                Arguments.of(false, false, false, true, 60.0, false),
                Arguments.of(false, false, true, null, null, false),
                Arguments.of(false, false, true, null, 40.0, false),
                Arguments.of(false, false, true, null, 50.0, false),
                Arguments.of(false, false, true, null, 60.0, false),
                Arguments.of(false, false, true, false, null, false),
                Arguments.of(false, false, true, false, 40.0, false),
                Arguments.of(false, false, true, false, 50.0, false),
                Arguments.of(false, false, true, false, 60.0, false),
                Arguments.of(false, false, true, true, null, false),
                Arguments.of(false, false, true, true, 40.0, false),
                Arguments.of(false, false, true, true, 50.0, false),
                Arguments.of(false, false, true, true, 60.0, false)
            );
        }

        @ParameterizedTest(name = "drone[cool={0},heat={1}] + req[cool={2},heat={3},cap={4}] => {5}")
        @MethodSource("allCombinationsProvider")
        @DisplayName("All-Combinations: Full parameter matrix (144 cases)")
        void testAllCombinations(boolean droneCooling, boolean droneHeating,
                                  Boolean reqCooling, Boolean reqHeating, Double reqCapacity,
                                  boolean expected) {
            Drone drone = createDrone("D1", droneCooling, droneHeating, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(reqCooling, reqHeating, reqCapacity);

            boolean result = canServe(drone, rec);
            assertEquals(expected, result,
                String.format("canServe failed for drone[cool=%s,heat=%s] + req[cool=%s,heat=%s,cap=%s]",
                    droneCooling, droneHeating, reqCooling, reqHeating, reqCapacity));
        }

        @Test
        @DisplayName("All-Combinations: Null capability always returns false")
        void testNullCapability() {
            Drone drone = createDroneWithNullCapability("D1");
            MedDispatchRec rec = createRecord(null, null, null);
            assertFalse(canServe(drone, rec));
        }

        @Test
        @DisplayName("All-Combinations: Null requirements always returns false")
        void testNullRequirements() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecordWithNullRequirements();
            assertFalse(canServe(drone, rec));
        }
    }

    // ==================== PAIR-WISE TESTING ====================

    @Nested
    @DisplayName("Pair-wise Testing")
    class PairwiseTesting {

        /**
         * Pair-wise test cases: every pair of parameter values appears at least once.
         *
         * Full combinations = 2 x 2 x 3 x 3 x 4 = 144
         * Pair-wise reduces to ~20 test cases while covering all pairs.
         */
        static Stream<Arguments> pairwiseProvider() {
            return Stream.of(
                // Pair-wise covering array
                Arguments.of(true,  true,  null,  null,  null, true),
                Arguments.of(true,  false, false, false, 40.0, true),
                Arguments.of(true,  true,  true,  true,  50.0, true),
                Arguments.of(false, true,  null,  false, 60.0, false),
                Arguments.of(false, false, false, null,  null, true),
                Arguments.of(false, true,  true,  null,  40.0, false),
                Arguments.of(true,  false, null,  true,  50.0, false),
                Arguments.of(false, false, true,  false, 60.0, false),
                Arguments.of(true,  true,  false, null,  40.0, true),
                Arguments.of(false, false, null,  true,  null, false),
                Arguments.of(true,  false, true,  false, null, true),
                Arguments.of(false, true,  false, true,  50.0, true),
                Arguments.of(true,  true,  null,  false, 60.0, false),
                Arguments.of(false, false, false, false, 40.0, true),
                Arguments.of(true,  false, false, null,  50.0, true),
                Arguments.of(false, true,  null,  null,  40.0, true),
                Arguments.of(true,  true,  true,  false, null, true),
                Arguments.of(false, false, null,  false, 50.0, true),
                Arguments.of(true,  false, null,  null,  60.0, false),
                Arguments.of(false, true,  false, null,  null, true)
            );
        }

        @ParameterizedTest(name = "TC{index}: drone[cool={0},heat={1}] + req[cool={2},heat={3},cap={4}] => {5}")
        @MethodSource("pairwiseProvider")
        @DisplayName("Pair-wise: Covering array for all parameter pairs (20 cases)")
        void testPairwise(boolean droneCooling, boolean droneHeating,
                          Boolean reqCooling, Boolean reqHeating, Double reqCapacity,
                          boolean expected) {
            Drone drone = createDrone("D1", droneCooling, droneHeating, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(reqCooling, reqHeating, reqCapacity);

            boolean result = canServe(drone, rec);
            assertEquals(expected, result);
        }
    }

    // ==================== EACH-CHOICE TESTING ====================

    @Nested
    @DisplayName("Each-Choice Testing")
    class EachChoiceTesting {

        /**
         * Each-Choice: Every value of every parameter appears at least once.
         *
         * Minimum test cases = max(values per parameter) = 4
         */

        @Test
        @DisplayName("Each-Choice TC1: All TRUE/supported values")
        void testEachChoice_TC1() {
            // Covers: droneCooling=true, droneHeating=true, reqCooling=true, reqHeating=true, capacity=null
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(true, true, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Each-Choice TC2: All FALSE/unsupported values")
        void testEachChoice_TC2() {
            // Covers: droneCooling=false, droneHeating=false, reqCooling=false, reqHeating=false, capacity=less
            Drone drone = createDrone("D1", false, false, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(false, false, 40.0);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Each-Choice TC3: Null requirements with equal capacity")
        void testEachChoice_TC3() {
            // Covers: reqCooling=null, reqHeating=null, capacity=equal
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, 50.0);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Each-Choice TC4: Capacity greater than drone")
        void testEachChoice_TC4() {
            // Covers: capacity=greater (60.0 > 50.0)
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, 60.0);
            assertFalse(canServe(drone, rec));
        }
    }

    // ==================== BASE-CHOICE TESTING ====================

    @Nested
    @DisplayName("Base-Choice Testing")
    class BaseChoiceTesting {

        /**
         * Base-Choice: Select base values, then vary one parameter at a time.
         *
         * Base case:
         * - droneCooling = true
         * - droneHeating = true
         * - reqCooling = null (no requirement)
         * - reqHeating = null (no requirement)
         * - reqCapacity = null (no requirement)
         */

        @Test
        @DisplayName("Base Case: All base values (fully capable drone, no requirements)")
        void testBaseCase() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, null);
            assertTrue(canServe(drone, rec), "Base case should pass");
        }

        @Test
        @DisplayName("Vary droneCooling: false (still passes - no cooling required)")
        void testVaryDroneCooling_False() {
            Drone drone = createDrone("D1", false, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary droneHeating: false (still passes - no heating required)")
        void testVaryDroneHeating_False() {
            Drone drone = createDrone("D1", true, false, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqCooling: false (passes - explicitly not required)")
        void testVaryReqCooling_False() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(false, null, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqCooling: true (passes - drone supports cooling)")
        void testVaryReqCooling_True() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(true, null, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqHeating: false (passes - explicitly not required)")
        void testVaryReqHeating_False() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, false, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqHeating: true (passes - drone supports heating)")
        void testVaryReqHeating_True() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, true, null);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqCapacity: less than drone (40 < 50)")
        void testVaryReqCapacity_Less() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, 40.0);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqCapacity: equal to drone (50 == 50)")
        void testVaryReqCapacity_Equal() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, 50.0);
            assertTrue(canServe(drone, rec));
        }

        @Test
        @DisplayName("Vary reqCapacity: greater than drone (60 > 50)")
        void testVaryReqCapacity_Greater() {
            Drone drone = createDrone("D1", true, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, null, 60.0);
            assertFalse(canServe(drone, rec), "Should fail when required capacity exceeds drone");
        }

        // Interaction tests
        @Test
        @DisplayName("Interaction: Cooling required but drone lacks cooling")
        void testInteraction_CoolingMismatch() {
            Drone drone = createDrone("D1", false, true, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(true, null, null);
            assertFalse(canServe(drone, rec), "Should fail when cooling required but not supported");
        }

        @Test
        @DisplayName("Interaction: Heating required but drone lacks heating")
        void testInteraction_HeatingMismatch() {
            Drone drone = createDrone("D1", true, false, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(null, true, null);
            assertFalse(canServe(drone, rec), "Should fail when heating required but not supported");
        }

        @Test
        @DisplayName("Interaction: Both cooling and heating required, drone has neither")
        void testInteraction_BothMissing() {
            Drone drone = createDrone("D1", false, false, DRONE_CAPACITY);
            MedDispatchRec rec = createRecord(true, true, null);
            assertFalse(canServe(drone, rec), "Should fail when both required but neither supported");
        }
    }
}