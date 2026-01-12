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
 * Specification-Based (Black-Box) Techniques: Decision Table Testing
 *
 * This test class demonstrates Decision Table Testing for the isInRegion() method.
 * The method validates inputs and determines if a point lies within a polygon region.
 *
 * CONDITIONS:
 *   C1: Position is valid (not null, valid lat/lng range)
 *   C2: Region is not null
 *   C3: Region.vertices is not null
 *   C4: Vertices count >= 4
 *   C5: First vertex is valid
 *   C6: Last vertex is valid
 *   C7: Polygon is closed (first vertex == last vertex)
 *   C8: Point location relative to polygon
 *
 * ACTIONS:
 *   A1: Return NULL  (invalid input)
 *   A2: Return TRUE  (point inside or on boundary)
 *   A3: Return FALSE (point outside)
 *
 * I made a table in Excel, it will be in the portfolio
 */
public class GeoServiceDecisionTableTest {

    private GeoService geoService;
    private List<Position> validClosedPolygon;

    @BeforeEach
    void setUp() {
        geoService = new GeoService();
        // Valid closed square polygon: (0,0) -> (1,0) -> (1,1) -> (0,1) -> (0,0)
        validClosedPolygon = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(1.0, 0.0),
                new Position(1.0, 1.0),
                new Position(0.0, 1.0),
                new Position(0.0, 0.0)
        );
    }

    // RULE 1: Invalid Position (C1 = False)
    // When position is invalid, all other conditions are irrelevant ("-")
    @Nested
    @DisplayName("Rule 1: C1=F (Invalid Position) -> NULL")
    class Rule1_InvalidPosition {

        @Test
        @DisplayName("R1a: Position is null")
        void testPositionNull() {
            Region r = new Region();
            r.vertices = validClosedPolygon;

            Boolean result = geoService.isInRegion(null, r);
            assertNull(result, "Null position should return null");
        }

        @Test
        @DisplayName("R1b: Position latitude out of range (< -90)")
        void testPositionLatitudeTooLow() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position invalidPos = new Position(0.5, -90.1);

            Boolean result = geoService.isInRegion(invalidPos, r);
            assertNull(result, "Position with latitude < -90 should return null");
        }

        @Test
        @DisplayName("R1c: Position latitude out of range (> 90)")
        void testPositionLatitudeTooHigh() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position invalidPos = new Position(0.5, 90.1);

            Boolean result = geoService.isInRegion(invalidPos, r);
            assertNull(result, "Position with latitude > 90 should return null");
        }

        @Test
        @DisplayName("R1d: Position longitude out of range (< -180)")
        void testPositionLongitudeTooLow() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position invalidPos = new Position(-180.1, 0.0);

            Boolean result = geoService.isInRegion(invalidPos, r);
            assertNull(result, "Position with longitude < -180 should return null");
        }

        @Test
        @DisplayName("R1e: Position longitude out of range (> 180)")
        void testPositionLongitudeTooHigh() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position invalidPos = new Position(180.1, 0.0);

            Boolean result = geoService.isInRegion(invalidPos, r);
            assertNull(result, "Position with longitude > 180 should return null");
        }
    }

    // RULE 2: Null Region (C1 = True, C2 = False)
    @Nested
    @DisplayName("Rule 2: C1=T, C2=F (Null Region) -> NULL")
    class Rule2_NullRegion {

        @Test
        @DisplayName("R2: Region is null")
        void testRegionNull() {
            Position validPos = new Position(0.5, 0.5);

            Boolean result = geoService.isInRegion(validPos, null);
            assertNull(result, "Null region should return null");
        }
    }

    // RULE 3: Null Vertices (C1 = True, C2 = True, C3 = False)
    @Nested
    @DisplayName("Rule 3: C1=T, C2=T, C3=F (Null Vertices) -> NULL")
    class Rule3_NullVertices {

        @Test
        @DisplayName("R3: Region.vertices is null")
        void testVerticesNull() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = null;

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Region with null vertices should return null");
        }
    }

    // RULE 4: Insufficient Vertices (C1-C3 = True, C4 = False)
    // A valid polygon requires at least 4 vertices (3 unique + closing point)
    @Nested
    @DisplayName("Rule 4: C1-C3=T, C4=F (Vertices < 4) -> NULL")
    class Rule4_InsufficientVertices {

        @Test
        @DisplayName("R4a: Empty vertices list")
        void testEmptyVertices() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = List.of();

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Empty vertices list should return null");
        }

        @Test
        @DisplayName("R4b: Only 2 vertices")
        void testTwoVertices() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 1.0)
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Region with only 2 vertices should return null");
        }

        @Test
        @DisplayName("R4c: Only 3 vertices (minimum for polygon is 4)")
        void testThreeVertices() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(0.5, 1.0)
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Region with only 3 vertices should return null");
        }
    }

    // RULE 5: Invalid First Vertex (C1-C4 = True, C5 = False)
    @Nested
    @DisplayName("Rule 5: C1-C4=T, C5=F (Invalid First Vertex) -> NULL")
    class Rule5_InvalidFirstVertex {

        @Test
        @DisplayName("R5a: First vertex has invalid latitude")
        void testFirstVertexInvalidLatitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 91.0),  // Invalid: lat > 90
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.0, 91.0)   // Closed
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "First vertex with invalid latitude should return null");
        }

        @Test
        @DisplayName("R5b: First vertex has invalid longitude")
        void testFirstVertexInvalidLongitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(181.0, 0.0), // Invalid: lng > 180
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(181.0, 0.0)  // Closed
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "First vertex with invalid longitude should return null");
        }
    }

    // RULE 6: Invalid Last Vertex (C1-C5 = True, C6 = False)
    @Nested
    @DisplayName("Rule 6: C1-C5=T, C6=F (Invalid Last Vertex) -> NULL")
    class Rule6_InvalidLastVertex {

        @Test
        @DisplayName("R6a: Last vertex has invalid latitude")
        void testLastVertexInvalidLatitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.0, -91.0)  // Invalid: lat < -90
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Last vertex with invalid latitude should return null");
        }

        @Test
        @DisplayName("R6b: Last vertex has invalid longitude")
        void testLastVertexInvalidLongitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(-181.0, 0.0) // Invalid: lng < -180
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Last vertex with invalid longitude should return null");
        }
    }

    // RULE 7: Polygon Not Closed (C1-C6 = True, C7 = False)
    @Nested
    @DisplayName("Rule 7: C1-C6=T, C7=F (Polygon Not Closed) -> NULL")
    class Rule7_PolygonNotClosed {

        @Test
        @DisplayName("R7a: Last vertex differs from first (different longitude)")
        void testNotClosedDifferentLongitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.5, 0.0)    // Different lng from first
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Polygon with different first/last longitude should return null");
        }

        @Test
        @DisplayName("R7b: Last vertex differs from first (different latitude)")
        void testNotClosedDifferentLatitude() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.0, 0.5)    // Different lat from first
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Polygon with different first/last latitude should return null");
        }

        @Test
        @DisplayName("R7c: Last vertex differs from first (both coordinates different)")
        void testNotClosedBothDifferent() {
            Position validPos = new Position(0.5, 0.5);
            Region r = new Region();
            r.vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(1.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.5, 0.5)    // Completely different from first
            );

            Boolean result = geoService.isInRegion(validPos, r);
            assertNull(result, "Polygon with completely different first/last vertex should return null");
        }
    }

    // RULES 8-10: All Conditions Valid (C1-C7 = True)
    // Outcome determined by C8 (point location)
    @Nested
    @DisplayName("Rules 8-10: All Valid (C1-C7=T) -> Result depends on point location")
    class Rules8to10_AllValid {

        @Test
        @DisplayName("R8: Point clearly inside polygon -> TRUE")
        void testPointInside() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position inside = new Position(0.5, 0.5);

            Boolean result = geoService.isInRegion(inside, r);
            assertTrue(result, "Point inside polygon should return true");
        }

        @Test
        @DisplayName("R9a: Point clearly outside polygon -> FALSE")
        void testPointOutside() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position outside = new Position(2.0, 2.0);

            Boolean result = geoService.isInRegion(outside, r);
            assertFalse(result, "Point outside polygon should return false");
        }

        @Test
        @DisplayName("R9b: Point outside (negative coordinates) -> FALSE")
        void testPointOutsideNegative() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position outside = new Position(-1.0, -1.0);

            Boolean result = geoService.isInRegion(outside, r);
            assertFalse(result, "Point outside polygon (negative coords) should return false");
        }

        @Test
        @DisplayName("R10a: Point on bottom edge -> TRUE")
        void testPointOnBottomEdge() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position onEdge = new Position(0.5, 0.0);

            Boolean result = geoService.isInRegion(onEdge, r);
            assertTrue(result, "Point on boundary (bottom edge) should return true");
        }

        @Test
        @DisplayName("R10b: Point on right edge -> TRUE")
        void testPointOnRightEdge() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position onEdge = new Position(1.0, 0.5);

            Boolean result = geoService.isInRegion(onEdge, r);
            assertTrue(result, "Point on boundary (right edge) should return true");
        }

        @Test
        @DisplayName("R10c: Point on vertex -> TRUE")
        void testPointOnVertex() {
            Region r = new Region();
            r.vertices = validClosedPolygon;
            Position onVertex = new Position(0.0, 0.0);

            Boolean result = geoService.isInRegion(onVertex, r);
            assertTrue(result, "Point on vertex should return true");
        }
    }
}