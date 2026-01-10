package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ed.acp.cw2.controller.ServiceController;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceControllerTest {

    @Mock
    private GeoService geoService;

    @InjectMocks
    private ServiceController serviceController;

    @Test
    void testUid() {
        String result = serviceController.uid();
        assertEquals("s2518554", result);
    }



}
