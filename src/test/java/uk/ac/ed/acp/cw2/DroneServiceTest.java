package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.service.DroneService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class DroneServiceTest {
    @Autowired
    DroneService droneService;

    @Test
    public void testCalcDeliveryPathWithRealCoordinates() {
        List<MedDispatchRec> recs = new ArrayList<>();

        MedDispatchRec r1 = new MedDispatchRec();
        r1.id = 1;
        r1.date = "2025-12-22";
        r1.time = "14:30";
        r1.requirements = new MedDispatchRec.Requirements();
        r1.requirements.capacity = 0.5;
        r1.requirements.cooling = false;
        r1.requirements.heating = false;
        r1.requirements.maxCost = 0.0;
        r1.delivery = new Position();
        r1.delivery.lng = -3.1875;
        r1.delivery.lat = 55.9444;
        recs.add(r1);

        MedDispatchRec r2 = new MedDispatchRec();
        r2.id = 2;
        r2.date = "2025-12-22";
        r2.time = "15:00";
        r2.requirements = new MedDispatchRec.Requirements();
        r2.requirements.capacity = 0.5;
        r2.requirements.cooling = false;
        r2.requirements.heating = false;
        r2.requirements.maxCost = 0.0;
        r2.delivery = new Position();
        r2.delivery.lng = -3.1883;
        r2.delivery.lat = 55.9450;
        recs.add(r2);

        MedDispatchRec r3 = new MedDispatchRec();
        r3.id = 3;
        r3.date = "2025-12-22";
        r3.time = "15:20";
        r3.requirements = new MedDispatchRec.Requirements();
        r3.requirements.capacity = 0.5;
        r3.requirements.cooling = false;
        r3.requirements.heating = false;
        r3.requirements.maxCost = 0.0;
        r3.delivery = new Position();
        r3.delivery.lng = -3.1870;
        r3.delivery.lat = 55.9453;
        recs.add(r3);

        CalcDeliveryPathResult result = droneService.calcDeliveryPath(recs);

        System.out.println("Total Cost: " + result.totalCost);
        System.out.println("Total Moves: " + result.totalMoves);
        System.out.println("DronePaths: " + result.dronePaths.size());

        for (DronePath dp : result.dronePaths) {
            System.out.println("Drone: " + dp.droneId);
            for (DeliveryPath d : dp.deliveries) {
                System.out.println("  Delivery: " + d.deliveryId);
                System.out.println("  Moves: " + d.flightPath.size());
                for (Position p : d.flightPath) {
                    System.out.println("    " + p.lng + ", " + p.lat);
                }
            }
        }

        Assertions.assertFalse(result.dronePaths.isEmpty());
        Assertions.assertFalse(result.dronePaths.getFirst().deliveries.isEmpty());
        Assertions.assertTrue(result.totalMoves > 0);
    }

    @Test
    public void testCalcDeliveryPathAsGeoJson() {
        List<MedDispatchRec> recs = new ArrayList<>();

        MedDispatchRec r1 = new MedDispatchRec();
        r1.id = 1;
        r1.date = "2025-12-22";
        r1.time = "14:30";
        r1.requirements = new MedDispatchRec.Requirements();
        r1.requirements.capacity = 0.5;
        r1.requirements.cooling = false;
        r1.requirements.heating = false;
        r1.requirements.maxCost = 0.0;
        r1.delivery = new Position();
        r1.delivery.lng = -3.1875;
        r1.delivery.lat = 55.9444;
        recs.add(r1);

        MedDispatchRec r2 = new MedDispatchRec();
        r2.id = 2;
        r2.date = "2025-12-22";
        r2.time = "15:00";
        r2.requirements = new MedDispatchRec.Requirements();
        r2.requirements.capacity = 0.5;
        r2.requirements.cooling = false;
        r2.requirements.heating = false;
        r2.requirements.maxCost = 0.0;
        r2.delivery = new Position();
        r2.delivery.lng = -3.1883;
        r2.delivery.lat = 55.9450;
        recs.add(r2);

        MedDispatchRec r3 = new MedDispatchRec();
        r3.id = 3;
        r3.date = "2025-12-22";
        r3.time = "15:20";
        r3.requirements = new MedDispatchRec.Requirements();
        r3.requirements.capacity = 0.5;
        r3.requirements.cooling = false;
        r3.requirements.heating = false;
        r3.requirements.maxCost = 0.0;
        r3.delivery = new Position();
        r3.delivery.lng = -3.1870;
        r3.delivery.lat = 55.9453;
        recs.add(r3);

        String geojson = droneService.calcDeliveryPathAsGeoJson(recs);

        System.out.println("Returned GeoJSON:");
        System.out.println(geojson);

        Assertions.assertNotNull(geojson, "GeoJSON must not be null");
        Assertions.assertFalse(geojson.isEmpty(), "GeoJSON must not be empty");
        Assertions.assertTrue(geojson.contains("\"LineString\""), "Must contain LineString type");
        Assertions.assertTrue(geojson.contains("["), "Must contain coordinates");
    }
}
