package uk.ac.ed.acp.cw2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DroneService {
    private final RestTemplate rest;
    private final GeoService geoService;

    private final String ilpEndpoint;

    /**
     * Finds all drones with a specific cooling capability state.
     *
     * @param state the cooling capability state to filter by
     * @return list of drone IDs matching the cooling state
     */
    public List<String> findDronesWithCooling(Boolean state) {
        if (state == null) {
            return List.of();
        }

        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        if (drones == null) return List.of();

        return Arrays.stream(drones)
                .filter(drone -> drone.capability != null)
                .filter(drone -> drone.capability.cooling == state)
                .map(drone -> drone.id)
                .toList();
    }

    /**
     * Retrieves a drone by its ID.
     *
     * @param id the drone ID to search for
     * @return the drone with the specified ID, or null if not found or id is invalid
     */
    public Drone getDroneById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        if (drones == null || drones.length == 0) return null;

        return Arrays.stream(drones)
                .filter(drone -> drone.id != null && drone.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Queries drones by a single attribute and value.
     *
     * @param attribute the attribute name to filter by
     * @param value the value to match
     * @return list of drone IDs matching the attribute and value
     */
    public List<String> queryAsPath(String attribute, String value) {
        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        if (drones == null) return List.of();

        return Arrays.stream(drones)
                .filter(drone -> matchSingleAttribute(drone, attribute, value))
                .map(drone -> drone.id)
                .toList();
    }

    /**
     * Checks if a drone matches a single attribute-value pair.
     *
     * @param drone the drone to check
     * @param attribute the attribute name
     * @param value the value to match
     * @return true if the drone matches the attribute and value
     */
    private boolean matchSingleAttribute(Drone drone, String attribute, String value) {

        Capability c = drone.capability;

        return switch (attribute) {

            case "id" -> drone.id != null && drone.id.equals(value);

            case "name" -> drone.name != null && drone.name.equals(value);

            case "cooling" -> {
                if (c == null) yield false;
                boolean v = Boolean.parseBoolean(value);
                yield c.cooling == v;
            }

            case "heating" -> {
                if (c == null) yield false;
                boolean v = Boolean.parseBoolean(value);
                yield c.heating == v;
            }

            case "capacity" -> {
                if (c == null) yield false;
                try {
                    double v = Double.parseDouble(value);
                    yield c.capacity == v;
                } catch (Exception e) { yield false; }
            }

            case "maxMoves" -> {
                if (c == null) yield false;
                try {
                    int v = Integer.parseInt(value);
                    yield c.maxMoves == v;
                } catch (Exception e) { yield false; }
            }

            case "costPerMove" -> {
                if (c == null) yield false;
                try {
                    double v = Double.parseDouble(value);
                    yield c.costPerMove == v;
                } catch (Exception e) { yield false; }
            }

            case "costInitial" -> {
                if (c == null) yield false;
                try {
                    double v = Double.parseDouble(value);
                    yield c.costInitial == v;
                } catch (Exception e) { yield false; }
            }

            case "costFinal" -> {
                if (c == null) yield false;
                try {
                    double v = Double.parseDouble(value);
                    yield c.costFinal == v;
                } catch (Exception e) { yield false; }
            }

            default -> false;
        };
    }

    /**
     * Queries drones using multiple attribute filters with operators.
     *
     * @param queries list of query attributes with operators
     * @return list of drone IDs matching all query conditions
     */
    public List<String> query(List<QueryAttribute> queries) {
        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);

        if (drones == null) return List.of();

        List<QueryAttribute> valid = queries.stream()
                .filter(this::isValidQuery)
                .toList();

        return Arrays.stream(drones)
                .filter(drone -> matchesAll(drone, valid))
                .map(drone -> drone.id)
                .toList();
    }

    /**
     * Validates a query attribute for basic structural correctness.
     * Checks that all required fields are present and non-blank.
     * Value type validation is handled in matchQueryAttribute based on attribute type.
     *
     * @param query the query attribute to validate
     * @return true if the query has valid structure
     */
    private boolean isValidQuery(QueryAttribute query) {
        if (query == null) return false;
        if (query.attribute == null || query.operator == null || query.value == null) return false;
        return !query.attribute.isBlank() && !query.operator.isBlank() && !query.value.isBlank();
    }

    /**
     * Checks if a drone matches a single query attribute with an operator.
     *
     * @param drone the drone to check
     * @param queryAttribute the query attribute containing attribute name, operator, and value
     * @return true if the drone matches the query condition
     */
    private boolean matchQueryAttribute(Drone drone, QueryAttribute queryAttribute) {
        String attribute = queryAttribute.attribute;
        String operator = queryAttribute.operator;
        String value = queryAttribute.value;

        Capability c = drone.capability;

        return switch (attribute) {
            case "id" -> stringCompare(drone.id, operator, value);

            case "name" -> stringCompare(drone.name, operator, value);

            case "cooling" -> {
                if (c == null) yield false;
                boolean rhs = Boolean.parseBoolean(value);
                yield booleanCompare(c.cooling, operator, rhs);
            }

            case "heating" -> {
                if (c == null) yield false;
                boolean rhs = Boolean.parseBoolean(value);
                yield booleanCompare(c.heating, operator, rhs);
            }

            case "capacity" -> {
                if (c == null) yield false;
                try {
                    double rhs = Double.parseDouble(value);
                    yield numericCompare(c.capacity, operator, rhs);
                } catch (Exception e) {
                    yield false;
                }
            }

            case "maxMoves" -> {
                if (c == null) yield false;
                try {
                    double rhs = Double.parseDouble(value);
                    yield numericCompare(c.maxMoves, operator, rhs);
                } catch (Exception e) {
                    yield false;
                }
            }

            case "costPerMove" -> {
                if (c == null) yield false;
                try {
                    double rhs = Double.parseDouble(value);
                    yield numericCompare(c.costPerMove, operator, rhs);
                } catch (Exception e) {
                    yield false;
                }
            }

            case "costInitial" -> {
                if (c == null) yield false;
                try {
                    double rhs = Double.parseDouble(value);
                    yield numericCompare(c.costInitial, operator, rhs);
                } catch (Exception e) {
                    yield false;
                }
            }

            case "costFinal" -> {
                if (c == null) yield false;
                try {
                    double rhs = Double.parseDouble(value);
                    yield numericCompare(c.costFinal, operator, rhs);
                } catch (Exception e) {
                    yield false;
                }
            }

            default -> false;
        };
    }

    /**
     * Compares two numeric values using the specified operator.
     *
     * @param lhs left-hand side value
     * @param operator comparison operator (=, !=, <, >)
     * @param rhs right-hand side value
     * @return true if the comparison holds
     */
    private boolean numericCompare(double lhs, String operator, double rhs) {
        return switch (operator) {
            case "=" -> lhs == rhs;
            case "!=" -> lhs != rhs;
            case "<" -> lhs < rhs;
            case ">" -> lhs > rhs;
            default -> false;
        };
    }

    /**
     * Compares two string values using the specified operator.
     *
     * @param lhs left-hand side value
     * @param operator comparison operator (only = is supported)
     * @param rhs right-hand side value
     * @return true if the strings are equal
     */
    private boolean stringCompare(String lhs, String operator, String rhs) {
        return lhs != null && "=".equals(operator) && lhs.equals(rhs);
    }

    /**
     * Compares two boolean values using the specified operator.
     *
     * @param lhs left-hand side value
     * @param operator comparison operator (only = is supported)
     * @param rhs right-hand side value
     * @return true if the booleans are equal
     */
    private boolean booleanCompare(boolean lhs, String operator, boolean rhs) {
        return "=".equals(operator) && lhs == rhs;
    }

    /**
     * Checks if a drone matches all query attributes.
     *
     * @param drone the drone to check
     * @param queries list of query attributes
     * @return true if the drone matches all query conditions
     */
    private boolean matchesAll(Drone drone, List<QueryAttribute> queries) {
        for (QueryAttribute q : queries) {
            if (!matchQueryAttribute(drone, q)) return false;
        }
        return true;
    }

    /**
     * Queries for drones that can fulfill all given medical dispatch records.
     *
     * @param recs list of medical dispatch records
     * @return list of drone IDs that can serve all requests
     */
    public List<String> queryAvailableDrones(List<MedDispatchRec> recs) {
        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        DroneForServicePoint[] dfsp =
                rest.getForObject(ilpEndpoint + "/drones-for-service-points", DroneForServicePoint[].class);
        if (drones == null || recs == null || recs.isEmpty() || dfsp == null) return List.of();

        Map<String, List<AvailabilityWindow>> availabilityMap = buildAvailabilityMap(dfsp);

        return Arrays.stream(drones)
                .filter(drone -> canServeAll(drone, recs, availabilityMap))
                .map(drone -> drone.id)
                .toList();

    }

    /**
     * Checks if a drone can serve all dispatch records.
     *
     * @param drone the drone to check
     * @param recs list of medical dispatch records
     * @param availabilityMap map of drone IDs to availability windows
     * @return true if the drone can serve all dispatch records
     */
    private boolean canServeAll(
            Drone drone,
            List<MedDispatchRec> recs,
            Map<String, List<AvailabilityWindow>> availabilityMap
    ) {
        for (MedDispatchRec rec : recs) {
            // Must meet the requirements
            if (!canServe(drone, rec)) return false;

            // Must be available at that date/time
            if (!isDroneAvailableForDispatch(drone.id, rec, availabilityMap)) return false;
        }
        return true;
    }

    /**
     * Checks if a drone can serve a specific dispatch record based on requirements.
     *
     * @param drone the drone to check
     * @param rec the medical dispatch record
     * @return true if the drone meets all requirements
     */
    private boolean canServe(Drone drone, MedDispatchRec rec) {
        if (drone.capability == null || rec.requirements == null) return false;

        Capability c = drone.capability;
        MedDispatchRec.Requirements req = rec.requirements;

        // Drone capacity must be >= the required capacity (if present)
        if (req.capacity != null && c.capacity < req.capacity) return false;

        // Drone must support cooling if required
        if (req.cooling != null && req.cooling) {
            if (!c.cooling) return false;
        }

        // Drone must support heating if required
        if (req.heating != null && req.heating) {
            if (!c.heating) return false;
        }

        return true;
    }

    /**
     * Builds a map of drone IDs to their corresponding lists of availability windows.
     *
     * @param dfsp array of drone availability data per service point
     * @return a map where the keys are drone IDs and the values are lists of availability windows for each drone
     */
    private Map<String, List<AvailabilityWindow>> buildAvailabilityMap(DroneForServicePoint[] dfsp) {
        // key = drone id, value = list of availability windows for that drone
        Map<String, List<AvailabilityWindow>> result = new HashMap<>();
        if (dfsp == null) return result;

        // Iterate over each service point
        for (DroneForServicePoint sp : dfsp) {
            if (sp.drones == null) continue;

            // Iterate over each drone availability entry at this service point
            for (DroneAvailability da : sp.drones) {
                if (da.availability == null) continue;

                // Ensure there is a list in the map for this drone id
                // then all availability windows from this service point are appended to the list
                result.computeIfAbsent(da.id, k -> new ArrayList<>())
                        .addAll(da.availability);
            }
        }
        return result;
    }

    /**
     * Checks if a drone is available for a specific dispatch based on date and time.
     *
     * @param droneId the drone ID to check
     * @param rec the medical dispatch record with date and time
     * @param availabilityMap map of drone IDs to availability windows
     * @return true if the drone is available at the specified date and time
     */
    private boolean isDroneAvailableForDispatch(
            String droneId,
            MedDispatchRec rec,
            Map<String, List<AvailabilityWindow>> availabilityMap
    ) {
        List<AvailabilityWindow> windows = availabilityMap.get(droneId);
        if (windows == null || windows.isEmpty()) return false;
        if (rec.date == null || rec.time == null) return false;

        LocalDate date = LocalDate.parse(rec.date);
        LocalTime time = LocalTime.parse(rec.time);
        DayOfWeek dow = date.getDayOfWeek();

        for (AvailabilityWindow window : windows) {
            if (window.dayOfWeek == null || window.from == null || window.until == null) continue;

            // Day of week must match
            if (!dow.toString().equals(window.dayOfWeek)) continue;

            LocalTime from = LocalTime.parse(window.from);
            LocalTime until = LocalTime.parse(window.until);

            // Check if time is within window
            if (time.isAfter(from) && time.isBefore(until)) return true;
        }
        return false;
    }

    /**
     * Calculates delivery paths for all dispatch records.
     *
     * @param recs list of medical dispatch records to fulfill
     * @return result containing drone paths, total cost, and total moves
     */
    public CalcDeliveryPathResult calcDeliveryPath(List<MedDispatchRec> recs) {
        CalcDeliveryPathResult result = new CalcDeliveryPathResult();
        result.dronePaths = new ArrayList<>();
        result.totalCost = 0.0;
        result.totalMoves = 0;

        if (recs == null || recs.isEmpty()) return result;

        // Fetch data
        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        ServicePoint[] servicePoints = rest.getForObject(ilpEndpoint + "/service-points", ServicePoint[].class);
        DroneForServicePoint[] dfsp = rest.getForObject(ilpEndpoint + "/drones-for-service-points", DroneForServicePoint[].class);
        Region[] restrictedArr = rest.getForObject(ilpEndpoint + "/restricted-areas", Region[].class);

        if (drones == null || servicePoints == null || dfsp == null) return result;

        // Convert restricted areas to list
        List<Region> restrictedAreas = restrictedArr == null ? List.of() : Arrays.asList(restrictedArr);

        List<MedDispatchRec> remaining = new ArrayList<>(recs);

        while (!remaining.isEmpty()) {
            Drone bestDrone = null;
            ServicePoint bestSp = null;
            List<MedDispatchRec> bestSubset = List.of();

            for (Drone d : drones) {
                ServicePoint sp = findServicePointForDrone(d.id, dfsp, servicePoints);
                if (sp == null) continue;

                List<MedDispatchRec> subset = findMaxSubset(
                        d,
                        sp,
                        remaining,
                        buildAvailabilityMap(dfsp),
                        restrictedAreas,
                        geoService);

                if (subset.size() > bestSubset.size()) {
                    bestDrone = d;
                    bestSp = sp;
                    bestSubset = subset;
                }
            }

            if (bestDrone == null || bestSubset.isEmpty()) break;

            DronePath dronePath = buildDronePath(
                    bestDrone,
                    bestSp, bestSubset,
                    restrictedAreas);

            if (dronePath.deliveries.isEmpty()) break;

            result.dronePaths.add(dronePath);

            int moves = computeMoves(dronePath);
            result.totalMoves += moves;
            result.totalCost += computeCost(bestDrone, moves);

            // Remove the fulfilled deliveries and continue with remaining
            remaining.removeAll(bestSubset);
        }
        return result;
    }

    /**
     * Finds the maximum subset of dispatch records a drone can serve in one trip.
     *
     * @param drone the drone to use
     * @param sp the service point where the drone is based
     * @param remaining list of remaining dispatch records to consider
     * @param availabilityMap map of drone IDs to availability windows
     * @param restrictedAreas list of restricted areas to avoid
     * @param geoService the geo service for pathfinding
     * @return list of dispatch records the drone can fulfill in one trip
     */
    private List<MedDispatchRec> findMaxSubset(
            Drone drone,
            ServicePoint sp,
            List <MedDispatchRec> remaining,
            Map<String, List<AvailabilityWindow>> availabilityMap,
            List<Region> restrictedAreas,
            GeoService geoService) {
        Capability cap = drone.capability;

        double maxCapacity = cap.capacity;
        int maxMoves = cap.maxMoves;

        // Filter dispatches the drone can serve
        List<MedDispatchRec> candidates = new ArrayList<>();
        for (MedDispatchRec rec : remaining) {
            if (canServe(drone, rec)
                    && isDroneAvailableForDispatch(drone.id, rec, availabilityMap)) {
                candidates.add(rec);
            }
        }

        if (candidates.isEmpty()) return List.of();

        candidates.sort(Comparator.comparingInt(r -> r.id));

        List<MedDispatchRec> chosen = new ArrayList<>();
        double usedCapacity = 0.0;
        int usedMoves = 0;
        Position currentPos = sp.location;
        double minMaxCostConstraint = Double.POSITIVE_INFINITY;

        for (MedDispatchRec candidate : candidates) {
            // Capacity
            double nextCapacity = usedCapacity + candidate.requirements.capacity;
            if (nextCapacity > maxCapacity) continue;

            // Path
            List<Position> pathToRec = geoService.findPath(
                    currentPos,
                    candidate.delivery,
                    restrictedAreas
            );
            if (pathToRec == null || pathToRec.isEmpty()) continue;

            int movesToRec = pathToRec.size() - 1;
            int tmpMoves = usedMoves + movesToRec;

            // Return path to service point
            List<Position> returnPath = geoService.findPath(
                    candidate.delivery,
                    sp.location,
                    restrictedAreas
            );
            if (returnPath == null || returnPath.isEmpty()) continue;

            int movesReturn = returnPath.size() - 1;
            int movesIfIncluded = tmpMoves + movesReturn;

            if (movesIfIncluded > maxMoves) continue;

            // Track the tightest maxCost constraint across chosen deliveries
            double recMaxCost =
                    (candidate.requirements.maxCost == null ? 0.0 : candidate.requirements.maxCost);

            double newMinMaxCostConstraint = minMaxCostConstraint;
            if (recMaxCost > 0 && recMaxCost < minMaxCostConstraint) {
                newMinMaxCostConstraint = recMaxCost;
            }

            if (newMinMaxCostConstraint < Double.POSITIVE_INFINITY) {
                double flightCost = cap.costInitial
                        + (movesIfIncluded * cap.costPerMove)
                        + cap.costFinal;

                int deliveriesIfIncluded = chosen.size() + 1;
                double perDeliveryCost = flightCost / deliveriesIfIncluded;

                if (perDeliveryCost > newMinMaxCostConstraint) continue;
            }

            // Accept the candidate
            chosen.add(candidate);
            usedCapacity = nextCapacity;
            usedMoves = tmpMoves;
            currentPos = candidate.delivery;
            minMaxCostConstraint = newMinMaxCostConstraint;
        }
        return chosen;
    }

    /**
     * Finds the service point where a specific drone is based.
     *
     * @param droneId the drone ID to search for
     * @param dfsp array of drone availability data per service point
     * @param servicePoints array of all service points
     * @return the service point where the drone is based, or null if not found
     */
    private ServicePoint findServicePointForDrone(
                String droneId,
                DroneForServicePoint[] dfsp,
                ServicePoint[] servicePoints
        ) {
        if (dfsp == null || servicePoints == null) return null;

        for (DroneForServicePoint dfspEntry : dfsp) {
            if (dfspEntry.drones == null) continue;

            for (DroneAvailability da : dfspEntry.drones) {
                if (droneId.equals(da.id)) {
                    int spId = dfspEntry.servicePointId;
                    for (ServicePoint sp : servicePoints) {
                        if (sp.id == spId) return sp;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Builds a complete drone path including all delivery legs and return to service point.
     *
     * @param drone the drone to use
     * @param sp the service point where the drone starts and returns
     * @param recs list of medical dispatch records to deliver
     * @param restrictedAreas list of restricted areas to avoid
     * @return complete drone path with all delivery legs
     */
    private DronePath buildDronePath(
            Drone drone,
            ServicePoint sp,
            List<MedDispatchRec> recs,
            List<Region> restrictedAreas
    ) {
        DronePath dronePath = new DronePath();
        dronePath.droneId = drone.id;
        dronePath.deliveries = new ArrayList<>();

        if (recs.isEmpty()) return dronePath;

        List<MedDispatchRec> ordered = new ArrayList<>(recs);
        ordered.sort(Comparator.comparingInt(r -> r.id));

        Position current = sp.location;

        for (MedDispatchRec rec : ordered) {
            List<Position> legResult = geoService.findPath(current, rec.delivery, restrictedAreas);
            if (legResult == null || legResult.isEmpty()) return dronePath;

            // Create a mutable copy to allow adding the hover position
            List<Position> leg = new ArrayList<>(legResult);

            // Hover at last position
            Position last = leg.getLast();
            Position hover = new Position(last.lng, last.lat);
            leg.add(hover);

            DeliveryPath dpEntry = new DeliveryPath();
            dpEntry.deliveryId = rec.id;
            dpEntry.flightPath = leg;

            dronePath.deliveries.add(dpEntry);

            // Update the current position
            Position actual = leg.get(leg.size() - 2);
            current = new Position(actual.lng, actual.lat);
        }


        // Return to service point
        List<Position> returnLegResult = geoService.findPath(current, sp.location, restrictedAreas);
        if (returnLegResult == null || returnLegResult.isEmpty()) return dronePath;

        // Create a mutable copy
        List<Position> returnLeg = new ArrayList<>(returnLegResult);

        Position rLast = returnLeg.getLast();
        Position rHover = new Position(rLast.lng, rLast.lat);
        returnLeg.add(rHover);

        DeliveryPath returnPath = new DeliveryPath();
        returnPath.flightPath = returnLeg;
        returnPath.deliveryId = -1;

        dronePath.deliveries.add(returnPath);

        return dronePath;
    }

    /**
     * Computes the total number of moves in a drone path.
     *
     * @param dp the drone path to analyze
     * @return total number of moves across all delivery legs
     */
    private int computeMoves (DronePath dp) {
        int total = 0;

        if (dp.deliveries == null) return total;

        for (DeliveryPath dpEntry : dp.deliveries) {
            if (dpEntry.flightPath != null) {
                total += dpEntry.flightPath.size() - 1; // -1 for hover at last position
            }
        }

        return total;
    }

    /**
     * Computes the total cost for a drone to complete a path.
     *
     * @param drone the drone performing the delivery
     * @param moves the number of moves in the path
     * @return total cost including initial, per-move, and final costs
     */
    private double computeCost (Drone drone, int moves) {
        Capability cap = drone.capability;
        return cap.costInitial + (moves * cap.costPerMove) + cap.costFinal;
    }

    /**
     * Calculates the delivery path and returns it as a GeoJSON LineString.
     *
     * @param recs list of medical dispatch records to fulfill
     * @return GeoJSON string representing the complete flight path
     */
    public String calcDeliveryPathAsGeoJson(List<MedDispatchRec> recs) {
        if (recs == null || recs.isEmpty()) {
            return "{\"type\":\"LineString\",\"coordinates\":[]}";
        }

        // Fetch data
        Drone[] drones = rest.getForObject(ilpEndpoint + "/drones", Drone[].class);
        ServicePoint[] servicePoints = rest.getForObject(ilpEndpoint + "/service-points", ServicePoint[].class);
        DroneForServicePoint[] dfsp = rest.getForObject(ilpEndpoint + "/drones-for-service-points", DroneForServicePoint[].class);
        Region[] restrictedArr = rest.getForObject(ilpEndpoint + "/restricted-areas", Region[].class);

        if (drones == null || servicePoints == null || dfsp == null){
            return "{\"type\":\"LineString\",\"coordinates\":[]}"; //
        }

        // Convert restricted areas to list
        List<Region> restrictedAreas = restrictedArr == null ? List.of() : Arrays.asList(restrictedArr);

        Map<String, List<AvailabilityWindow>> availabilityMap = buildAvailabilityMap(dfsp);

        // Find drone that can serve all
        Drone chosenDrone = null;
        ServicePoint chosenSp = null;

        for (Drone d : drones) {
            ServicePoint sp = findServicePointForDrone(d.id, dfsp, servicePoints);
            if (sp == null) continue;

            List<MedDispatchRec> subset = findMaxSubset(
                    d,
                    sp,
                    recs,
                    availabilityMap,
                    restrictedAreas,
                    geoService);

            if (subset.size() == recs.size()) {
                chosenDrone = d;
                chosenSp = sp;
                break;
            }
        }

        if (chosenDrone == null) {
            // This shouldn't happen
            return "{\"type\":\"LineString\",\"coordinates\":[]}";
        }

        // Build the route
        DronePath dronePath = buildDronePath(chosenDrone, chosenSp, recs, restrictedAreas);

        // Flatten all the positions into a LineString
        List<Position> flat = new ArrayList<>();
        for (DeliveryPath dp : dronePath.deliveries) {
            if (dp.flightPath != null) flat.addAll(dp.flightPath);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"LineString\",\"coordinates\":[");

        for (int i = 0; i < flat.size(); i++) {
            Position p = flat.get(i);
            sb.append("[").append(p.lng).append(",").append(p.lat).append("]");
            if (i < flat.size() - 1) sb.append(",");
        }

        sb.append("]}");

        return sb.toString();
    }
}