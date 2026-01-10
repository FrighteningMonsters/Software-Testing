package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.ac.ed.acp.cw2.data.DistanceRequest;
import uk.ac.ed.acp.cw2.data.IsInRegionRequest;
import uk.ac.ed.acp.cw2.data.NextPositionRequest;
import uk.ac.ed.acp.cw2.data.Position;
import uk.ac.ed.acp.cw2.data.Region;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GeoService geoService;



}
