package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Position;
import uk.ac.ed.acp.cw2.data.Region;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specification-Based (Black-Box) Techniques: State Transition Testing
 * <p>
 * This test class demonstrates State Transition Testing for drone movement
 * through the GeoService methods (nextPosition, isValidMove, isInRegion).
 * <p>
 * STATES:
 *   S1: VALID - Position is in valid coordinate range and not in restricted area
 *   S2: BLOCKED - Position is inside a restricted area
 *   S3: OUT_OF_BOUNDS - Position has invalid coordinates (lat/lng out of range)
 * <p>
 * EVENTS/TRANSITIONS:
 *   E1: Move with valid angle to free space (VALID -> VALID)
 *   E2: Move into restricted area (VALID -> BLOCKED)
 *   E3: Move to boundary/out of bounds (VALID -> OUT_OF_BOUNDS)
 *   E4: Move away from restricted area (BLOCKED -> VALID)
 *   E5: Invalid angle (Any -> rejected, returns null)
 * <p>
 * State Transition Table in portfolio //TODO add table
 */
public class GeoServiceStateTransitionTest {

    private GeoService geoService;
    private Region restrictedArea;
    private List<Region> restrictedAreas;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();

        // Define a restricted area (small square)
        // closed polygon
        // A restricted square region from (0.001, 0.001) to (0.002, 0.002)
        List<Position> restrictedPolygon = Arrays.asList(
                new Position(0.001, 0.001),
                new Position(0.002, 0.001),
                new Position(0.002, 0.002),
                new Position(0.001, 0.002),
                new Position(0.001, 0.001)  // closed polygon
        );

        restrictedArea = new Region();
        restrictedArea.vertices = restrictedPolygon;
        restrictedAreas = List.of(restrictedArea);
    }


    // STATE: VALID (S1) - Position in valid range, not in restricted area
    @Nested
    @DisplayName("State S1: VALID - Position in free space")
    class StateValid {

        @Test
        @DisplayName("S1: Verify initial VALID state")
        void testInitialValidState() {
            Position validPos = new Position(0.0, 0.0);

            // Position should be valid (not in restricted area)
            Boolean inRegion = geoService.isInRegion(validPos, restrictedArea);
            assertFalse(inRegion, "Position (0,0) should not be in restricted area");
        }

        @Test
        @DisplayName("S1: Multiple positions in VALID state")
        void testMultipleValidPositions() {
            // Various positions outside the restricted area
            Position[] validPositions = {
                    new Position(0.0, 0.0),
                    new Position(-0.001, -0.001),
                    new Position(0.003, 0.003),
                    new Position(0.0005, 0.0005)  // Just outside restricted area
            };

            for (Position pos : validPositions) {
                Boolean inRegion = geoService.isInRegion(pos, restrictedArea);
                assertFalse(inRegion, "Position should be in VALID state (outside restricted area)");
            }
        }
    }

    // STATE: BLOCKED (S2) - Position inside restricted area
    @Nested
    @DisplayName("State S2: BLOCKED - Position in restricted area")
    class StateBlocked {

        @Test
        @DisplayName("S2: Verify BLOCKED state (center of restricted area)")
        void testBlockedStateCenter() {
            Position blockedPos = new Position(0.0015, 0.0015);  // Center of restricted square

            Boolean inRegion = geoService.isInRegion(blockedPos, restrictedArea);
            assertTrue(inRegion, "Position inside restricted area should be BLOCKED");
        }

        @Test
        @DisplayName("S2: BLOCKED state on boundary of restricted area")
        void testBlockedStateBoundary() {
            Position onBoundary = new Position(0.001, 0.0015);  // On left edge

            Boolean inRegion = geoService.isInRegion(onBoundary, restrictedArea);
            assertTrue(inRegion, "Position on boundary should be considered BLOCKED");
        }
    }

    // STATE: OUT_OF_BOUNDS (S3) - Invalid coordinates
    @Nested
    @DisplayName("State S3: OUT_OF_BOUNDS - Invalid coordinates")
    class StateOutOfBounds {

        @Test
        @DisplayName("S3: Latitude too high (> 90)")
        void testOutOfBoundsLatHigh() {
            Position outOfBounds = new Position(0.0, 90.1);

            Boolean result = geoService.isInRegion(outOfBounds, restrictedArea);
            assertNull(result, "Invalid latitude should result in OUT_OF_BOUNDS (null)");
        }

        @Test
        @DisplayName("S3: Longitude too low (< -180)")
        void testOutOfBoundsLngLow() {
            Position outOfBounds = new Position(-180.1, 0.0);

            Boolean result = geoService.isInRegion(outOfBounds, restrictedArea);
            assertNull(result, "Invalid longitude should result in OUT_OF_BOUNDS (null)");
        }
    }

    // TRANSITION E1: VALID -> VALID (Move to free space)
    @Nested
    @DisplayName("Transition E1: VALID -> VALID (valid move in free space)")
    class TransitionValidToValid {

        @Test
        @DisplayName("E1: Single step move stays in VALID state")
        void testSingleStepValidToValid() {
            Position start = new Position(0.0, 0.0);
            Position end = geoService.nextPosition(start, 0.0);  // Move East

            assertNotNull(end, "nextPosition should return valid position");
            assertTrue(geoService.isValidMove(start, end, restrictedAreas),
                    "Move in free space should be valid");
            assertFalse(geoService.isInRegion(end, restrictedArea),
                    "End position should still be in VALID state");
        }

        @Test
        @DisplayName("E1: Multiple consecutive moves in VALID state")
        void testMultipleMovesValidToValid() {
            Position current = new Position(0.0, 0.0);

            // Sequence of moves: East, North, West, South (should stay in VALID)
            double[] angles = {0.0, 90.0, 180.0, 270.0};

            for (double angle : angles) {
                Position next = geoService.nextPosition(current, angle);
                assertNotNull(next, "Each move should produce valid position");
                assertTrue(geoService.isValidMove(current, next, restrictedAreas),
                        "Each move should be valid (VALID -> VALID)");
                current = next;
            }
        }

        @Test
        @DisplayName("E1: Move around restricted area (path avoidance)")
        void testMoveAroundRestrictedArea() {
            // Start just below the restricted area, move around it
            Position start = new Position(0.0005, 0.0005);

            // Move East (away from restricted area)
            Position end = geoService.nextPosition(start, 0.0);

            assertTrue(geoService.isValidMove(start, end, restrictedAreas),
                    "Move away from restricted area should remain VALID");
        }
    }

    // TRANSITION E2: VALID -> BLOCKED (Move into restricted area)
    @Nested
    @DisplayName("Transition E2: VALID -> BLOCKED (move into restricted area)")
    class TransitionValidToBlocked {

        @Test
        @DisplayName("E2: Direct move into restricted area is blocked")
        void testMoveIntoRestrictedArea() {
            // Position just outside restricted area
            Position start = new Position(0.00095, 0.0015);
            // Position inside restricted area
            Position end = new Position(0.0015, 0.0015);

            assertFalse(geoService.isValidMove(start, end, restrictedAreas),
                    "Move into restricted area should be BLOCKED (invalid)");
        }

        @Test
        @DisplayName("E2: Path crossing restricted area is blocked")
        void testPathCrossingRestrictedArea() {
            // Start before restricted area
            Position start = new Position(0.0005, 0.0015);
            // End after restricted area (path crosses through it)
            Position end = new Position(0.0025, 0.0015);

            assertFalse(geoService.isValidMove(start, end, restrictedAreas),
                    "Path crossing restricted area should be BLOCKED");
        }

        @Test
        @DisplayName("E2: Move ending on restricted boundary is blocked")
        void testMoveToRestrictedBoundary() {
            Position start = new Position(0.0005, 0.001);
            Position end = new Position(0.001, 0.001);  // On boundary vertex

            // Boundary is considered inside
            assertFalse(geoService.isValidMove(start, end, restrictedAreas),
                    "Move to restricted boundary should be BLOCKED");
        }
    }

    // TRANSITION E3: Boundary handling (longitude wraps, latitude returns null)
    @Nested
    @DisplayName("Transition E3: Boundary handling (longitude wraps, latitude out-of-bounds returns null)")
    class TransitionBoundaryHandling {

        @Test
        @DisplayName("E3a: Moving past North pole returns null")
        void testLatitudeNorthPoleReturnsNull() {
            Position nearPole = new Position(0.0, 89.99999);
            Position next = geoService.nextPosition(nearPole, 90.0);  // Move North

            assertNull(next, "Cannot move past North pole - should return null");
        }

        @Test
        @DisplayName("E3b: Moving past South pole returns null")
        void testLatitudeSouthPoleReturnsNull() {
            Position nearPole = new Position(0.0, -89.99999);
            Position next = geoService.nextPosition(nearPole, 270.0);  // Move South

            assertNull(next, "Cannot move past South pole - should return null");
        }

        @Test
        @DisplayName("E3c: Longitude wraps from +180 to -180")
        void testLongitudeWrapsPositive() {
            Position nearEdge = new Position(179.99999, 0.0);
            Position next = geoService.nextPosition(nearEdge, 0.0);  // Move East

            assertNotNull(next, "nextPosition should return valid position");
            assertTrue(next.lng >= -180.0 && next.lng <= 180.0,
                    "Longitude should wrap to valid range");
            // After crossing 180, should wrap to negative
            assertTrue(next.lng < 0, "Longitude should wrap to negative side");
        }

        @Test
        @DisplayName("E3d: Longitude wraps from -180 to +180")
        void testLongitudeWrapsNegative() {
            Position nearEdge = new Position(-179.99999, 0.0);
            Position next = geoService.nextPosition(nearEdge, 180.0);  // Move West

            assertNotNull(next, "nextPosition should return valid position");
            assertTrue(next.lng >= -180.0 && next.lng <= 180.0,
                    "Longitude should wrap to valid range");
            // After crossing -180, should wrap to positive
            assertTrue(next.lng > 0, "Longitude should wrap to positive side");
        }

        @Test
        @DisplayName("E3e: Invalid starting position is rejected (OUT_OF_BOUNDS)")
        void testInvalidStartPosition() {
            Position invalid = new Position(0.0, 91.0);

            Position result = geoService.nextPosition(invalid, 0.0);
            assertNull(result, "Cannot transition from OUT_OF_BOUNDS starting state");
        }
    }

    // TRANSITION E4: BLOCKED -> VALID (Move away from restricted area)
    @Nested
    @DisplayName("Transition E4: BLOCKED -> VALID (escaping restricted area)")
    class TransitionBlockedToValid {

        @Test
        @DisplayName("E4: findPath escapes blocked area by routing around")
        void testPathAroundRestrictedArea() {
            // Start before restricted area
            Position start = new Position(0.0, 0.0015);
            // Goal after restricted area
            Position goal = new Position(0.003, 0.0015);

            List<Position> path = geoService.findPath(start, goal, restrictedAreas);

            assertFalse(path.isEmpty(), "Path should exist around restricted area");

            // Verify path doesn't go through restricted area
            for (int i = 0; i < path.size() - 1; i++) {
                assertTrue(geoService.isValidMove(path.get(i), path.get(i + 1), restrictedAreas),
                        "Each segment of path should avoid BLOCKED state");
            }
        }

        @Test
        @DisplayName("E4: Route navigates from near-blocked to VALID")
        void testNavigateAwayFromRestricted() {
            // Position very close to restricted area (but valid)
            Position nearBlocked = new Position(0.00099, 0.0015);
            // Goal in opposite direction
            Position goal = new Position(0.0, 0.0);

            List<Position> path = geoService.findPath(nearBlocked, goal, restrictedAreas);

            assertFalse(path.isEmpty(), "Should find path away from restricted area");
            // First position should be the start
            assertEquals(nearBlocked.lng, path.getFirst().lng, 0.0001);
            assertEquals(nearBlocked.lat, path.getFirst().lat, 0.0001);
        }
    }

    // TRANSITION E5: Invalid angle (rejected transition)
    @Nested
    @DisplayName("Transition E5: Invalid angle (transition rejected)")
    class TransitionInvalidAngle {

        @Test
        @DisplayName("E5: Invalid angle (15.0) rejects transition")
        void testInvalidAngleRejected() {
            Position validPos = new Position(0.0, 0.0);

            Position result = geoService.nextPosition(validPos, 15.0);  // Not a legal angle
            assertNull(result, "Invalid angle should reject transition (return null)");
        }

        @Test
        @DisplayName("E5: Negative angle rejects transition")
        void testNegativeAngleRejected() {
            Position validPos = new Position(0.0, 0.0);

            Position result = geoService.nextPosition(validPos, -22.5);
            assertNull(result, "Negative angle should reject transition");
        }

        @Test
        @DisplayName("E5: Angle > 360 rejects transition")
        void testAngleOver360Rejected() {
            Position validPos = new Position(0.0, 0.0);

            Position result = geoService.nextPosition(validPos, 360.0);
            assertNull(result, "Angle >= 360 should reject transition");
        }

        @Test
        @DisplayName("E5: Null angle rejects transition")
        void testNullAngleRejected() {
            Position validPos = new Position(0.0, 0.0);

            Position result = geoService.nextPosition(validPos, null);
            assertNull(result, "Null angle should reject transition");
        }
    }

    // TRANSITION SEQUENCES (Testing paths through state machine)
    @Nested
    @DisplayName("Transition Sequences: Multi-step state paths")
    class TransitionSequences {

        @Test
        @DisplayName("Sequence: VALID -> VALID -> VALID -> VALID (complete journey)")
        void testCompleteValidJourney() {
            Position start = new Position(-0.001, 0.0);
            Position goal = new Position(-0.001, 0.001);

            List<Position> path = geoService.findPath(start, goal, restrictedAreas);

            assertFalse(path.isEmpty(), "Path should be found");

            // Verify entire path stays in VALID state
            for (int i = 0; i < path.size() - 1; i++) {
                Position current = path.get(i);
                Position next = path.get(i + 1);

                // Each position should be valid
                assertNotNull(geoService.isInRegion(current, restrictedArea),
                        "Position should have valid coordinates");
                // Each move should be valid
                assertTrue(geoService.isValidMove(current, next, restrictedAreas),
                        "Move should stay in VALID state");
            }
        }

        @Test
        @DisplayName("Sequence: VALID -> attempt BLOCKED -> reroute VALID -> VALID (obstacle avoidance)")
        void testObstacleAvoidanceSequence() {
            // Start on one side of restricted area
            Position start = new Position(0.0, 0.0015);
            // Goal on other side (direct path would be blocked)
            Position goal = new Position(0.003, 0.0015);

            // Direct path would be blocked
            assertFalse(geoService.isValidMove(start, goal, restrictedAreas),
                    "Direct path should be BLOCKED");

            // A* should find alternate route
            List<Position> path = geoService.findPath(start, goal, restrictedAreas);
            assertFalse(path.isEmpty(), "A* should find path avoiding obstacle");

            // Path should never enter BLOCKED state
            for (Position pos : path) {
                assertFalse(geoService.isInRegion(pos, restrictedArea),
                        "Path should never enter BLOCKED state");
            }
        }

        @Test
        @DisplayName("Sequence: VALID -> (multiple angles) -> stays VALID")
        void testMultipleDirectionChanges() {
            Position current = new Position(0.0, 0.0);
            double[] angles = {0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0};

            for (double angle : angles) {
                Position next = geoService.nextPosition(current, angle);
                assertNotNull(next, "Valid angle should produce next position");

                // Reset to origin for each test (testing individual transitions)
                Boolean inRegion = geoService.isInRegion(next, restrictedArea);
                assertFalse(inRegion, "Movement in any valid angle from origin stays VALID");
            }
        }

        @Test
        @DisplayName("Sequence: OUT_OF_BOUNDS cannot transition (absorbing state)")
        void testOutOfBoundsAbsorbingState() {
            Position outOfBounds = new Position(0.0, 100.0);  // Invalid latitude

            // Cannot perform any valid operation from OUT_OF_BOUNDS
            assertNull(geoService.nextPosition(outOfBounds, 0.0),
                    "Cannot transition from OUT_OF_BOUNDS");
            assertNull(geoService.calculateDistance(outOfBounds, new Position(0, 0)),
                    "Cannot calculate distance from OUT_OF_BOUNDS");
            assertNull(geoService.isCloseTo(outOfBounds, new Position(0, 0)),
                    "Cannot check proximity from OUT_OF_BOUNDS");
        }
    }
}
