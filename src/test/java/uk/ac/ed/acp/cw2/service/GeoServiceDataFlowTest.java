package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Position;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structure-Based (White-Box) Techniques: Data Flow Testing
 *
 * Data Flow Testing tracks the path from where a variable is defined (def)
 * to where it is used (use). This ensures all def-use pairs are exercised.
 *
 * Terminology:
 *   - def: Variable is assigned a value
 *   - c-use: Variable used in a computation
 *   - p-use: Variable used in a predicate (condition)
 *   - def-use pair: A path from definition to use
 *
 * Target Method: nextPosition() in GeoService
 */
public class GeoServiceDataFlowTest {
    private GeoService geoService;
    private static final double EPSILON = 1e-9;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();
    }

    /**
     * DATA FLOW ANALYSIS: nextPosition() method
     *
     * Variable: newLng
     * Line 83: newLng = start.lng + Math.cos(rad) * move       [def1]
     * Line 92: if (newLng > 180.0)                             [p-use]
     * Line 93: newLng = -180.0 + (newLng - 180.0)              [def2, c-use of def1]
     * Line 94: else if (newLng < -180.0)                       [p-use]
     * Line 95: newLng = 180.0 + (newLng + 180.0)               [def3, c-use of def1]
     * Line 99: p.lng = newLng                                  [c-use]
     *
     * Def-Use Pairs for newLng:
     *   DU1: def1 (line 83) -> p-use (line 92) -> c-use (line 99)  [no wrap path]
     *   DU2: def1 (line 83) -> p-use (line 92) -> def2 (line 93) -> c-use (line 99)  [positive wrap]
     *   DU3: def1 (line 83) -> p-use (line 94) -> def3 (line 95) -> c-use (line 99)  [negative wrap]
     *
     * Variable: newLat
     * Line 84: newLat = start.lat + Math.sin(rad) * move       [def]
     * Line 87: if (newLat > 90.0 || newLat < -90.0)            [p-use]
     * Line 100: p.lat = newLat                                 [c-use]
     *
     * Def-Use Pairs for newLat:
     *   DU4: def (line 84) -> p-use (line 87) -> return null  [out of bounds]
     *   DU5: def (line 84) -> p-use (line 87) -> c-use (line 100)  [in bounds]
     *
     * Variable: rad
     * Line 81: rad = Math.toRadians(angle)                     [def]
     * Line 83: Math.cos(rad)                                   [c-use]
     * Line 84: Math.sin(rad)                                   [c-use]
     *
     * Def-Use Pairs for rad:
     *   DU6: def (line 81) -> c-use (line 83)
     *   DU7: def (line 81) -> c-use (line 84)
     *
     * Variable: move
     * Line 80: move = 0.00015                                  [def]
     * Line 83: ... * move                                      [c-use]
     * Line 84: ... * move                                      [c-use]
     *
     * Def-Use Pairs for move:
     *   DU8: def (line 80) -> c-use (line 83)
     *   DU9: def (line 80) -> c-use (line 84)
     */
    @Nested
    @DisplayName("Data Flow: newLng variable")
    class NewLngDataFlow {

        @Test
        @DisplayName("DU1: def1 -> p-use (line 92 false) -> c-use (line 99) [no wrapping]")
        void testDU1_NoWrap() {
            // Path: newLng defined at line 83, condition at 92 is FALSE, used at line 99
            // Start at lng=0, move east (angle=0) -> newLng stays in valid range
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 0.0);

            assertNotNull(result);
            // newLng = 0.0 + cos(0) * 0.00015 = 0.00015
            assertEquals(0.00015, result.lng, EPSILON,
                    "DU1: newLng should flow from def1 directly to c-use without wrapping");
        }

        @Test
        @DisplayName("DU2: def1 -> p-use (line 92 true) -> def2 -> c-use (line 99) [positive wrap]")
        void testDU2_PositiveWrap() {
            // Path: newLng defined at line 83, condition at 92 is TRUE, redefined at 93, used at 99
            // Start near lng=180, move east -> triggers positive wrap
            Position start = new Position(179.99990, 0.0);
            Position result = geoService.nextPosition(start, 0.0);

            assertNotNull(result);
            // newLng = 179.99990 + 0.00015 = 180.00005 (triggers wrap)
            // wrapped = -180.0 + (180.00005 - 180.0) = -179.99995
            assertTrue(result.lng < 0,
                    "DU2: newLng should be redefined by wrap logic (def2) before final use");
            assertEquals(-179.99995, result.lng, EPSILON);
        }

        @Test
        @DisplayName("DU3: def1 -> p-use (line 94 true) -> def3 -> c-use (line 99) [negative wrap]")
        void testDU3_NegativeWrap() {
            // Path: newLng defined at line 83, condition at 94 is TRUE, redefined at 95, used at 99
            // Start near lng=-180, move west (angle=180) -> triggers negative wrap
            Position start = new Position(-179.99990, 0.0);
            Position result = geoService.nextPosition(start, 180.0);

            assertNotNull(result);
            // newLng = -179.99990 + cos(180) * 0.00015 = -179.99990 - 0.00015 = -180.00005
            // wrapped = 180.0 + (-180.00005 + 180.0) = 179.99995
            assertTrue(result.lng > 0,
                    "DU3: newLng should be redefined by wrap logic (def3) before final use");
            assertEquals(179.99995, result.lng, EPSILON);
        }
    }

    @Nested
    @DisplayName("Data Flow: newLat variable")
    class NewLatDataFlow {

        @Test
        @DisplayName("DU4: def -> p-use (line 87 true) -> return null [exceeds upper bound]")
        void testDU4_ExceedsUpperBound() {
            // Path: newLat defined at line 84, condition at 87 is TRUE -> return null
            // newLat never reaches c-use at line 100
            Position start = new Position(0.0, 89.99995);
            Position result = geoService.nextPosition(start, 90.0);  // move north

            assertNull(result,
                    "DU4: newLat p-use at line 87 should trigger early return (def killed)");
        }

        @Test
        @DisplayName("DU4: def -> p-use (line 87 true) -> return null [exceeds lower bound]")
        void testDU4_ExceedsLowerBound() {
            // Path: newLat defined at line 84, condition at 87 is TRUE -> return null
            Position start = new Position(0.0, -89.99995);
            Position result = geoService.nextPosition(start, 270.0);  // move south

            assertNull(result,
                    "DU4: newLat p-use at line 87 should trigger early return (def killed)");
        }

        @Test
        @DisplayName("DU5: def -> p-use (line 87 false) -> c-use (line 100) [in bounds]")
        void testDU5_InBounds() {
            // Path: newLat defined at line 84, condition at 87 is FALSE, used at line 100
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 90.0);  // move north

            assertNotNull(result);
            // newLat = 0.0 + sin(90) * 0.00015 = 0.00015
            assertEquals(0.00015, result.lat, EPSILON,
                    "DU5: newLat should flow from def through p-use to c-use");
        }
    }

    @Nested
    @DisplayName("Data Flow: rad variable")
    class RadDataFlow {

        @Test
        @DisplayName("DU6: def (line 81) -> c-use in cos() (line 83)")
        void testDU6_RadToCos() {
            // Verify rad is correctly used in cos calculation
            // angle=0 -> rad=0 -> cos(0)=1 -> newLng increases by full step
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 0.0);

            assertNotNull(result);
            assertEquals(0.00015, result.lng, EPSILON,
                    "DU6: rad should flow to cos() calculation affecting newLng");
        }

        @Test
        @DisplayName("DU7: def (line 81) -> c-use in sin() (line 84)")
        void testDU7_RadToSin() {
            // Verify rad is correctly used in sin calculation
            // angle=90 -> rad=pi/2 -> sin(pi/2)=1 -> newLat increases by full step
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 90.0);

            assertNotNull(result);
            assertEquals(0.00015, result.lat, EPSILON,
                    "DU7: rad should flow to sin() calculation affecting newLat");
        }

        @Test
        @DisplayName("DU6+DU7: rad flows to both cos() and sin() [diagonal movement]")
        void testDU6AndDU7_DiagonalMovement() {
            // angle=45 -> rad=pi/4 -> cos=sin=sqrt(2/2) -> both lng and lat change equally
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 45.0);

            assertNotNull(result);
            double expected = 0.00015 * Math.cos(Math.toRadians(45.0));
            assertEquals(expected, result.lng, EPSILON,
                    "DU6: rad used in cos() for longitude");
            assertEquals(expected, result.lat, EPSILON,
                    "DU7: rad used in sin() for latitude");
        }
    }

    @Nested
    @DisplayName("Data Flow: move variable")
    class MoveDataFlow {

        @Test
        @DisplayName("DU8: def (line 80) -> c-use (line 83) [affects newLng]")
        void testDU8_MoveToNewLng() {
            // move=0.00015 is used to calculate newLng
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 0.0);  // due east

            assertNotNull(result);
            // Verify move constant (0.00015) was used in calculation
            assertEquals(0.00015, result.lng - start.lng, EPSILON,
                    "DU8: move should be used in newLng calculation");
        }

        @Test
        @DisplayName("DU9: def (line 80) -> c-use (line 84) [affects newLat]")
        void testDU9_MoveToNewLat() {
            // move=0.00015 is used to calculate newLat
            Position start = new Position(0.0, 0.0);
            Position result = geoService.nextPosition(start, 90.0);  // due north

            assertNotNull(result);
            // Verify move constant (0.00015) was used in calculation
            assertEquals(0.00015, result.lat - start.lat, EPSILON,
                    "DU9: move should be used in newLat calculation");
        }
    }

    /**
     * ALL-DEFS Coverage Summary
     * This test class achieves all-defs coverage by ensuring every variable
     * definition reaches at least one use:
     *
     * | Variable | Definition | Reaching Use(s) | Test(s) |
     * |----------|------------|-----------------|---------|
     * | move     | line 80    | lines 83, 84    | DU8, DU9 |
     * | rad      | line 81    | lines 83, 84    | DU6, DU7 |
     * | newLng   | line 83    | lines 92, 99    | DU1 |
     * | newLng   | line 93    | line 99         | DU2 |
     * | newLng   | line 95    | line 99         | DU3 |
     * | newLat   | line 84    | lines 87, 100   | DU4, DU5 |
     *
     * ALL-USES Coverage Summary
     * Every use of each variable is reached from its definition:
     *
     * | Variable | Use        | Type   | Covered By |
     * |----------|------------|--------|------------|
     * | move     | line 83    | c-use  | DU8 |
     * | move     | line 84    | c-use  | DU9 |
     * | rad      | line 83    | c-use  | DU6 |
     * | rad      | line 84    | c-use  | DU7 |
     * | newLng   | line 92    | p-use  | DU1, DU2 |
     * | newLng   | line 93    | c-use  | DU2 |
     * | newLng   | line 94    | p-use  | DU1, DU3 |
     * | newLng   | line 95    | c-use  | DU3 |
     * | newLng   | line 99    | c-use  | DU1, DU2, DU3 |
     * | newLat   | line 87    | p-use  | DU4, DU5 |
     * | newLat   | line 100   | c-use  | DU5 |
     */
    @Nested
    @DisplayName("Data Flow: Complete Path Coverage")
    class CompletePathCoverage {

        @Test
        @DisplayName("All-defs: Every definition reaches at least one use")
        void testAllDefs() {
            // This test exercises all definitions in a single execution path
            // Path: start -> angle valid -> compute rad, move -> compute newLng, newLat
            //       -> newLat in bounds -> newLng in bounds -> return position
            Position start = new Position(10.0, 20.0);
            Position result = geoService.nextPosition(start, 45.0);

            assertNotNull(result, "All variable definitions should reach their uses");
            assertNotEquals(start.lng, result.lng, "newLng def reached use");
            assertNotEquals(start.lat, result.lat, "newLat def reached use");
        }
    }
}
