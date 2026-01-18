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
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Metamorphic Testing: Testing based on metamorphic relations when expected
 * outputs are difficult to determine.
 *
 * Instead of checking specific outputs, we verify that certain mathematical
 * or logical properties hold between related inputs and their outputs.
 *
 * Metamorphic Relations Tested:
 *
 * GeoService:
 * MR1: Distance Symmetry - distance(A,B) = distance(B,A)
 * MR2: Distance Identity - distance(A,A) = 0
 * MR3: Triangle Inequality - distance(A,C) <= distance(A,B) + distance(B,C)
 * MR4: Opposite Direction - moving angle X then X+180 returns near start
 * MR5: isCloseTo Symmetry - isCloseTo(A,B) = isCloseTo(B,A)
 * MR6: isCloseTo Reflexivity - isCloseTo(A,A) = true
 * MR7: Consecutive Moves - n moves in same direction = n * step distance
 *
 * DroneService:
 * MR8: Query Conjunction - more conditions => same or fewer results
 * MR9: Query Commutativity - query order doesn't affect results
 * MR10: Subset Property - matching A AND B implies matching just A
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Metamorphic Testing - Relation-based Property Verification")
public class MetamorphicTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService mockGeoService;
    private DroneService droneService;
    private GeoService geoService;
    private static final String ENDPOINT = "http://api";
    private static final double EPSILON = 1e-10;
    private static final double STEP = 0.00015;

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, mockGeoService, ENDPOINT);
        geoService = new GeoService();
    }

    // GEOSERVICE METAMORPHIC RELATIONS

    @Nested
    @DisplayName("MR1: Distance Symmetry - distance(A,B) = distance(B,A)")
    class DistanceSymmetry {

        static Stream<Arguments> positionPairs() {
            return Stream.of(
                    Arguments.of(new Position(-3.188, 55.944), new Position(-3.190, 55.946)),
                    Arguments.of(new Position(0.0, 0.0), new Position(1.0, 1.0)),
                    Arguments.of(new Position(-180.0, -90.0), new Position(180.0, 90.0)),
                    Arguments.of(new Position(-3.5, 55.0), new Position(-3.5, 56.0)),
                    Arguments.of(new Position(10.0, 20.0), new Position(-10.0, -20.0))
            );
        }

        @ParameterizedTest(name = "distance({0}, {1}) = distance({1}, {0})")
        @MethodSource("positionPairs")
        @DisplayName("Distance from A to B equals distance from B to A")
        void distanceIsSymmetric(Position a, Position b) {
            Double distAB = geoService.calculateDistance(a, b);
            Double distBA = geoService.calculateDistance(b, a);

            assertNotNull(distAB);
            assertNotNull(distBA);
            assertEquals(distAB, distBA, EPSILON,
                    "Distance should be symmetric: d(A,B) = d(B,A)");
        }

        @Test
        @DisplayName("Symmetry holds for random positions")
        void distanceSymmetryRandomPositions() {
            Random rand = new Random(42);
            for (int i = 0; i < 20; i++) {
                Position a = new Position(
                        rand.nextDouble() * 360 - 180,
                        rand.nextDouble() * 180 - 90
                );
                Position b = new Position(
                        rand.nextDouble() * 360 - 180,
                        rand.nextDouble() * 180 - 90
                );

                Double distAB = geoService.calculateDistance(a, b);
                Double distBA = geoService.calculateDistance(b, a);

                assertEquals(distAB, distBA, EPSILON,
                        "Symmetry should hold for random positions");
            }
        }
    }

    @Nested
    @DisplayName("MR2: Distance Identity - distance(A,A) = 0")
    class DistanceIdentity {

        static Stream<Arguments> singlePositions() {
            return Stream.of(
                    Arguments.of(new Position(-3.188, 55.944)),
                    Arguments.of(new Position(0.0, 0.0)),
                    Arguments.of(new Position(-180.0, -90.0)),
                    Arguments.of(new Position(180.0, 90.0)),
                    Arguments.of(new Position(0.0, 0.0))
            );
        }

        @ParameterizedTest(name = "distance({0}, {0}) = 0")
        @MethodSource("singlePositions")
        @DisplayName("Distance from any point to itself is zero")
        void distanceToSelfIsZero(Position p) {
            Double dist = geoService.calculateDistance(p, p);

            assertNotNull(dist);
            assertEquals(0.0, dist, EPSILON,
                    "Distance from a point to itself must be zero");
        }
    }

    @Nested
    @DisplayName("MR3: Triangle Inequality - distance(A,C) <= distance(A,B) + distance(B,C)")
    class TriangleInequality {

        static Stream<Arguments> positionTriples() {
            return Stream.of(
                    Arguments.of(
                            new Position(-3.188, 55.944),
                            new Position(-3.190, 55.946),
                            new Position(-3.192, 55.948)
                    ),
                    Arguments.of(
                            new Position(0.0, 0.0),
                            new Position(1.0, 0.0),
                            new Position(1.0, 1.0)
                    ),
                    Arguments.of(
                            new Position(-10.0, 10.0),
                            new Position(0.0, 0.0),
                            new Position(10.0, 10.0)
                    )
            );
        }

        @ParameterizedTest(name = "d(A,C) <= d(A,B) + d(B,C)")
        @MethodSource("positionTriples")
        @DisplayName("Direct path is never longer than indirect path")
        void triangleInequalityHolds(Position a, Position b, Position c) {
            Double distAC = geoService.calculateDistance(a, c);
            Double distAB = geoService.calculateDistance(a, b);
            Double distBC = geoService.calculateDistance(b, c);

            assertNotNull(distAC);
            assertNotNull(distAB);
            assertNotNull(distBC);

            assertTrue(distAC <= distAB + distBC + EPSILON,
                    "Triangle inequality violated: d(A,C)=" + distAC +
                            " > d(A,B)+d(B,C)=" + (distAB + distBC));
        }

        @Test
        @DisplayName("Triangle inequality holds for random triangles")
        void triangleInequalityRandomPositions() {
            Random rand = new Random(42);
            for (int i = 0; i < 20; i++) {
                Position a = new Position(rand.nextDouble() * 10, rand.nextDouble() * 10);
                Position b = new Position(rand.nextDouble() * 10, rand.nextDouble() * 10);
                Position c = new Position(rand.nextDouble() * 10, rand.nextDouble() * 10);

                Double distAC = geoService.calculateDistance(a, c);
                Double distAB = geoService.calculateDistance(a, b);
                Double distBC = geoService.calculateDistance(b, c);

                assertTrue(distAC <= distAB + distBC + EPSILON,
                        "Triangle inequality must hold for all triangles");
            }
        }
    }

    @Nested
    @DisplayName("MR4: Opposite Direction - move X then X+180 returns near start")
    class OppositeDirection {

        static Stream<Arguments> anglesAndPositions() {
            Position edinburgh = new Position(-3.188, 55.944);
            return Stream.of(
                    Arguments.of(edinburgh, 0.0, 180.0),
                    Arguments.of(edinburgh, 45.0, 225.0),
                    Arguments.of(edinburgh, 90.0, 270.0),
                    Arguments.of(edinburgh, 135.0, 315.0),
                    Arguments.of(edinburgh, 22.5, 202.5),
                    Arguments.of(edinburgh, 67.5, 247.5)
            );
        }

        @ParameterizedTest(name = "Move {1}° then {2}° returns to start")
        @MethodSource("anglesAndPositions")
        @DisplayName("Moving in opposite directions returns to approximate start")
        void oppositeDirectionsReturnToStart(Position start, double angle1, double angle2) {
            Position afterFirst = geoService.nextPosition(start, angle1);
            assertNotNull(afterFirst, "First move should succeed");

            Position afterSecond = geoService.nextPosition(afterFirst, angle2);
            assertNotNull(afterSecond, "Second move should succeed");

            Double distanceFromStart = geoService.calculateDistance(start, afterSecond);
            assertNotNull(distanceFromStart);

            // Should be very close to start (within floating point tolerance)
            assertTrue(distanceFromStart < EPSILON * 1000,
                    "After moving in opposite directions, should return near start. " +
                            "Distance from start: " + distanceFromStart);
        }

        @Test
        @DisplayName("All 16 angles have valid opposites")
        void allAnglesHaveOpposites() {
            double[] angles = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
                    180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5};
            Position start = new Position(-3.188, 55.944);

            for (double angle : angles) {
                double opposite = (angle + 180) % 360;

                Position p1 = geoService.nextPosition(start, angle);
                Position p2 = geoService.nextPosition(p1, opposite);

                assertNotNull(p2, "Opposite move for angle " + angle + " should succeed");

                Double dist = geoService.calculateDistance(start, p2);
                assertTrue(dist < EPSILON * 1000,
                        "Angle " + angle + " and " + opposite + " should return to start");
            }
        }
    }

    @Nested
    @DisplayName("MR5: isCloseTo Symmetry - isCloseTo(A,B) = isCloseTo(B,A)")
    class IsCloseToSymmetry {

        @Test
        @DisplayName("isCloseTo is symmetric for close positions")
        void isCloseToSymmetricClose() {
            Position a = new Position(-3.188, 55.944);
            Position b = new Position(-3.188, 55.944 + STEP / 2); // Within threshold

            Boolean closeAB = geoService.isCloseTo(a, b);
            Boolean closeBA = geoService.isCloseTo(b, a);

            assertEquals(closeAB, closeBA, "isCloseTo should be symmetric");
            assertTrue(closeAB, "Positions within threshold should be close");
        }

        @Test
        @DisplayName("isCloseTo is symmetric for distant positions")
        void isCloseToSymmetricDistant() {
            Position a = new Position(-3.188, 55.944);
            Position b = new Position(-3.200, 55.960); // Far apart

            Boolean closeAB = geoService.isCloseTo(a, b);
            Boolean closeBA = geoService.isCloseTo(b, a);

            assertEquals(closeAB, closeBA, "isCloseTo should be symmetric");
            assertFalse(closeAB, "Distant positions should not be close");
        }
    }

    @Nested
    @DisplayName("MR6: isCloseTo Reflexivity - isCloseTo(A,A) = true")
    class IsCloseToReflexivity {

        static Stream<Arguments> positions() {
            return Stream.of(
                    Arguments.of(new Position(-3.188, 55.944)),
                    Arguments.of(new Position(0.0, 0.0)),
                    Arguments.of(new Position(-180.0, 90.0)),
                    Arguments.of(new Position(100.5, -45.3))
            );
        }

        @ParameterizedTest(name = "isCloseTo({0}, {0}) = true")
        @MethodSource("positions")
        @DisplayName("Any position is close to itself")
        void positionIsCloseToItself(Position p) {
            Boolean result = geoService.isCloseTo(p, p);

            assertNotNull(result);
            assertTrue(result, "A position must be close to itself");
        }
    }

    @Nested
    @DisplayName("MR7: Consecutive Moves - n moves = n * step distance from start")
    class ConsecutiveMoves {

        @Test
        @DisplayName("Multiple moves in same direction accumulate linearly")
        void movesAccumulateLinearly() {
            Position start = new Position(-3.188, 55.944);
            double angle = 0.0; // East

            Position after1 = geoService.nextPosition(start, angle);
            Position after2 = geoService.nextPosition(after1, angle);
            Position after3 = geoService.nextPosition(after2, angle);

            Double dist1 = geoService.calculateDistance(start, after1);
            Double dist2 = geoService.calculateDistance(start, after2);
            Double dist3 = geoService.calculateDistance(start, after3);

            // Distance should scale linearly
            assertEquals(STEP, dist1, EPSILON, "1 move = 1 step");
            assertEquals(2 * STEP, dist2, EPSILON, "2 moves = 2 steps");
            assertEquals(3 * STEP, dist3, EPSILON, "3 moves = 3 steps");
        }

        @Test
        @DisplayName("Linear accumulation holds for all cardinal directions")
        void linearAccumulationAllDirections() {
            double[] cardinals = {0, 90, 180, 270}; // E, N, W, S

            for (double angle : cardinals) {
                Position start = new Position(0.0, 0.0);
                Position current = start;

                for (int n = 1; n <= 5; n++) {
                    current = geoService.nextPosition(current, angle);
                    assertNotNull(current);

                    Double dist = geoService.calculateDistance(start, current);
                    assertEquals(n * STEP, dist, EPSILON,
                            "After " + n + " moves at angle " + angle +
                                    ", distance should be " + (n * STEP));
                }
            }
        }
    }

    //DRONESERVICE METAMORPHIC RELATIONS

    @Nested
    @DisplayName("MR8: Query Conjunction - more conditions => fewer or equal results")
    class QueryConjunction {

        private Drone[] createTestDrones() {
            Drone d1 = new Drone();
            d1.id = "D1";
            d1.name = "Drone1";
            Capability c1 = new Capability();
            c1.cooling = true;
            c1.heating = false;
            c1.capacity = 10.0;
            c1.maxMoves = 1000;
            d1.capability = c1;

            Drone d2 = new Drone();
            d2.id = "D2";
            d2.name = "Drone2";
            Capability c2 = new Capability();
            c2.cooling = true;
            c2.heating = true;
            c2.capacity = 20.0;
            c2.maxMoves = 2000;
            d2.capability = c2;

            Drone d3 = new Drone();
            d3.id = "D3";
            d3.name = "Drone3";
            Capability c3 = new Capability();
            c3.cooling = false;
            c3.heating = true;
            c3.capacity = 15.0;
            c3.maxMoves = 1500;
            d3.capability = c3;

            return new Drone[]{d1, d2, d3};
        }

        private QueryAttribute createQuery(String attr, String op, String val) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attr;
            q.operator = op;
            q.value = val;
            return q;
        }

        @Test
        @DisplayName("Adding conditions reduces or maintains result count")
        void addingConditionsReducesResults() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(createTestDrones());

            // Query with 1 condition
            List<String> result1 = droneService.query(List.of(
                    createQuery("cooling", "=", "true")
            ));

            // Query with 2 conditions (more restrictive)
            List<String> result2 = droneService.query(List.of(
                    createQuery("cooling", "=", "true"),
                    createQuery("heating", "=", "true")
            ));

            // Query with 3 conditions (even more restrictive)
            List<String> result3 = droneService.query(List.of(
                    createQuery("cooling", "=", "true"),
                    createQuery("heating", "=", "true"),
                    createQuery("capacity", ">", "15")
            ));

            assertTrue(result1.size() >= result2.size(),
                    "Adding conditions should not increase results: " +
                            result1.size() + " >= " + result2.size());

            assertTrue(result2.size() >= result3.size(),
                    "Adding more conditions should not increase results: " +
                            result2.size() + " >= " + result3.size());
        }

        @Test
        @DisplayName("Subset of results maintained when adding conditions")
        void subsetMaintainedWhenAddingConditions() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(createTestDrones());

            List<String> broader = droneService.query(List.of(
                    createQuery("cooling", "=", "true")
            ));

            List<String> narrower = droneService.query(List.of(
                    createQuery("cooling", "=", "true"),
                    createQuery("capacity", ">", "5")
            ));

            // All results from narrower query must be in broader query
            assertTrue(broader.containsAll(narrower),
                    "Narrower query results must be subset of broader query results");
        }
    }

    @Nested
    @DisplayName("MR9: Query Commutativity - query order doesn't affect results")
    class QueryCommutativity {

        private Drone[] createTestDrones() {
            Drone d1 = new Drone();
            d1.id = "D1";
            Capability c1 = new Capability();
            c1.cooling = true;
            c1.capacity = 10.0;
            d1.capability = c1;

            Drone d2 = new Drone();
            d2.id = "D2";
            Capability c2 = new Capability();
            c2.cooling = true;
            c2.capacity = 20.0;
            d2.capability = c2;

            return new Drone[]{d1, d2};
        }

        private QueryAttribute createQuery(String attr, String op, String val) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attr;
            q.operator = op;
            q.value = val;
            return q;
        }

        @Test
        @DisplayName("Swapping query order gives same results")
        void queryOrderDoesNotMatter() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(createTestDrones());

            QueryAttribute q1 = createQuery("cooling", "=", "true");
            QueryAttribute q2 = createQuery("capacity", ">", "5");

            List<String> resultAB = droneService.query(List.of(q1, q2));
            List<String> resultBA = droneService.query(List.of(q2, q1));

            assertEquals(resultAB.size(), resultBA.size(),
                    "Query order should not affect result count");
            assertTrue(resultAB.containsAll(resultBA) && resultBA.containsAll(resultAB),
                    "Query order should not affect which drones are returned");
        }

        @Test
        @DisplayName("Three conditions in any order give same results")
        void threeConditionsAnyOrder() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(createTestDrones());

            QueryAttribute q1 = createQuery("cooling", "=", "true");
            QueryAttribute q2 = createQuery("capacity", ">", "5");
            QueryAttribute q3 = createQuery("capacity", "<", "25");

            List<String> result123 = droneService.query(List.of(q1, q2, q3));
            List<String> result321 = droneService.query(List.of(q3, q2, q1));
            List<String> result213 = droneService.query(List.of(q2, q1, q3));

            assertEquals(result123.size(), result321.size());
            assertEquals(result123.size(), result213.size());
            assertTrue(result123.containsAll(result321));
            assertTrue(result123.containsAll(result213));
        }
    }

    @Nested
    @DisplayName("MR10: Subset Property - matching A AND B implies matching A alone")
    class SubsetProperty {

        private Drone[] createTestDrones() {
            Drone d1 = new Drone();
            d1.id = "D1";
            Capability c1 = new Capability();
            c1.cooling = true;
            c1.heating = true;
            c1.capacity = 15.0;
            d1.capability = c1;

            return new Drone[]{d1};
        }

        private QueryAttribute createQuery(String attr, String op, String val) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attr;
            q.operator = op;
            q.value = val;
            return q;
        }

        @Test
        @DisplayName("Drone matching combined query matches individual queries")
        void combinedMatchImpliesIndividualMatch() {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(createTestDrones());

            QueryAttribute qCooling = createQuery("cooling", "=", "true");
            QueryAttribute qHeating = createQuery("heating", "=", "true");

            // Combined query
            List<String> combined = droneService.query(List.of(qCooling, qHeating));

            // Individual queries
            List<String> coolingOnly = droneService.query(List.of(qCooling));
            List<String> heatingOnly = droneService.query(List.of(qHeating));

            // Any drone in combined must be in both individual results
            for (String droneId : combined) {
                assertTrue(coolingOnly.contains(droneId),
                        "Drone " + droneId + " matches combined but not cooling-only query");
                assertTrue(heatingOnly.contains(droneId),
                        "Drone " + droneId + " matches combined but not heating-only query");
            }
        }
    }
}
