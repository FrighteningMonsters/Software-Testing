package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Syntax Testing: Violating formal input definitions to test robustness.
 *
 * This test class verifies the system handles malformed, invalid, or unexpected
 * input syntax gracefully without crashing or producing incorrect results.
 *
 * Formal Input Definitions Tested:
 * 1. QueryAttribute.attribute: {id, name, cooling, heating, capacity, maxMoves, costPerMove, costInitial, costFinal}
 * 2. QueryAttribute.operator: {=, !=, <, >}
 * 3. QueryAttribute.value: type-specific (boolean strings, numeric strings, plain strings)
 * 4. MedDispatchRec.date: YYYY-MM-DD format (e.g., "2025-12-22")
 * 5. MedDispatchRec.time: HH:mm format (e.g., "14:30") per spec
 * 6. AvailabilityWindow.from/until: HH:mm:ss format (e.g., "00:00:00")
 * 7. AvailabilityWindow.dayOfWeek: {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY}
 * 8. Position.lat: [-90.0, 90.0]
 * 9. Position.lng: [-180.0, 180.0]
 * 10. Angle (for nextPosition): {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Syntax Testing - Input Format Violation Robustness")
public class SyntaxTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService mockGeoService;
    private DroneService droneService;
    private GeoService geoService;
    private static final String ENDPOINT = "http://api";

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, mockGeoService, ENDPOINT);
        geoService = new GeoService();
    }

    // QUERY ATTRIBUTE SYNTAX TESTS

    @Nested
    @DisplayName("QueryAttribute Syntax Violations")
    class QueryAttributeSyntax {

        private Drone createTestDrone() {
            Drone drone = new Drone();
            drone.id = "D1";
            drone.name = "TestDrone";
            Capability c = new Capability();
            c.cooling = true;
            c.heating = false;
            c.capacity = 10.0;
            c.maxMoves = 100;
            c.costPerMove = 0.5;
            c.costInitial = 5.0;
            c.costFinal = 2.0;
            drone.capability = c;
            return drone;
        }

        private QueryAttribute createQuery(String attribute, String operator, String value) {
            QueryAttribute q = new QueryAttribute();
            q.attribute = attribute;
            q.operator = operator;
            q.value = value;
            return q;
        }

        private void setupMockForQuery(Drone drone) {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{drone});
        }

        // --- Invalid Attribute Names ---

        /**
         * Invalid attribute names are NOT filtered out - they pass to matchQueryAttribute()
         * which returns false (via default case), so NO drones match.
         *
         * This is the correct behavior: invalid queries should fail, not be ignored.
         */
        @ParameterizedTest(name = "Invalid attribute: \"{0}\"")
        @ValueSource(strings = {
                "COOLING",             // Wrong case (uppercase)
                "Cooling",             // Wrong case (title case)
                "cool",                // Truncated
                "coolingg",            // Extra character
                "colling",             // Typo
                "max_moves",           // Underscore instead of camelCase
                "max-moves",           // Hyphen instead of camelCase
                "unknown",             // Non-existent attribute
                "capability",          // Object name, not attribute
                "capability.cooling",  // Dot notation
                "123",                 // Numeric
                "cooling=true",        // Embedded operator
                "<script>",            // XSS attempt
                "null",                // Literal null string
                "undefined"            // JavaScript undefined
        })
        @DisplayName("Invalid attribute name causes no match (returns false)")
        void invalidAttributeName(String invalidAttribute) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery(invalidAttribute, "=", "true")
            ));

            // Invalid attribute -> matchQueryAttribute returns false -> no drones match
            assertTrue(result.isEmpty(),
                    "Invalid attribute '" + invalidAttribute + "' should cause no match");
        }

        /**
         * Queries with blank/empty fields are filtered out before matching.
         * When ALL queries are filtered out, matchesAll() returns true (vacuous truth).
         */
        @ParameterizedTest(name = "Blank attribute: \"{0}\"")
        @ValueSource(strings = {
                "",                    // Empty string
                " ",                   // Whitespace only
                "   "                  // Multiple spaces
        })
        @DisplayName("Blank attribute is filtered out - all drones returned (vacuous truth)")
        void blankAttributeFilteredOut(String blankAttribute) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery(blankAttribute, "=", "true")
            ));

            // Blank values are filtered out by isBlank() check
            // Empty valid list -> matchesAll returns true -> all drones returned
            assertTrue(result.contains("D1"),
                    "Blank attribute should be filtered out, returning all drones");
        }

        @Test
        @DisplayName("Null attribute name should be handled gracefully")
        void nullAttributeName() {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            QueryAttribute q = new QueryAttribute();
            q.attribute = null;
            q.operator = "=";
            q.value = "true";

            // Should not throw NullPointerException
            assertDoesNotThrow(() -> droneService.query(List.of(q)));
        }

        // --- Invalid Operators ---

        /**
         * Invalid operators pass to the comparison methods which return false
         * (via default case or != "=" check), so NO drones match.
         */
        @ParameterizedTest(name = "Invalid operator: \"{0}\"")
        @ValueSource(strings = {
                "==",              // Double equals (programming style)
                "===",             // Triple equals (JavaScript)
                "<>",              // SQL not equals
                ">=",              // Greater or equal (not supported)
                "<=",              // Less or equal (not supported)
                "eq",              // Word operator
                "ne",              // Word operator
                "lt",              // Word operator
                "gt",              // Word operator
                "LIKE",            // SQL LIKE
                "IN",              // SQL IN
                "~",               // Regex operator
                "contains",        // String operator
                "!",               // Single negation
                ">>",              // Bit shift
                "&&",              // Logical AND
                "||"               // Logical OR
        })
        @DisplayName("Invalid operator causes no match (comparison returns false)")
        void invalidOperator(String invalidOperator) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery("cooling", invalidOperator, "true")
            ));

            // Invalid operator -> comparison method returns false -> no drones match
            assertTrue(result.isEmpty(),
                    "Invalid operator '" + invalidOperator + "' should cause no match");
        }

        /**
         * Blank operators are filtered out before matching.
         */
        @ParameterizedTest(name = "Blank operator: \"{0}\"")
        @ValueSource(strings = {
                "",                // Empty
                " ",               // Whitespace
                "   "              // Multiple spaces
        })
        @DisplayName("Blank operator is filtered out - all drones returned")
        void blankOperatorFilteredOut(String blankOperator) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery("cooling", blankOperator, "true")
            ));

            // Blank operators filtered out -> empty valid list -> all drones returned
            assertTrue(result.contains("D1"),
                    "Blank operator should be filtered out, returning all drones");
        }

        @Test
        @DisplayName("Null operator should be handled gracefully")
        void nullOperator() {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            QueryAttribute q = createQuery("cooling", null, "true");

            assertDoesNotThrow(() -> droneService.query(List.of(q)));
        }

        // --- Invalid Values for Numeric Attributes ---

        /**
         * Blank values are filtered out before matching.
         */
        @ParameterizedTest(name = "Blank value: \"{0}\"")
        @ValueSource(strings = {
                "",                // Empty
                " ",               // Whitespace
                "   "              // Multiple spaces
        })
        @DisplayName("Blank value is filtered out - all drones returned")
        void blankValueFilteredOut(String blankValue) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery("capacity", "=", blankValue)
            ));

            // Blank values filtered out -> empty valid list -> all drones returned
            assertTrue(result.contains("D1"),
                    "Blank value should be filtered out, returning all drones");
        }

        /**
         * Invalid numeric formats that cannot be parsed by Double.parseDouble().
         * These pass to matchQueryAttribute but parsing fails -> returns false -> no match.
         */
        @ParameterizedTest(name = "Unparseable numeric value: \"{0}\"")
        @ValueSource(strings = {
                "abc",             // Letters - fails parseDouble
                "0x10",            // Hex notation - fails parseDouble
                "ten",             // Word number - fails parseDouble
                "null",            // Literal null string - fails parseDouble
                "undefined",       // JavaScript undefined - fails parseDouble
                "10.5.5",          // Multiple decimals - fails parseDouble
                "10,5",            // Comma decimal (European format) - fails parseDouble
                "$10",             // Currency symbol - fails parseDouble
                "10%",             // Percentage symbol - fails parseDouble
                "10 kg",           // With unit - fails parseDouble
                "1,000",           // Thousand separator - fails parseDouble
                "1_000"            // Underscore separator - fails parseDouble
        })
        @DisplayName("Unparseable numeric values cause no match (parseDouble fails)")
        void unparseableNumericValue(String unparseableValue) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery("capacity", "=", unparseableValue)
            ));

            // parseDouble fails -> matchQueryAttribute returns false -> no match
            assertTrue(result.isEmpty(),
                    "Unparseable value '" + unparseableValue + "' should cause no match");
        }

        /**
         * Valid decimal numbers should now work properly (validation moved to match checking).
         */
        @Test
        @DisplayName("Valid decimal 10.5 is parsed and compared correctly")
        void validDecimalWorks() {
            Drone drone = createTestDrone(); // capacity = 10.0
            setupMockForQuery(drone);

            // Query for capacity = 10.0 (matches drone)
            List<String> result = droneService.query(List.of(
                    createQuery("capacity", "=", "10.0")
            ));
            assertTrue(result.contains("D1"), "10.0 should match drone capacity");

            // Query for capacity = 10.5 (doesn't match drone with capacity 10.0)
            result = droneService.query(List.of(
                    createQuery("capacity", "=", "10.5")
            ));
            assertTrue(result.isEmpty(), "10.5 should not match drone with capacity 10.0");

            // Query for capacity > 9.5 (matches drone)
            result = droneService.query(List.of(
                    createQuery("capacity", ">", "9.5")
            ));
            assertTrue(result.contains("D1"), "capacity > 9.5 should match drone with capacity 10.0");
        }

        /**
         * Valid negative numbers should now work properly.
         */
        @Test
        @DisplayName("Valid negative numbers are parsed correctly")
        void validNegativeWorks() {
            Drone drone = createTestDrone(); // capacity = 10.0
            setupMockForQuery(drone);

            // Query for capacity > -5 (matches drone)
            List<String> result = droneService.query(List.of(
                    createQuery("capacity", ">", "-5")
            ));
            assertTrue(result.contains("D1"), "capacity > -5 should match drone with capacity 10.0");
        }

        /**
         * Special case: NaN and Infinity are parsed by Double.parseDouble() but won't equal drone capacity.
         */
        @ParameterizedTest(name = "Special double value: \"{0}\"")
        @ValueSource(strings = {
                "NaN",             // parseDouble returns Double.NaN, comparison fails
                "Infinity"         // parseDouble returns Double.POSITIVE_INFINITY, comparison fails
        })
        @DisplayName("NaN/Infinity parse successfully but don't match drone capacity")
        void specialDoubleValues(String specialValue) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            List<String> result = droneService.query(List.of(
                    createQuery("capacity", "=", specialValue)
            ));

            // These parse successfully but NaN != 10.0 and Infinity != 10.0
            assertTrue(result.isEmpty(),
                    "Value '" + specialValue + "' parses but doesn't match drone capacity");
        }

        // --- Invalid Values for Boolean Attributes ---

        @ParameterizedTest(name = "Non-standard boolean value: \"{0}\"")
        @ValueSource(strings = {
                "",                // Empty
                " ",               // Whitespace
                "TRUE",            // Uppercase
                "FALSE",           // Uppercase
                "True",            // Title case
                "False",           // Title case
                "1",               // Numeric true
                "0",               // Numeric false
                "yes",             // Alternative true
                "no",              // Alternative false
                "on",              // Alternative true
                "off",             // Alternative false
                "t",               // Abbreviated
                "f",               // Abbreviated
                "y",               // Abbreviated
                "n",               // Abbreviated
                "null",            // Null string
                "undefined"        // JavaScript undefined
        })
        @DisplayName("Non-standard boolean values should be handled")
        void nonStandardBooleanValue(String boolValue) {
            Drone drone = createTestDrone();
            setupMockForQuery(drone);

            // This tests whether non-standard boolean values are handled
            // Boolean.parseBoolean() returns false for anything not "true" (case-insensitive)
            assertDoesNotThrow(() -> droneService.query(List.of(
                    createQuery("cooling", "=", boolValue)
            )));
        }
    }

    // DATE/TIME SYNTAX TESTS

    @Nested
    @DisplayName("Date/Time Format Syntax Violations")
    class DateTimeSyntax {

        private Drone createTestDrone() {
            Drone drone = new Drone();
            drone.id = "D1";
            Capability c = new Capability();
            c.cooling = true;
            c.heating = true;
            c.capacity = 10.0;
            c.maxMoves = 100;
            c.costPerMove = 0.5;
            c.costInitial = 5.0;
            c.costFinal = 2.0;
            drone.capability = c;
            return drone;
        }

        private MedDispatchRec createDispatch(String date, String time) {
            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = date;
            rec.time = time;
            rec.delivery = new Position(-3.188, 55.944);
            MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
            req.cooling = false;
            req.heating = false;
            req.capacity = 5.0;
            rec.requirements = req;
            return rec;
        }

        private DroneForServicePoint[] createAvailability(String droneId) {
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = "MONDAY";
            window.from = "08:00:00";
            window.until = "18:00:00";

            DroneAvailability da = new DroneAvailability();
            da.id = droneId;
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);
            return new DroneForServicePoint[]{dfsp};
        }

        private void setupMocks(Drone drone) {
            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{drone});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(createAvailability("D1"));
        }

        // --- Invalid Date Formats ---

        @ParameterizedTest(name = "Invalid date format: \"{0}\"")
        @ValueSource(strings = {
                "",                    // Empty
                " ",                   // Whitespace
                "2025-1-20",           // Single digit month
                "2025-01-5",           // Single digit day
                "25-01-20",            // Two digit year
                "01-20-2025",          // MM-DD-YYYY (US format)
                "20-01-2025",          // DD-MM-YYYY (European format)
                "2025/01/20",          // Slash separators
                "2025.01.20",          // Dot separators
                "20250120",            // No separators
                "2025 01 20",          // Space separators
                "January 20, 2025",    // Full text
                "Jan 20, 2025",        // Abbreviated text
                "2025-13-01",          // Invalid month (13)
                "2025-00-01",          // Invalid month (0)
                "2025-01-32",          // Invalid day (32)
                "2025-01-00",          // Invalid day (0)
                "2025-02-30",          // Feb 30 doesn't exist
                "2025-04-31",          // April 31 doesn't exist
                "abcd-ef-gh",          // Non-numeric
                "null",                // Literal null
                "today",               // Relative date
                "tomorrow",            // Relative date
                "2025-01-20T10:00:00", // ISO with time
                "2025-01-20 10:00:00"  // Date and time combined
        })
        @DisplayName("Invalid date format should throw or handle gracefully")
        void invalidDateFormat(String invalidDate) {
            Drone drone = createTestDrone();
            setupMocks(drone);

            MedDispatchRec dispatch = createDispatch(invalidDate, "12:00");

            // Should either throw DateTimeParseException or handle gracefully
            assertThrows(Exception.class, () ->
                    droneService.queryAvailableDrones(List.of(dispatch)),
                    "Invalid date '" + invalidDate + "' should cause exception");
        }

        @Test
        @DisplayName("Null date should be handled gracefully")
        void nullDate() {
            Drone drone = createTestDrone();
            setupMocks(drone);

            MedDispatchRec dispatch = createDispatch(null, "12:00");

            // Should not throw - method checks for null
            List<String> result = droneService.queryAvailableDrones(List.of(dispatch));
            assertTrue(result.isEmpty(), "Null date should result in no matches");
        }

        // --- Invalid Time Formats ---

        /**
         * Invalid time formats that should cause parsing to fail.
         * Per spec, time format is HH:mm (e.g., "14:30").
         */
        @ParameterizedTest(name = "Invalid time format: \"{0}\"")
        @ValueSource(strings = {
                "",                // Empty
                " ",               // Whitespace
                "12",              // Just hours
                "1:00",            // Single digit hour
                "12:0",            // Single digit minute
                "12.00",           // Dot separators
                "12-00",           // Hyphen separators
                "1200",            // No separators
                "12 00",           // Space separators
                "12:00 AM",        // 12-hour format with AM
                "12:00 PM",        // 12-hour format with PM
                "12:00AM",         // No space before AM
                "25:00",           // Invalid hour (25)
                "12:60",           // Invalid minute (60)
                "-1:00",           // Negative hour
                "12:-1",           // Negative minute
                "noon",            // Word time
                "midnight",        // Word time
                "null",            // Literal null
                "12:00+00:00"      // With timezone offset
        })
        @DisplayName("Invalid time format should throw or handle gracefully")
        void invalidTimeFormat(String invalidTime) {
            Drone drone = createTestDrone();
            setupMocks(drone);

            MedDispatchRec dispatch = createDispatch("2025-01-20", invalidTime);

            // Should either throw DateTimeParseException or handle gracefully
            assertThrows(Exception.class, () ->
                    droneService.queryAvailableDrones(List.of(dispatch)),
                    "Invalid time '" + invalidTime + "' should cause exception");
        }

        @Test
        @DisplayName("Null time should be handled gracefully")
        void nullTime() {
            Drone drone = createTestDrone();
            setupMocks(drone);

            MedDispatchRec dispatch = createDispatch("2025-01-20", null);

            // Should not throw - method checks for null
            List<String> result = droneService.queryAvailableDrones(List.of(dispatch));
            assertTrue(result.isEmpty(), "Null time should result in no matches");
        }
    }

    // POSITION COORDINATE SYNTAX TESTS

    @Nested
    @DisplayName("Position Coordinate Syntax Violations")
    class PositionSyntax {

        // Invalid Latitude Values

        @ParameterizedTest(name = "Invalid latitude: {0}")
        @ValueSource(doubles = {
                -90.1,             // Just below minimum
                90.1,              // Just above maximum
                -91.0,             // Below minimum
                91.0,              // Above maximum
                -180.0,            // Longitude range (wrong)
                180.0,             // Longitude range (wrong)
                -1000.0,           // Far out of range
                1000.0,            // Far out of range
                Double.MAX_VALUE,  // Maximum double
                -Double.MAX_VALUE, // Minimum double
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        @DisplayName("Invalid latitude should return null from calculateDistance")
        void invalidLatitude(double invalidLat) {
            Position p1 = new Position(-3.188, invalidLat);
            Position p2 = new Position(-3.188, 55.944);

            Double result = geoService.calculateDistance(p1, p2);
            assertNull(result, "Invalid latitude " + invalidLat + " should return null");
        }

        @Test
        @DisplayName("NaN latitude should return null")
        void nanLatitude() {
            Position p1 = new Position(-3.188, Double.NaN);
            Position p2 = new Position(-3.188, 55.944);

            Double result = geoService.calculateDistance(p1, p2);
            // NaN comparisons are tricky - this tests behavior
            assertNull(result, "NaN latitude should return null");
        }

        // --- Invalid Longitude Values ---

        @ParameterizedTest(name = "Invalid longitude: {0}")
        @ValueSource(doubles = {
                -180.1,            // Just below minimum
                180.1,             // Just above maximum
                -181.0,            // Below minimum
                181.0,             // Above maximum
                -360.0,            // Full rotation negative
                360.0,             // Full rotation positive
                -1000.0,           // Far out of range
                1000.0,            // Far out of range
                Double.MAX_VALUE,
                -Double.MAX_VALUE,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        @DisplayName("Invalid longitude should return null from calculateDistance")
        void invalidLongitude(double invalidLng) {
            Position p1 = new Position(invalidLng, 55.944);
            Position p2 = new Position(-3.188, 55.944);

            Double result = geoService.calculateDistance(p1, p2);
            assertNull(result, "Invalid longitude " + invalidLng + " should return null");
        }

        // --- Null Coordinate Components ---

        @Test
        @DisplayName("Null latitude should return null")
        void nullLatitude() {
            Position p = new Position();
            p.lng = -3.188;
            p.lat = null;

            Double result = geoService.calculateDistance(p, new Position(-3.188, 55.944));
            assertNull(result, "Null latitude should return null");
        }

        @Test
        @DisplayName("Null longitude should return null")
        void nullLongitude() {
            Position p = new Position();
            p.lng = null;
            p.lat = 55.944;

            Double result = geoService.calculateDistance(p, new Position(-3.188, 55.944));
            assertNull(result, "Null longitude should return null");
        }

        @Test
        @DisplayName("Null position should return null")
        void nullPosition() {
            Double result = geoService.calculateDistance(null, new Position(-3.188, 55.944));
            assertNull(result, "Null position should return null");
        }

        @Test
        @DisplayName("Both positions null should return null")
        void bothPositionsNull() {
            Double result = geoService.calculateDistance(null, null);
            assertNull(result, "Both null positions should return null");
        }
    }

    // ANGLE SYNTAX TESTS

    @Nested
    @DisplayName("Angle Value Syntax Violations")
    class AngleSyntax {

        // Valid angles are: 0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5

        // --- Invalid Angle Values ---

        @ParameterizedTest(name = "Invalid angle: {0}")
        @ValueSource(doubles = {
                1.0,               // Not in valid set
                10.0,              // Not in valid set
                22.4,              // Close but not exact
                22.6,              // Close but not exact
                44.9,              // Close but not exact
                45.1,              // Close but not exact
                -22.5,             // Negative of valid angle
                -45.0,             // Negative
                -90.0,             // Negative
                360.0,             // Full rotation
                361.0,             // Beyond full rotation
                -360.0,            // Negative full rotation
                720.0,             // Two rotations
                179.9,             // Close to 180
                180.1,             // Close to 180
                337.4,             // Close to 337.5
                337.6,             // Close to 337.5
                Double.MAX_VALUE,
                -Double.MAX_VALUE,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        @DisplayName("Invalid angle should return null from nextPosition")
        void invalidAngle(double invalidAngle) {
            Position start = new Position(-3.188, 55.944);

            Position result = geoService.nextPosition(start, invalidAngle);
            assertNull(result, "Invalid angle " + invalidAngle + " should return null");
        }

        @Test
        @DisplayName("NaN angle should return null")
        void nanAngle() {
            Position start = new Position(-3.188, 55.944);

            Position result = geoService.nextPosition(start, Double.NaN);
            assertNull(result, "NaN angle should return null");
        }

        @Test
        @DisplayName("Null angle should return null")
        void nullAngle() {
            Position start = new Position(-3.188, 55.944);

            Position result = geoService.nextPosition(start, null);
            assertNull(result, "Null angle should return null");
        }

        // --- Valid angles should work ---

        @ParameterizedTest(name = "Valid angle: {0}")
        @ValueSource(doubles = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5})
        @DisplayName("Valid angles should return non-null position")
        void validAngle(double validAngle) {
            Position start = new Position(-3.188, 55.944);

            Position result = geoService.nextPosition(start, validAngle);
            assertNotNull(result, "Valid angle " + validAngle + " should return a position");
        }
    }

    //D AY OF WEEK SYNTAX TESTS

    @Nested
    @DisplayName("DayOfWeek Format Syntax Violations")
    class DayOfWeekSyntax {

        private Drone createTestDrone() {
            Drone drone = new Drone();
            drone.id = "D1";
            Capability c = new Capability();
            c.cooling = true;
            c.heating = true;
            c.capacity = 10.0;
            c.maxMoves = 100;
            c.costPerMove = 0.5;
            c.costInitial = 5.0;
            c.costFinal = 2.0;
            drone.capability = c;
            return drone;
        }

        private MedDispatchRec createMondayDispatch() {
            MedDispatchRec rec = new MedDispatchRec();
            rec.id = 1;
            rec.date = "2025-01-20"; // Monday
            rec.time = "12:00";      // HH:mm format per spec
            rec.delivery = new Position(-3.188, 55.944);
            MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
            req.cooling = false;
            req.capacity = 5.0;
            rec.requirements = req;
            return rec;
        }

        private DroneForServicePoint[] createAvailabilityWithDay(String droneId, String dayOfWeek) {
            AvailabilityWindow window = new AvailabilityWindow();
            window.dayOfWeek = dayOfWeek;
            window.from = "08:00:00";
            window.until = "18:00:00";

            DroneAvailability da = new DroneAvailability();
            da.id = droneId;
            da.availability = List.of(window);

            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.servicePointId = 1;
            dfsp.drones = List.of(da);
            return new DroneForServicePoint[]{dfsp};
        }

        @ParameterizedTest(name = "Invalid dayOfWeek format: \"{0}\"")
        @ValueSource(strings = {
                "",                // Empty
                " ",               // Whitespace
                "monday",          // Lowercase
                "Monday",          // Title case
                "Mon",             // Abbreviated
                "MON",             // Abbreviated uppercase
                "M",               // Single letter
                "1",               // Numeric (1 = Monday in some systems)
                "0",               // Numeric (0 = Sunday in some systems)
                "MONDAYY",         // Typo - extra letter
                "MNDAY",           // Typo - missing letter
                "MONDAY ",         // Trailing space
                " MONDAY",         // Leading space
                "null",            // Literal null string
                "LUNDI",           // French
                "MONTAG"           // German
        })
        @DisplayName("Invalid dayOfWeek format should not match Monday dispatch")
        void invalidDayOfWeekFormat(String invalidDay) {
            Drone drone = createTestDrone();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{drone});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(createAvailabilityWithDay("D1", invalidDay));

            MedDispatchRec dispatch = createMondayDispatch();

            // Should not match because dayOfWeek format is wrong
            List<String> result = droneService.queryAvailableDrones(List.of(dispatch));
            assertTrue(result.isEmpty(),
                    "Invalid dayOfWeek '" + invalidDay + "' should not match MONDAY dispatch");
        }

        @Test
        @DisplayName("Null dayOfWeek should be handled gracefully")
        void nullDayOfWeek() {
            Drone drone = createTestDrone();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{drone});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(createAvailabilityWithDay("D1", null));

            MedDispatchRec dispatch = createMondayDispatch();

            // Should not throw, just not match
            List<String> result = droneService.queryAvailableDrones(List.of(dispatch));
            assertTrue(result.isEmpty(), "Null dayOfWeek should not match");
        }

        @Test
        @DisplayName("Correct MONDAY format should match Monday dispatch")
        void correctDayOfWeekFormat() {
            Drone drone = createTestDrone();

            when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class))
                    .thenReturn(new Drone[]{drone});
            when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                    .thenReturn(createAvailabilityWithDay("D1", "MONDAY"));

            MedDispatchRec dispatch = createMondayDispatch();

            List<String> result = droneService.queryAvailableDrones(List.of(dispatch));
            assertTrue(result.contains("D1"), "Correct MONDAY format should match");
        }
    }
}