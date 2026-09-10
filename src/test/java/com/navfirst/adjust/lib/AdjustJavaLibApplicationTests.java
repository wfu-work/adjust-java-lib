package com.navfirst.adjust.lib;

import com.navfirst.adjust.lib.domains.GeodeticNetworkProblem;
import com.navfirst.adjust.lib.domains.GeodeticNetworkResult;
import com.navfirst.adjust.lib.domains.Geometry;
import com.navfirst.adjust.lib.services.AdjustLibService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AdjustJavaLibApplicationTests {

    private final AdjustLibService adjustLibService;

    @Test
    void getVersion() {
        String version = this.adjustLibService.getVersion();
        System.out.println("version: " + version);
    }

    @Test
    void contextLoads() {
        Geometry.WGS84Coordinate origin = new Geometry.WGS84Coordinate(30.5, 114.3, 50);
        GeodeticNetworkProblem problem = GeodeticNetworkProblem.builder()
                .stations(List.of(
                        GeodeticNetworkProblem.Station.builder().id("A").exactWGS84(origin)
                                .approximateWGS84(origin).build(),
                        GeodeticNetworkProblem.Station.builder().id("B").build()))
                .baselines(List.of(
                        GeodeticNetworkProblem.Baseline.builder().id("AB").from("A").to("B")
                                .frameWGS84(origin).vectorENU(new Geometry.ENU(10, 2, 0.5))
                                .covarianceENU(Geometry.Matrix3.fromStdDev(0.01, 0.01, 0.02)).build()))
                .build();

        GeodeticNetworkResult result = this.adjustLibService.startAdjustWgs84(problem);
        result.getStations().forEach(station -> {
            System.out.println(station.getPositionWGS84());
            System.out.println(station.getPositionECEF());
        });
    }

}
