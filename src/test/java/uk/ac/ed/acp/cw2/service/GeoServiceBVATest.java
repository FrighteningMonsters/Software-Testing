package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Position;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specification-Based (Black-Box) Techniques: Boundary Value Analysis (BVA)
 */
public class GeoServiceBVATest {

    private GeoService geoService;
    private static final double STEP = 0.00015;
    private static final double EPSILON = 1e-12;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();
    }

    @Nested
    @DisplayName("isCloseTo(): Proximity Threshold (0.00015)")
    class ProximityBoundaries {
        /*
         * Closeness defined as distance < 0.00015.
         * BVA Points:
         * - Min: 0.0 (True)
         * - Nominal: 0.000075 (True)
         * - Just Below Max: 0.00015 - EPSILON (True)
         * - Max (Boundary): 0.00015 (False)
         * - Just Above Max: 0.00015 + EPSILON (False)
         */

        @Test
        @DisplayName("BVA: Distance 0.0 (Minimum)")
        void testMin() {
            assertTrue(geoService.isCloseTo(new Position(0,0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA: Distance 0.000075 (Nominal)")
        void testNominal() {
            assertTrue(geoService.isCloseTo(new Position(0,0), new Position(0, 0.000075)));
        }

        @Test
        @DisplayName("BVA: Distance just below 0.00015")
        void testJustBelowMax() {
            assertTrue(geoService.isCloseTo(new Position(0,0), new Position(0, STEP - EPSILON)));
        }

        @Test
        @DisplayName("BVA: Distance exactly 0.00015 (Max Boundary)")
        void testMaxBoundary() {
            assertFalse(geoService.isCloseTo(new Position(0,0), new Position(0, STEP)));
        }

        @Test
        @DisplayName("BVA: Distance just above 0.00015")
        void testJustAboveMax() {
            assertFalse(geoService.isCloseTo(new Position(0,0), new Position(0, STEP + EPSILON)));
        }
    }

    @Nested
    @DisplayName("validPosition(): Longitude Boundaries [-180, 180]")
    class LongitudeBoundaries {
        /*
         * Note: validPosition() is a private utility method used for input validation across
         * several public methods (calculateDistance, isCloseTo, nextPosition, isInRegion).
         * It is tested here through calculateDistance(); if validation fails, the method returns null.
         *
         * BVA Points:
         * - Just Below Min: -90.0 - EPSILON (Returns null)
         * - Min: -90.0 (Returns distance)
         * - Nominal: 0.0 (Returns distance)
         * - Max: 90.0 (Returns distance)
         * - Just Above Max: 90.0 + EPSILON (Returns null)
         */

        @Test
        @DisplayName("BVA Lng: Just below -180")
        void testLngJustBelow() {
            assertNull(geoService.calculateDistance(new Position(-180.0 - EPSILON, 0.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lng: Exactly -180 (Min)")
        void testLngMin() {
            assertNotNull(geoService.calculateDistance(new Position(-180.0, 0.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lng: 0.0 (Nominal)")
        void testLngNominal() {
            assertNotNull(geoService.calculateDistance(new Position(0.0, 0.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lng: Exactly 180 (Max)")
        void testLngMax() {
            assertNotNull(geoService.calculateDistance(new Position(180.0, 0.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lng: Just above 180")
        void testLngJustAbove() {
            assertNull(geoService.calculateDistance(new Position(180.0 + EPSILON, 0.0), new Position(0,0)));
        }
    }

    @Nested
    @DisplayName("validPosition(): Latitude Boundaries [-90, 90]")
    class LatitudeBoundaries {
        /*
         * BVA Points:
         * - Just Below Min: -90.0 - EPSILON (Invalid)
         * - Min: -90.0 (Valid)
         * - Nominal: 0.0 (Valid)
         * - Max: 90.0 (Valid)
         * - Just Above Max: 90.0 + EPSILON (Invalid)
         */

        @Test
        @DisplayName("BVA Lat: Just below -90")
        void testLatJustBelow() {
            assertNull(geoService.calculateDistance(new Position(0, -90.0 - EPSILON), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lat: Exactly -90 (Min)")
        void testLatMin() {
            assertNotNull(geoService.calculateDistance(new Position(0, -90.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lat: 0.0 (Nominal)")
        void testLatNominal() {
            assertNotNull(geoService.calculateDistance(new Position(0, 0.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lat: Exactly 90 (Max)")
        void testLatMax() {
            assertNotNull(geoService.calculateDistance(new Position(0, 90.0), new Position(0,0)));
        }

        @Test
        @DisplayName("BVA Lat: Just above 90")
        void testLatJustAbove() {
            assertNull(geoService.calculateDistance(new Position(0, 90.0 + EPSILON), new Position(0,0)));
        }
    }

    @Nested
    @DisplayName("nextPosition(): Angle Set Boundaries")
    class AngleBoundaries {
        /*
         * Legal set: {0, 22.5, ..., 337.5}
         * BVA Points:
         * - Just Below Min: 0.0 - EPSILON (Invalid)
         * - Min: 0.0 (Valid)
         * - Max: 337.5 (Valid)
         * - Just Above Max: 337.5 + EPSILON (Invalid)
         */

        @Test
        @DisplayName("BVA Angle: Just below 0.0")
        void testAngleJustBelow() {
            assertNull(geoService.nextPosition(new Position(0,0), 0.0 - EPSILON));
        }

        @Test
        @DisplayName("BVA Angle: Exactly 0.0 (Min legal)")
        void testAngleMin() {
            assertNotNull(geoService.nextPosition(new Position(0,0), 0.0));
        }

        @Test
        @DisplayName("BVA Angle: Exactly 337.5 (Max legal)")
        void testAngleMax() {
            assertNotNull(geoService.nextPosition(new Position(0,0), 337.5));
        }

        @Test
        @DisplayName("BVA Angle: Just above 337.5")
        void testAngleJustAbove() {
            assertNull(geoService.nextPosition(new Position(0,0), 337.5 + EPSILON));
        }
    }
}