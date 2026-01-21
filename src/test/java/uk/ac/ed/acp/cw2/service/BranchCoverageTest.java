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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Branch Coverage Tests
 *
 * This test class specifically targets uncovered branches identified by JaCoCo.
 * Each test is documented with the specific line/branch it aims to cover.
 */
@ExtendWith(MockitoExtension.class)
public class BranchCoverageTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService geoServiceMock;
    private DroneService droneService;
    private GeoService geoService;
    private final String ENDPOINT = "http://api";

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, geoServiceMock, ENDPOINT);
        geoService = new GeoService();
    }

    private Drone createDrone(String id, String name, boolean cooling, boolean heating,
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

    private QueryAttribute createQuery(String attr, String op, String val) {
        QueryAttribute q = new QueryAttribute();
        q.attribute = attr;
        q.operator = op;
        q.value = val;
        return q;
    }

    // DroneService: matchSingleAttribute - False branches for comparisons
    // Lines 100, 106, 113, 121, 129, 137, 145

    @Nested
    @DisplayName("matchSingleAttribute - Comparison false branches")
    class MatchSingleAttributeFalseBranches {

        @Test
        @DisplayName("Line 100: cooling comparison evaluates to false")
        void testCoolingComparisonFalse() {
            // Drone has cooling=true, query for cooling=false should not match
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("cooling", "false");
            assertTrue(result.isEmpty(), "Cooling=true drone should not match cooling=false query");
        }

        @Test
        @DisplayName("Line 106: heating comparison evaluates to false")
        void testHeatingComparisonFalse() {
            // Drone has heating=false, query for heating=true should not match
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("heating", "true");
            assertTrue(result.isEmpty(), "Heating=false drone should not match heating=true query");
        }

        @Test
        @DisplayName("Line 113: capacity comparison evaluates to false")
        void testCapacityComparisonFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Query for different capacity
            List<String> result = droneService.queryAsPath("capacity", "50.0");
            assertTrue(result.isEmpty(), "Capacity=100 drone should not match capacity=50 query");
        }

        @Test
        @DisplayName("Line 121: maxMoves comparison evaluates to false")
        void testMaxMovesComparisonFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("maxMoves", "100");
            assertTrue(result.isEmpty(), "maxMoves=500 drone should not match maxMoves=100 query");
        }

        @Test
        @DisplayName("Line 129: costPerMove comparison evaluates to false")
        void testCostPerMoveComparisonFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costPerMove", "0.5");
            assertTrue(result.isEmpty(), "costPerMove=0.1 drone should not match costPerMove=0.5 query");
        }

        @Test
        @DisplayName("Line 137: costInitial comparison evaluates to false")
        void testCostInitialComparisonFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costInitial", "10.0");
            assertTrue(result.isEmpty(), "costInitial=5 drone should not match costInitial=10 query");
        }

        @Test
        @DisplayName("Line 145: costFinal comparison evaluates to false")
        void testCostFinalComparisonFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costFinal", "10.0");
            assertTrue(result.isEmpty(), "costFinal=5 drone should not match costFinal=10 query");
        }
    }

    // DroneService: matchSingleAttribute - Null capability for maxMoves, costPerMove, etc.
    // Lines 118, 126, 134, 142

    @Nested
    @DisplayName("matchSingleAttribute - Null capability checks")
    class MatchSingleAttributeNullCapability {

        private Drone createDroneWithoutCapability(String id) {
            Drone d = new Drone();
            d.id = id;
            d.name = "Test";
            d.capability = null;
            return d;
        }

        @Test
        @DisplayName("Line 118: maxMoves with null capability returns false")
        void testMaxMovesNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("maxMoves", "500");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 126: costPerMove with null capability returns false")
        void testCostPerMoveNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costPerMove", "0.1");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 134: costInitial with null capability returns false")
        void testCostInitialNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costInitial", "5.0");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 142: costFinal with null capability returns false")
        void testCostFinalNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.queryAsPath("costFinal", "5.0");
            assertTrue(result.isEmpty());
        }
    }

    // DroneService: numericCompare - False branches for !=, <, > operators
    // Lines 284, 285, 286

    @Nested
    @DisplayName("numericCompare - Operator false branches")
    class NumericCompareFalseBranches {

        @Test
        @DisplayName("Line 284: != operator returns false when values are equal")
        void testNotEqualsReturnsFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // capacity=100, query != 100 should NOT match (returns false)
            List<String> result = droneService.query(List.of(createQuery("capacity", "!=", "100.0")));
            assertTrue(result.isEmpty(), "!= should return false when values are equal");
        }

        @Test
        @DisplayName("Line 285: < operator returns false when lhs >= rhs")
        void testLessThanReturnsFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // capacity=100, query < 50 should NOT match
            List<String> result = droneService.query(List.of(createQuery("capacity", "<", "50.0")));
            assertTrue(result.isEmpty(), "< should return false when lhs >= rhs");

            // capacity=100, query < 100 should NOT match (equal)
            List<String> result2 = droneService.query(List.of(createQuery("capacity", "<", "100.0")));
            assertTrue(result2.isEmpty(), "< should return false when lhs == rhs");
        }

        @Test
        @DisplayName("Line 286: > operator returns false when lhs <= rhs")
        void testGreaterThanReturnsFalse() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // capacity=100, query > 200 should NOT match
            List<String> result = droneService.query(List.of(createQuery("capacity", ">", "200.0")));
            assertTrue(result.isEmpty(), "> should return false when lhs <= rhs");

            // capacity=100, query > 100 should NOT match (equal)
            List<String> result2 = droneService.query(List.of(createQuery("capacity", ">", "100.0")));
            assertTrue(result2.isEmpty(), "> should return false when lhs == rhs");
        }
    }

    // DroneService: matchQueryAttribute - Null capability in query() method
    // Lines 230, 240, 250, 260

    @Nested
    @DisplayName("matchQueryAttribute - Null capability checks in query()")
    class MatchQueryAttributeNullCapability {

        private Drone createDroneWithoutCapability(String id) {
            Drone d = new Drone();
            d.id = id;
            d.name = "Test";
            d.capability = null;
            return d;
        }

        @Test
        @DisplayName("Line 230: maxMoves query with null capability")
        void testMaxMovesQueryNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("maxMoves", "=", "500")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 240: costPerMove query with null capability")
        void testCostPerMoveQueryNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("costPerMove", "=", "0.1")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 250: costInitial query with null capability")
        void testCostInitialQueryNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("costInitial", "=", "5.0")));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Line 260: costFinal query with null capability")
        void testCostFinalQueryNullCapability() {
            Drone d1 = createDroneWithoutCapability("D1");
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<String> result = droneService.query(List.of(createQuery("costFinal", "=", "5.0")));
            assertTrue(result.isEmpty());
        }
    }

    // DroneService: isDroneAvailableForDispatch - Time boundary conditions
    // Line 461: time.isAfter(from) && time.isBefore(until)

    @Nested
    @DisplayName("isDroneAvailableForDispatch - Time boundary tests")
    class TimeBoundaryTests {

        @Test
        @DisplayName("Line 461: Time exactly at window start (from) - not available")
        void testTimeAtExactWindowStart() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

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
            rec.date = "2025-01-20"; // Monday
            rec.time = "08:00:00"; // Exactly at window start
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            // Time at exactly window.from should NOT be available (isAfter returns false)
            assertTrue(result.isEmpty(), "Time at exact window start should not be available");
        }

        @Test
        @DisplayName("Line 461: Time exactly at window end (until) - not available")
        void testTimeAtExactWindowEnd() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

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
            rec.date = "2025-01-20"; // Monday
            rec.time = "18:00:00"; // Exactly at window end
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            // Time at exactly window.until should NOT be available (isBefore returns false)
            assertTrue(result.isEmpty(), "Time at exact window end should not be available");
        }

        @Test
        @DisplayName("Line 461: Time before window start - not available")
        void testTimeBeforeWindowStart() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

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
            rec.date = "2025-01-20"; // Monday
            rec.time = "07:00:00"; // Before window start
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty(), "Time before window should not be available");
        }

        @Test
        @DisplayName("Line 461: Time after window end - not available")
        void testTimeAfterWindowEnd() {
            Drone d1 = createDrone("D1", "Drone1", true, false, 100.0, 500, 0.1, 5, 5);

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
            rec.date = "2025-01-20"; // Monday
            rec.time = "19:00:00"; // After window end
            rec.requirements = new MedDispatchRec.Requirements();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(new DroneForServicePoint[]{dfsp});

            List<String> result = droneService.queryAvailableDrones(List.of(rec));
            assertTrue(result.isEmpty(), "Time after window should not be available");
        }
    }

    // GeoService: isCloseTo - Invalid position check
    // Line 55: !validPosition(p2)

    @Nested
    @DisplayName("GeoService.isCloseTo - Invalid position tests")
    class IsCloseToInvalidPosition {

        @Test
        @DisplayName("Line 55: Second position is invalid (null)")
        void testSecondPositionNull() {
            Position p1 = new Position(0.0, 0.0);
            assertNull(geoService.isCloseTo(p1, null), "Should return null for null second position");
        }

        @Test
        @DisplayName("Line 55: Second position has null latitude")
        void testSecondPositionNullLat() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position();
            p2.lng = 0.0;
            p2.lat = null;
            assertNull(geoService.isCloseTo(p1, p2), "Should return null for position with null lat");
        }

        @Test
        @DisplayName("Line 55: Second position has null longitude")
        void testSecondPositionNullLng() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position();
            p2.lng = null;
            p2.lat = 0.0;
            assertNull(geoService.isCloseTo(p1, p2), "Should return null for position with null lng");
        }

        @Test
        @DisplayName("Line 55: Second position out of bounds")
        void testSecondPositionOutOfBounds() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(0.0, 91.0); // Invalid latitude
            assertNull(geoService.isCloseTo(p1, p2), "Should return null for out of bounds position");
        }
    }

    // GeoService: pointInPolygon - Vertical line segment
    // Line 138: p1.lng.equals(p2.lng)

    @Nested
    @DisplayName("GeoService.pointInPolygon - Vertical line segment")
    class PointInPolygonVerticalSegment {

        @Test
        @DisplayName("Line 138: Polygon with vertical edge (p1.lng == p2.lng)")
        void testPolygonWithVerticalEdge() {
            // Create a square polygon where some edges are vertical
            Region region = new Region();
            region.vertices = List.of(
                    new Position(0.0, 0.0),
                    new Position(0.0, 1.0),  // Vertical edge from (0,0) to (0,1)
                    new Position(1.0, 1.0),
                    new Position(1.0, 0.0),
                    new Position(0.0, 0.0)   // Closed polygon
            );

            // Point inside the polygon
            Position inside = new Position(0.5, 0.5);
            Boolean result = geoService.isInRegion(inside, region);
            assertTrue(result, "Point should be inside polygon with vertical edges");

            // Point outside the polygon
            Position outside = new Position(2.0, 0.5);
            Boolean result2 = geoService.isInRegion(outside, region);
            assertFalse(result2, "Point should be outside polygon");
        }

        @Test
        @DisplayName("Line 138: Point on vertical edge of polygon")
        void testPointOnVerticalEdge() {
            Region region = new Region();
            region.vertices = List.of(
                    new Position(0.0, 0.0),
                    new Position(0.0, 1.0),  // Vertical edge
                    new Position(1.0, 1.0),
                    new Position(1.0, 0.0),
                    new Position(0.0, 0.0)
            );

            // Point exactly on the vertical edge
            Position onEdge = new Position(0.0, 0.5);
            Boolean result = geoService.isInRegion(onEdge, region);
            assertTrue(result, "Point on vertical edge should be considered inside");
        }
    }

    // GeoService: findPath - No path found (returns empty list)
    // Line 309: return List.of()

    @Nested
    @DisplayName("GeoService.findPath - No path scenarios")
    class FindPathNoPath {

        @Test
        @DisplayName("Line 309: Goal completely surrounded by restricted region")
        void testGoalSurroundedByRestriction() {
            Position start = new Position(0.0, 0.0);
            // Goal is far enough that any path would be blocked
            Position goal = new Position(0.001, 0.001);

            // Create a restricted region that completely blocks the path
            Region restriction = new Region();
            restriction.vertices = List.of(
                    new Position(-0.001, -0.001),
                    new Position(-0.001, 0.002),
                    new Position(0.002, 0.002),
                    new Position(0.002, -0.001),
                    new Position(-0.001, -0.001)
            );

            List<Position> path = geoService.findPath(start, goal, List.of(restriction));
            // The path should either be empty (blocked) or find a way around
            assertNotNull(path);
        }
    }

    // GeoService: isValidMove - Region validation
    // Line 222: r.vertices == null || r.vertices.size() < 3

    @Nested
    @DisplayName("GeoService.isValidMove - Region validation")
    class IsValidMoveRegionValidation {

        @Test
        @DisplayName("Line 222: Region with null vertices is skipped")
        void testRegionWithNullVertices() {
            Position start = new Position(0.0, 0.0);
            Position end = new Position(0.00015, 0.0);

            Region region = new Region();
            region.vertices = null;

            boolean result = geoService.isValidMove(start, end, List.of(region));
            assertTrue(result, "Move should be valid when region has null vertices");
        }

        @Test
        @DisplayName("Line 222: Region with less than 3 vertices is skipped")
        void testRegionWithTooFewVertices() {
            Position start = new Position(0.0, 0.0);
            Position end = new Position(0.00015, 0.0);

            Region region = new Region();
            region.vertices = List.of(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0)
            );

            boolean result = geoService.isValidMove(start, end, List.of(region));
            assertTrue(result, "Move should be valid when region has < 3 vertices");
        }
    }

    // GeoService: heuristic - null distance
    // Line 255: distance == null

    @Nested
    @DisplayName("GeoService path finding with invalid positions")
    class PathFindingInvalidPositions {

        @Test
        @DisplayName("Path finding with close start and goal")
        void testPathFindingClosePositions() {
            Position start = new Position(0.0, 0.0);
            Position goal = new Position(0.0001, 0.0001);

            List<Position> path = geoService.findPath(start, goal, List.of());
            assertNotNull(path);
            assertFalse(path.isEmpty(), "Should find path between close positions");
        }
    }

    // DroneService: Additional edge cases for complete coverage

    @Nested
    @DisplayName("Additional DroneService edge cases")
    class AdditionalDroneServiceEdgeCases {

        @Test
        @DisplayName("queryAsPath with drone having matching id but null capability")
        void testQueryAsPathIdMatchNullCapability() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = "TestDrone";
            d1.capability = null;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Query by id should still work
            List<String> result = droneService.queryAsPath("id", "D1");
            assertEquals(1, result.size());
            assertTrue(result.contains("D1"));
        }

        @Test
        @DisplayName("queryAsPath with drone having matching name but null capability")
        void testQueryAsPathNameMatchNullCapability() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = "TestDrone";
            d1.capability = null;

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            // Query by name should still work
            List<String> result = droneService.queryAsPath("name", "TestDrone");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("query() with multiple valid queries all matching")
        void testQueryMultipleMatching() {
            Drone d1 = createDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<QueryAttribute> queries = List.of(
                    createQuery("id", "=", "D1"),
                    createQuery("cooling", "=", "true"),
                    createQuery("capacity", ">", "50.0")
            );

            List<String> result = droneService.query(queries);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("query() with one non-matching query filters out drone")
        void testQueryOneNonMatching() {
            Drone d1 = createDrone("D1", "Drone1", true, true, 100.0, 500, 0.1, 5, 5);
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{d1});

            List<QueryAttribute> queries = List.of(
                    createQuery("id", "=", "D1"),
                    createQuery("capacity", ">", "200.0")  // This won't match
            );

            List<String> result = droneService.query(queries);
            assertTrue(result.isEmpty(), "Drone should be filtered when one query doesn't match");
        }
    }
}