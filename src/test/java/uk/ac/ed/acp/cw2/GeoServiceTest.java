package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Position;
import uk.ac.ed.acp.cw2.data.Region;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeoServiceTest {

    private final double MOVE = 0.00015;
    private final double EPS = 1e-12;

    private final GeoService geoService = new GeoService();

    private Position pos(double lng, double lat) {
        Position p = new Position();
        p.lng = lng;
        p.lat = lat;
        return p;
    }

    private Region region(Position... positions) {
        Region r = new Region();
        r.vertices = java.util.Arrays.asList(positions);
        return r;
    }

    @Nested
    class CalculateDistanceTests {
        @Test
        void zeroDistance() {
            Position a = pos(-3.0, 56.0);
            assertEquals(0.0, geoService.calculateDistance(a, a), EPS);
        }

        @Test
        void horizontalDistance() {
            Position a = pos(-3.0, 56.0);
            Position b = pos(-4.0, 56.0);
            assertEquals(1.0, geoService.calculateDistance(a, b), EPS);
        }

        @Test
        void verticalDistance() {
            Position a = pos(-3.0, 56.0);
            Position b = pos(-3.0, 57.0);
            assertEquals(1.0, geoService.calculateDistance(a, b), EPS);
        }

        @Test
        void diagonalDistance345() {
            Position a = pos(0.0, 0.0);
            Position b = pos(3.0, 4.0);
            assertEquals(5.0, geoService.calculateDistance(a, b), EPS);
        }

        @Test
        void symmetry() {
            Position a = pos(-3.0, 56.0);
            Position b = pos(-4.0, 57.0);
            assertEquals(
                    geoService.calculateDistance(a, b),
                    geoService.calculateDistance(b, a),
                    EPS
            );
        }

        @Test
        void largeValidBounds() {
            Position a = pos(-180.0, -90.0);
            Position b = pos(180.0, 90.0);
            double expected = Math.sqrt(Math.pow(-180.0 - 180.0, 2) + Math.pow(-90.0 - 90.0, 2));
            assertEquals(expected, geoService.calculateDistance(a, b), EPS);
        }

        @Test
        void manyDecimalPlaces() {
            Position a = pos(-3.123456789, 55.987654321);
            Position b = pos(-3.000000001, 55.000000009);
            double expected = Math.sqrt(
                    Math.pow(a.lng - b.lng, 2) +
                            Math.pow(a.lat - b.lat, 2)
            );
            assertEquals(expected, geoService.calculateDistance(a, b), EPS);
        }
    }

    @Nested
    class NextPositionTests {
        @Test
        void angle0MovesEast() {
            Position start = pos(-3.0, 56.0);
            Position end = geoService.nextPosition(start, 0);
            assertEquals(start.lng + MOVE, end.lng, EPS);
            assertEquals(start.lat, end.lat, EPS);
        }

        @Test
        void angle90MovesNorth() {
            Position start = pos(-3.0, 56.0);
            Position end = geoService.nextPosition(start, 90);
            assertEquals(start.lng, end.lng, EPS);
            assertEquals(start.lat + MOVE, end.lat, EPS);
        }

        @Test
        void angle180MovesWest() {
            Position start = pos(-3.0, 56.0);
            Position end = geoService.nextPosition(start, 180);
            assertEquals(start.lng - MOVE, end.lng, EPS);
            assertEquals(start.lat, end.lat, EPS);
        }

        @Test
        void angle270MovesSouth() {
            Position start = pos(-3.0, 56.0);
            Position end = geoService.nextPosition(start, 270);
            assertEquals(start.lng, end.lng, EPS);
            assertEquals(start.lat - MOVE, end.lat, EPS);
        }

        @Test
        void diagonal45MovesCorrectly() {
            Position start = pos(-3.0, 56.0);
            Position end = geoService.nextPosition(start, 45);
            double dx = end.lng - start.lng;
            double dy = end.lat - start.lat;
            assertEquals(Math.abs(dx), Math.abs(dy), EPS);
            assertEquals(MOVE, Math.sqrt(dx*dx + dy*dy), EPS);
        }
    }

    @Nested
    class PointInPolygonTests {
        @Test
        void pointInsideSimpleSquare() {
            Region square = region(
                    pos(0,0),
                    pos(4,0),
                    pos(4,4),
                    pos(0,4),
                    pos(0,0)   // closed
            );

            Position inside = pos(2,2);
            assertTrue(geoService.pointInPolygon(inside, square.vertices));
        }

        @Test
        void pointOutsideSimpleSquare() {
            Region square = region(
                    pos(0,0),
                    pos(4,0),
                    pos(4,4),
                    pos(0,4),
                    pos(0,0)
            );

            Position outside = pos(5,5);
            assertFalse(geoService.pointInPolygon(outside, square.vertices));
        }

        @Test
        void pointOnEdgeCountsAsInside() {
            Region square = region(
                    pos(0,0),
                    pos(4,0),
                    pos(4,4),
                    pos(0,4),
                    pos(0,0)
            );

            Position onEdge = pos(2,0);
            assertTrue(geoService.pointInPolygon(onEdge, square.vertices));
        }

        @Test
        void concavePolygonInside() {
            Region concave = region(
                    pos(0,0),
                    pos(4,0),
                    pos(4,4),
                    pos(2,2), // inward dent
                    pos(0,4),
                    pos(0,0)
            );

            Position inside = pos(1,3);
            assertTrue(geoService.pointInPolygon(inside, concave.vertices));
        }

        @Test
        void concavePolygonOutsideDent() {
            Region concave = region(
                    pos(0,0),
                    pos(4,0),
                    pos(4,4),
                    pos(2,2), // inward dent
                    pos(0,4),
                    pos(0,0)
            );

            Position inDent = pos(2,3);
            assertFalse(geoService.pointInPolygon(inDent, concave.vertices));
        }
    }

    @Nested
    class findPathTests {

        @Test
        void pathWithNFZ() {
            Position start = new Position(-3.195, 55.9435);
            Position goal = new Position(-3.175, 55.940);

            Region r = new Region();
            r.name = "block";
            r.vertices = List.of(
                    new Position(-3.190578818321228, 55.94402412577528),
                    new Position(-3.1899887323379517, 55.94284650540911),
                    new Position(-3.187097311019897, 55.94328811724263),
                    new Position(-3.187682032585144, 55.944477740393744),
                    new Position(-3.190578818321228, 55.94402412577528)
            );

            List<Region> regions = List.of(r);
            List<Position> path = geoService.findPath(start, goal, regions);

            // Get the final position
            Position last = path.getLast();

            // Assert every consecutive move in the path is valid
            for (int i = 1; i < path.size(); i++) {
                Position prev = path.get(i - 1);
                Position curr = path.get(i);
                assertTrue(geoService.isValidMove(prev, curr, regions));
            }

            // Assert the last position is close to the goal
            assertTrue(geoService.isCloseTo(last, goal));
        }

    }

}



