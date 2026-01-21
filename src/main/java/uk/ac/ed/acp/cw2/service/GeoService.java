package uk.ac.ed.acp.cw2.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.Node;
import uk.ac.ed.acp.cw2.data.Position;
import uk.ac.ed.acp.cw2.data.Region;

import java.util.*;

@Service
public class GeoService {
    private static final double STEP = 0.00015;
    private static final double[] ANGLES = {
            0,22.5,45,67.5,90,112.5,135,157.5,
            180,202.5,225,247.5,270,292.5,315,337.5
    };

    /**
     * Validates if a position has valid latitude and longitude values.
     *
     * @param p the position to validate
     * @return true if position is valid, false otherwise
     */
    private boolean validPosition(Position p) {
        if (p == null) return false;
        if (p.lat == null || p.lng == null) return false;
        return p.lat >= -90.0 && p.lat <= 90.0 &&
               p.lng >= -180.0 && p.lng <= 180.0;
    }

    /**
     * Calculates the Euclidean distance between two positions.
     *
     * @param p1 the first position
     * @param p2 the second position
     * @return the distance between the two positions, or null if positions are invalid
     */
    public Double calculateDistance(Position p1, Position p2) {
        if (!validPosition(p1) || !validPosition(p2)) {
            return null;
        }
        double dx = p1.lng - p2.lng;
        double dy = p1.lat - p2.lat;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Checks if two positions are within close proximity (0.00015 units).
     *
     * @param p1 the first position
     * @param p2 the second position
     * @return true if positions are close, false otherwise, or null if positions are invalid
     */
    public Boolean isCloseTo(Position p1, Position p2) {
        if (!validPosition(p1) || !validPosition(p2)) {
            return null;
        }
        double threshold = 0.00015;
        return calculateDistance(p1, p2) < threshold;
    }

    /**
     * Calculates the next position from a start position moving in the specified angle.
     * Wraps longitude around at ±180, clamps latitude at ±90.
     *
     * @param start the starting position
     * @param angle the direction angle in degrees (must be one of the 16 legal angles)
     * @return the next position after moving 0.00015 units in the specified direction, or null if inputs are invalid
     */
    public Position nextPosition(Position start, Double angle) {
        if (!validPosition(start) || angle == null) {
            return null;
        }

        boolean isLegalAngle = Arrays.stream(ANGLES).anyMatch(a -> a == angle);
        if (!isLegalAngle) {
            return null;
        }

        double move = 0.00015;
        double rad = Math.toRadians(angle);

        double newLng = start.lng + Math.cos(rad) * move;
        double newLat = start.lat + Math.sin(rad) * move;

        // Latitude out of bounds - cannot move past poles
        if (newLat > 90.0 || newLat < -90.0) {
            return null;
        }

        // Wrap longitude: if > 180, wrap to negative; if < -180, wrap to positive
        if (newLng > 180.0) {
            newLng = -180.0 + (newLng - 180.0);
        } else if (newLng < -180.0) {
            newLng = 180.0 + (newLng + 180.0);
        }

        Position p = new Position();
        p.lng = newLng;
        p.lat = newLat;
        return p;
    }

    /**
     * Determines if a point is inside a polygon using the ray casting algorithm.
     *
     * @param point the position to check
     * @param polygon the list of vertices defining the polygon boundary
     * @return true if the point is inside the polygon, false otherwise
     */
    public boolean pointInPolygon(Position point, java.util.List<Position> polygon) {
        // This was adapted from https://www.geeksforgeeks.org/dsa/how-to-check-if-a-given-point-lies-inside-a-polygon/
        int numVertices = polygon.size();
        double x = point.lng;
        double y = point.lat;
        boolean inside = false;

        Position p1 = polygon.getFirst();
        Position p2;

        for (int i = 0; i <= numVertices; i++) {
            p2 = polygon.get(i % numVertices);
            double x1 = p1.lng, y1 = p1.lat;
            double x2 = p2.lng, y2 = p2.lat;

            if (pointOnSegment(x, y, x1, y1, x2, y2)) {
                return true;
            }

            if (y > Math.min(p1.lat, p2.lat)) {
                if (y <= Math.max(p1.lat, p2.lat)) {
                    if (x <= Math.max(p1.lng, p2.lng)) {
                        double xIntersection =
                                (y - p1.lat) * (p2.lng - p1.lng)
                                / (p2.lat - p1.lat)
                                + p1.lng;

                        if (p1.lng.equals(p2.lng)
                        || x <= xIntersection) {
                            inside = !inside;
                        }
                    }
                }
            }
            p1 = p2;
        }
        return inside;
    }

    /**
     * Checks if a position is inside a region with full validation.
     *
     * @param position the position to check
     * @param region the region to check against
     * @return true if position is inside the region, false if outside, or null if inputs are invalid
     */
    public Boolean isInRegion(Position position, Region region) {
        if (!validPosition(position)) {
            return null;
        }

        if (region == null || region.vertices == null) {
            return null;
        }

        List<Position> vertices = region.vertices;
        if (vertices.size() < 4) {
            return null;
        }

        Position first = vertices.getFirst();
        Position last = vertices.getLast();

        if (!validPosition(first) || !validPosition(last)) {
            return null;
        }

        // Check if polygon is closed
        if (!(first.lng.doubleValue() == last.lng.doubleValue()) ||
                !(first.lat.doubleValue() == last.lat.doubleValue())) {
            return null;
        }

        return pointInPolygon(position, vertices);
    }

    /**
     * Checks if a point lies on a line segment.
     *
     * @param px the x-coordinate of the point
     * @param py the y-coordinate of the point
     * @param x1 the x-coordinate of the first endpoint
     * @param y1 the y-coordinate of the first endpoint
     * @param x2 the x-coordinate of the second endpoint
     * @param y2 the y-coordinate of the second endpoint
     * @return true if the point is on the segment, false otherwise
     */
    private boolean pointOnSegment(double px, double py,
                                   double x1, double y1,
                                   double x2, double y2) {

        double cross = (py - y1) * (x2 - x1) - (px - x1) * (y2 - y1);
        if (Math.abs(cross) > 1e-12) {
            return false; // not collinear
        }

        double dot = (px - x1) * (px - x2) + (py - y1) * (py - y2);
        return dot <= 0; // between endpoints
    }

    /**
     * Validates if a move from start to end position is legal, checking against forbidden regions.
     *
     * @param start the starting position
     * @param end the ending position
     * @param regions list of forbidden regions to avoid
     * @return true if the move is valid, false if it intersects a forbidden region
     */
    public boolean isValidMove(Position start, Position end, List<Region> regions) {
        final int NUM_POINTS = 100;

        for (Region r : regions) {
            if (r.vertices == null || r.vertices.size() < 3) continue;

            // 1. Check the endpoint first (fast exit for the most common failure)
            if (pointInPolygon(end, r.vertices)) {
                return false;
            }

            // 2. Check points along the segment
            for (int i = 1; i < NUM_POINTS; i++) { // Changed to < NUM_POINTS since endpoint is already checked
                double t = i / (double) NUM_POINTS;

                Position sample = new Position();
                sample.lng = start.lng + t * (end.lng - start.lng);
                sample.lat = start.lat + t * (end.lat - start.lat);

                if (pointInPolygon(sample, r.vertices)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Calculates the heuristic estimate for A* pathfinding.
     *
     * @param start the starting position
     * @param end the goal position
     * @return the estimated cost to reach the goal
     */
    private double heuristic(Position start, Position end) {
        Double distance = calculateDistance(start, end);
        if (distance == null) return Double.MAX_VALUE;
        return distance / STEP;
    }

    /**
     * Finds an optimal path from start to goal position using A* algorithm while avoiding forbidden regions.
     *
     * @param start the starting position
     * @param goal the goal position
     * @param regions list of forbidden regions to avoid
     * @return list of positions representing the path, or empty list if no path exists
     */
    public List<Position> findPath(Position start, Position goal, List<Region> regions) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));

        Set<Node> closed = new HashSet<>();

        Node s = new Node(start);
        s.g = 0.0;
        s.f = heuristic(start, goal);
        open.add(s);

        List<Integer> recents = new ArrayList<>();

        while (!open.isEmpty()) {
            Node u = open.poll();
            if (closed.contains(u)) continue;
            closed.add(u);

            recents.add(u.position.hashCode());
            if (recents.size() >= 10) {
                recents.removeFirst();
            }

            if (Boolean.TRUE.equals(isCloseTo(u.position, goal))) {
                return reconstruct(u);
            }

            for (Node v : neighbors(u)) {
                if (closed.contains(v)) continue;
                if (!isValidMove(u.position, v.position, regions)) continue;
                if (recents.contains(v.position.hashCode())) {
                    continue;
                }

                double tentative = u.g + STEP;
                if (tentative < v.g) {
                    v.parent = u;
                    v.g = tentative;
                    v.f = tentative + heuristic(v.position, goal);
                    open.add(v);
                }
            }
        }
        return List.of();
    }

    /**
     * Generates all neighboring nodes from the given node by exploring all possible movement angles.
     *
     * @param u the current node
     * @return list of neighboring nodes
     */
    private List<Node> neighbors(Node u) {
        List<Node> neighbors = new ArrayList<>(ANGLES.length);
        for (double angle : ANGLES) {
            Position np = nextPosition(u.position, angle);
            if (np != null) {
                neighbors.add(new Node(np));
            }
        }
        return neighbors;
    }

    /**
     * Reconstructs the path from the end node by following parent references back to the start.
     *
     * @param end the final node in the path
     * @return list of positions representing the complete path
     */
    private List<Position> reconstruct(Node end) {
        List<Position> path = new LinkedList<>();
        Node current = end;
        while (current != null) {
            path.addFirst(current.position);
            current = current.parent;
        }
        return path;
    }
}