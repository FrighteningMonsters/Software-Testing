package uk.ac.ed.acp.cw2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DroneServiceEPTest {

    @Mock private RestTemplate restTemplate;
    @Mock private GeoService geoService;
    private DroneService droneService;
    private final String ENDPOINT = "http://api";

    @BeforeEach
    void setUp() {
        droneService = new DroneService(restTemplate, geoService, ENDPOINT);
    }

    // Helper to create a standard drone
    private Drone createDrone(String id, boolean cooling, double capacity) {
        Capability c = new Capability();
        c.cooling = cooling;
        c.capacity = capacity;
        Drone d = new Drone();
        d.id = id;
        d.capability = c;
        return d;
    }

    // Helper to create availability for a specific day
    private DroneForServicePoint[] createDayAvailability(String droneId, String day) {
        AvailabilityWindow window = new AvailabilityWindow();
        window.dayOfWeek = day;
        window.from = "08:00";
        window.until = "18:00";

        DroneAvailability da = new DroneAvailability();
        da.id = droneId;
        da.availability = List.of(window);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.drones = List.of(da);
        return new DroneForServicePoint[]{dfsp};
    }

    @Test
    @DisplayName("EP Partition: Capability Mismatch (Lacks Required Cooling)")
    void testQuery_CapabilityMismatch_Cooling() {
        // Partition: Requirements include cooling = true, Drone capability cooling = false
        Drone d = createDrone("D1", false, 100.0);
        MedDispatchRec rec = new MedDispatchRec();
        rec.requirements = new MedDispatchRec.Requirements();
        rec.requirements.cooling = true; // Required

        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(new Drone[]{d});

        List<String> result = droneService.queryAvailableDrones(List.of(rec));
        assertFalse(result.contains("D1"), "Drone without cooling should be partitioned as 'Ineligible'");
    }

    @Test
    @DisplayName("EP Partition: Capacity Insufficient")
    void testQuery_CapacityInsufficient() {
        // Partition: Required capacity (20.0) > Drone capacity (10.0)
        Drone d = createDrone("D2", true, 10.0);
        MedDispatchRec rec = new MedDispatchRec();
        rec.requirements = new MedDispatchRec.Requirements();
        rec.requirements.capacity = 20.0;

        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(new Drone[]{d});

        List<String> result = droneService.queryAvailableDrones(List.of(rec));
        assertTrue(result.isEmpty(), "Drone with low capacity should be partitioned as 'Ineligible'");
    }

    @Test
    @DisplayName("EP Partition: Temporal Mismatch (Wrong Day of Week)")
    void testQuery_TemporalMismatch_Day() {
        // Partition: Request is on a Sunday, Drone only available on Mondays
        Drone d = createDrone("D3", true, 100.0);
        MedDispatchRec rec = new MedDispatchRec();
        rec.date = "2025-01-19"; // A Sunday
        rec.time = "12:00:00";
        rec.requirements = new MedDispatchRec.Requirements();

        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(new Drone[]{d});
        when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                .thenReturn(createDayAvailability("D3", "MONDAY"));

        List<String> result = droneService.queryAvailableDrones(List.of(rec));
        assertFalse(result.contains("D3"), "Drone available only on Monday should be ineligible for a Sunday request");
    }

    @Test
    @DisplayName("EP Partition: Full Match (Representative of Valid Class)")
    void testQuery_FullMatch() {
        // Partition: All criteria (Capability, Capacity, Temporal) are satisfied
        Drone d = createDrone("D4", true, 50.0);
        MedDispatchRec rec = new MedDispatchRec();
        rec.date = "2025-01-20"; // A Monday
        rec.time = "12:00:00";
        rec.requirements = new MedDispatchRec.Requirements();
        rec.requirements.cooling = true;
        rec.requirements.capacity = 10.0;

        when(restTemplate.getForObject(ENDPOINT + "/drones", Drone[].class)).thenReturn(new Drone[]{d});
        when(restTemplate.getForObject(ENDPOINT + "/drones-for-service-points", DroneForServicePoint[].class))
                .thenReturn(createDayAvailability("D4", "MONDAY"));

        List<String> result = droneService.queryAvailableDrones(List.of(rec));
        assertTrue(result.contains("D4"), "Drone meeting all criteria should be partitioned as 'Eligible'");
    }
}