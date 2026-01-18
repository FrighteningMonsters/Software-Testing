package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Position;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structure-Based (White-Box) Techniques: Modified Condition/Decision Coverage (MC/DC)
 *
 * MC/DC requires that each condition in a decision independently affects the outcome.
 * For each condition, we need to test pairs where:
 * 1. Only that condition changes value
 * 2. The overall decision result changes
 *
 * This demonstrates that each condition has an independent effect on the decision.
 */
public class GeoServiceMCDCTest {

    private GeoService geoService;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();
    }

    /**
     * Target: validPosition() method (tested through calculateDistance)
     *
     * Decision: p.lat >= -90.0 && p.lat <= 90.0 && p.lng >= -180.0 && p.lng <= 180.0
     *
     * Conditions:
     *   A: p.lat >= -90.0
     *   B: p.lat <= 90.0
     *   C: p.lng >= -180.0
     *   D: p.lng <= 180.0
     *
     * Truth Table for MC/DC:
     * ---------------------------------
     * | Test | A | B | C | D | Result |
     * ---------------------------------
     * | T1   | T | T | T | T | TRUE   |  (baseline: all conditions true)
     * | T2   | F | T | T | T | FALSE  |  (only A differs from T1)
     * | T3   | T | F | T | T | FALSE  |  (only B differs from T1)
     * | T4   | T | T | F | T | FALSE  |  (only C differs from T1)
     * | T5   | T | T | T | F | FALSE  |  (only D differs from T1)
     * ---------------------------------
     *
     * Independence Pairs:
     *   - Condition A: T1 vs T2 (A changes T->F, result changes TRUE->FALSE)
     *   - Condition B: T1 vs T3 (B changes T->F, result changes TRUE->FALSE)
     *   - Condition C: T1 vs T4 (C changes T->F, result changes TRUE->FALSE)
     *   - Condition D: T1 vs T5 (D changes T->F, result changes TRUE->FALSE)
     */
    @Nested
    @DisplayName("MC/DC: validPosition() range check (4-way AND)")
    class ValidPositionRangeCheckMCDC {

        @Test
        @DisplayName("T1: All conditions TRUE -> Decision TRUE (baseline)")
        void testT1_AllTrue() {
            // A=T (0 >= -90), B=T (0 <= 90), C=T (0 >= -180), D=T (0 <= 180)
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(0.0, 0.0);
            assertNotNull(geoService.calculateDistance(p1, p2),
                    "Valid position should return distance (not null)");
        }

        @Test
        @DisplayName("T2: Only A=FALSE -> Decision FALSE (proves A independently affects outcome)")
        void testT2_OnlyAFalse() {
            // A=F (lat=-91 violates >= -90), B=T, C=T, D=T
            // Independence pair with T1: only A changed, result changed
            Position p1 = new Position(0.0, -91.0);  // lat < -90
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "lat < -90 should invalidate position");
        }

        @Test
        @DisplayName("T3: Only B=FALSE -> Decision FALSE (proves B independently affects outcome)")
        void testT3_OnlyBFalse() {
            // A=T, B=F (lat=91 violates <= 90), C=T, D=T
            // Independence pair with T1: only B changed, result changed
            Position p1 = new Position(0.0, 91.0);  // lat > 90
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "lat > 90 should invalidate position");
        }

        @Test
        @DisplayName("T4: Only C=FALSE -> Decision FALSE (proves C independently affects outcome)")
        void testT4_OnlyCFalse() {
            // A=T, B=T, C=F (lng=-181 violates >= -180), D=T
            // Independence pair with T1: only C changed, result changed
            Position p1 = new Position(-181.0, 0.0);  // lng < -180
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "lng < -180 should invalidate position");
        }

        @Test
        @DisplayName("T5: Only D=FALSE -> Decision FALSE (proves D independently affects outcome)")
        void testT5_OnlyDFalse() {
            // A=T, B=T, C=T, D=F (lng=181 violates <= 180)
            // Independence pair with T1: only D changed, result changed
            Position p1 = new Position(181.0, 0.0);  // lng > 180
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "lng > 180 should invalidate position");
        }
    }

    /**
     * Target: validPosition() null check
     *
     * Decision: p.lat == null || p.lng == null
     *
     * Conditions:
     *   A: p.lat == null
     *   B: p.lng == null
     *
     * Truth Table for MC/DC:
     * -------------------------
     * | Test | A | B | Result |
     * -------------------------
     * | T1   | F | F | FALSE  |  (neither null - passes check)
     * | T2   | T | F | TRUE   |  (lat null - fails check)
     * | T3   | F | T | TRUE   |  (lng null - fails check)
     * ------------------------
     *
     * Independence Pairs:
     *   - Condition A: T1 vs T2 (A changes F->T, result changes FALSE->TRUE)
     *   - Condition B: T1 vs T3 (B changes F->T, result changes FALSE->TRUE)
     */
    @Nested
    @DisplayName("MC/DC: validPosition() null check (2-way OR)")
    class ValidPositionNullCheckMCDC {

        @Test
        @DisplayName("T1: A=FALSE, B=FALSE -> Decision FALSE (baseline - position valid)")
        void testT1_NeitherNull() {
            // A=F (lat not null), B=F (lng not null) -> passes null check
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(0.0, 0.0);
            assertNotNull(geoService.calculateDistance(p1, p2),
                    "Non-null lat/lng should pass validation");
        }

        @Test
        @DisplayName("T2: A=TRUE, B=FALSE -> Decision TRUE (proves A independently affects outcome)")
        void testT2_OnlyLatNull() {
            // A=T (lat is null), B=F (lng not null)
            // Independence pair with T1: only A changed, result changed
            Position p1 = new Position(0.0, null);  // lat is null
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "null lat should invalidate position");
        }

        @Test
        @DisplayName("T3: A=FALSE, B=TRUE -> Decision TRUE (proves B independently affects outcome)")
        void testT3_OnlyLngNull() {
            // A=F (lat not null), B=T (lng is null)
            // Independence pair with T1: only B changed, result changed
            Position p1 = new Position(null, 0.0);  // lng is null
            Position p2 = new Position(0.0, 0.0);
            assertNull(geoService.calculateDistance(p1, p2),
                    "null lng should invalidate position");
        }
    }

    /**
     * Target: calculateDistance() input validation
     *
     * Decision: !validPosition(p1) || !validPosition(p2)
     *
     * Conditions:
     *   A: !validPosition(p1)  (p1 is invalid)
     *   B: !validPosition(p2)  (p2 is invalid)
     *
     * Truth Table for MC/DC:
     * -------------------------
     * | Test | A | B | Result |
     * -------------------------
     * | T1   | F | F | FALSE  |  (both valid - returns distance)
     * | T2   | T | F | TRUE   |  (p1 invalid - returns null)
     * | T3   | F | T | TRUE   |  (p2 invalid - returns null)
     * -------------------------
     *
     * Independence Pairs:
     *   - Condition A: T1 vs T2 (A changes F->T, result changes)
     *   - Condition B: T1 vs T3 (B changes F->T, result changes)
     */
    @Nested
    @DisplayName("MC/DC: calculateDistance() input validation (2-way OR)")
    class CalculateDistanceValidationMCDC {

        @Test
        @DisplayName("T1: Both positions valid -> returns distance")
        void testT1_BothValid() {
            Position p1 = new Position(10.0, 20.0);
            Position p2 = new Position(30.0, 40.0);
            assertNotNull(geoService.calculateDistance(p1, p2),
                    "Two valid positions should return a distance");
        }

        @Test
        @DisplayName("T2: Only p1 invalid -> returns null (proves p1 validation matters)")
        void testT2_OnlyP1Invalid() {
            // Independence pair with T1: only p1 validity changed
            Position p1 = new Position(200.0, 20.0);  // invalid lng
            Position p2 = new Position(30.0, 40.0);   // valid
            assertNull(geoService.calculateDistance(p1, p2),
                    "Invalid p1 should cause null return");
        }

        @Test
        @DisplayName("T3: Only p2 invalid -> returns null (proves p2 validation matters)")
        void testT3_OnlyP2Invalid() {
            // Independence pair with T1: only p2 validity changed
            Position p1 = new Position(10.0, 20.0);   // valid
            Position p2 = new Position(30.0, 100.0);  // invalid lat
            assertNull(geoService.calculateDistance(p1, p2),
                    "Invalid p2 should cause null return");
        }
    }

    /**
     * Target: nextPosition() validation
     *
     * Decision: !validPosition(start) || angle == null
     *
     * Conditions:
     *   A: !validPosition(start)  (start position is invalid)
     *   B: angle == null
     *
     * Truth Table for MC/DC:
     * -------------------------
     * | Test | A | B | Result |
     * -------------------------
     * | T1   | F | F | FALSE  |  (valid start, non-null angle - proceeds)
     * | T2   | T | F | TRUE   |  (invalid start - returns null)
     * | T3   | F | T | TRUE   |  (null angle - returns null)
     * -------------------------
     *
     * Independence Pairs:
     *   - Condition A: T1 vs T2
     *   - Condition B: T1 vs T3
     */
    @Nested
    @DisplayName("MC/DC: nextPosition() input validation (2-way OR)")
    class NextPositionValidationMCDC {

        @Test
        @DisplayName("T1: Valid start, non-null angle -> returns position")
        void testT1_BothValid() {
            Position start = new Position(0.0, 0.0);
            assertNotNull(geoService.nextPosition(start, 0.0),
                    "Valid inputs should return a position");
        }

        @Test
        @DisplayName("T2: Invalid start, non-null angle -> returns null (proves start validation matters)")
        void testT2_InvalidStart() {
            Position start = new Position(0.0, -100.0);  // invalid lat
            assertNull(geoService.nextPosition(start, 0.0),
                    "Invalid start should return null");
        }

        @Test
        @DisplayName("T3: Valid start, null angle -> returns null (proves angle null check matters)")
        void testT3_NullAngle() {
            Position start = new Position(0.0, 0.0);
            assertNull(geoService.nextPosition(start, null),
                    "Null angle should return null");
        }
    }

    /**
     * Target: nextPosition() latitude boundary check
     *
     * Decision: newLat > 90.0 || newLat < -90.0
     *
     * Conditions:
     *   A: newLat > 90.0
     *   B: newLat < -90.0
     *
     * Truth Table for MC/DC:
     * -------------------------
     * | Test | A | B | Result |
     * -------------------------
     * | T1   | F | F | FALSE  |  (lat in bounds - returns position)
     * | T2   | T | F | TRUE   |  (lat > 90 - returns null)
     * | T3   | F | T | TRUE   |  (lat < -90 - returns null)
     * -------------------------
     *
     * Independence Pairs:
     *   - Condition A: T1 vs T2
     *   - Condition B: T1 vs T3
     */
    @Nested
    @DisplayName("MC/DC: nextPosition() latitude boundary (2-way OR)")
    class NextPositionLatBoundaryMCDC {

        @Test
        @DisplayName("T1: Resulting lat in bounds -> returns position")
        void testT1_LatInBounds() {
            // Moving from lat=0 at angle 90 (north) won't exceed bounds
            Position start = new Position(0.0, 0.0);
            assertNotNull(geoService.nextPosition(start, 90.0),
                    "Movement within bounds should succeed");
        }

        @Test
        @DisplayName("T2: Resulting lat > 90 -> returns null (proves upper bound check matters)")
        void testT2_LatExceedsUpper() {
            // Start very close to north pole, move north
            Position start = new Position(0.0, 89.99999);
            assertNull(geoService.nextPosition(start, 90.0),
                    "Movement past north pole should return null");
        }

        @Test
        @DisplayName("T3: Resulting lat < -90 -> returns null (proves lower bound check matters)")
        void testT3_LatExceedsLower() {
            // Start very close to south pole, move south
            Position start = new Position(0.0, -89.99999);
            assertNull(geoService.nextPosition(start, 270.0),
                    "Movement past south pole should return null");
        }
    }
}
