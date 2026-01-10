package uk.ac.ed.acp.cw2.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.service.DroneService;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.util.Arrays;
import java.util.List;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController()
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ServiceController {


    private final String ilpEndpoint;

    private final GeoService geoService;
    private final DroneService droneService;

    /**
     * Returns the welcome page with ILP service information.
     *
     * @return HTML string containing welcome message and service URL
     */
    @GetMapping("/")
    public String index() {
        return "<html><body>" +
                "<h1>Welcome from ILP</h1>" +
                "<h4>ILP-REST-Service-URL:</h4> <a href=\"" + ilpEndpoint + "\" target=\"_blank\"> " + ilpEndpoint + " </a>" +
                "</body></html>";
    }

    /**
     * Returns student UID.
     *
     * @return the UID string
     */
    @GetMapping("/uid")
    public String uid() {
        return "s2518554";
    }

    /**
     * Calculates the distance between two positions.
     *
     * @param request the distance request containing two positions
     * @return the calculated distance, or null if request is invalid
     */
    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@RequestBody DistanceRequest request) {
        if (request == null ||
                !validPosition(request.position1) ||
                !validPosition(request.position2)) {
            return ResponseEntity.ok(null);
        }

        double distance = geoService.calculateDistance(
                request.position1,
                request.position2
        );

        return ResponseEntity.ok(distance);
    }

    /**
     * Checks if two positions are close to each other.
     *
     * @param request the distance request containing two positions
     * @return response containing true if positions are close, false otherwise, or null if request is invalid
     */
    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@RequestBody DistanceRequest request) {
        if (request == null ||
                !validPosition(request.position1) ||
                !validPosition(request.position2)) {
            return ResponseEntity.ok(null);
        }

        return ResponseEntity.ok(geoService.isCloseTo(
                request.position1,
                request.position2
        ));
    }


    /**
     * Calculates the next position from a start position at a given angle.
     *
     * @param request the request containing start position and angle
     * @return the next position, or null if request is invalid or angle is not legal
     */
    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@RequestBody NextPositionRequest request) {
        if (request == null ||
                !validPosition(request.start) ||
                request.angle == null) {
            return ResponseEntity.ok(null);
        }

        double[] legal = {
                0,22.5,45,67.5,90,112.5,135,157.5,
                180,202.5,225,247.5,270,292.5,315,337.5
        };

        boolean ok = Arrays.stream(legal).anyMatch(a -> a == request.angle);

        if (!ok) {
            return ResponseEntity.ok(null);
        }

        Position result = geoService.nextPosition(request.start, request.angle);
        return ResponseEntity.ok(result);
    }

    /**
     * Checks if a position is inside a region.
     *
     * @param request the request containing a position and region
     * @return response containing true if position is inside the region, false otherwise, or null if request is invalid
     */
    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@RequestBody IsInRegionRequest request) {
        if (request == null ||
                !validPosition(request.position) ||
                request.region == null ||
                request.region.vertices == null) {
            return ResponseEntity.ok(null);
        }

        java.util.List<Position> vertices = request.region.vertices;
        if (vertices.size() < 4) {
            return ResponseEntity.ok(null);
        }

        // check if polygon is closed
        Position first = vertices.getFirst();
        Position last = vertices.getLast();
        if (!validPosition(first) || !validPosition(last)) {
            return ResponseEntity.ok(null);
        }

        if (!(first.lng.doubleValue() == last.lng.doubleValue()) ||
                !(first.lat.doubleValue() == last.lat.doubleValue())) {
            return ResponseEntity.ok(null);
        }

        boolean inside = geoService.pointInPolygon(request.position, vertices);
        return ResponseEntity.ok(inside);
    }

    /**
     * Retrieves a list of drone IDs that have the specified cooling capability.
     *
     * @param state the cooling capability state (true or false)
     * @return list of drone IDs with the specified cooling state
     */
    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<String>> dronesWithCooling(@PathVariable boolean state) {
        List<String> result = droneService.findDronesWithCooling(state);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the details of a drone by its ID.
     *
     * @param id the drone ID
     * @return the drone details, or 404 status if not found
     */
    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        Drone d = droneService.getDroneById(id);
        if (d == null) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(d);
    }

    /**
     * Queries drones by a single attribute-value pair.
     *
     * @param attribute the attribute name to query
     * @param value the attribute value to match
     * @return list of drone IDs matching the query
     */
    @GetMapping("queryAsPath/{attribute}/{value}")
    public ResponseEntity<List<String>> queryAsPath(
            @PathVariable String attribute,
            @PathVariable String value) {
        return ResponseEntity.ok(droneService.queryAsPath(attribute, value));
    }

    /**
     * Queries drones by multiple attributes.
     *
     * @param queries list of query attributes with operators
     * @return list of drone IDs matching all query conditions
     */
    @PostMapping("/query")
    public ResponseEntity<List<String>> query(@RequestBody List<QueryAttribute> queries) {
        return ResponseEntity.ok(droneService.query(queries));
    }

    /**
     * Queries drones available for the specified delivery records.
     *
     * @param recs list of medical dispatch records
     * @return list of drone IDs that can fulfill all delivery requirements
     */
    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(@RequestBody List<MedDispatchRec> recs) {
        return ResponseEntity.ok(droneService.queryAvailableDrones(recs));
    }

    /**
     * Calculates delivery paths for the specified medical dispatch records.
     *
     * @param recs list of medical dispatch records to process
     * @return calculated delivery paths with total cost and moves
     */
    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<CalcDeliveryPathResult> calcDeliveryPath(@RequestBody List<MedDispatchRec> recs) {
        CalcDeliveryPathResult result = droneService.calcDeliveryPath(recs);
        return ResponseEntity.ok(result);
    }

    /**
     * Calcultaes delivery paths and returns them in GeoJSON format.
     *
     * @param recs list of medical dispatch records to process
     * @return delivery paths in GeoJSON format
     */
    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<String> calcDeliveryPathAsGeoJson(
            @RequestBody List<MedDispatchRec> recs
    ) {
        String geojson = droneService.calcDeliveryPathAsGeoJson(recs);
        return ResponseEntity.ok(geojson);
    }

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
}