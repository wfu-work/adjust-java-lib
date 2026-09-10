package com.navfirst.adjust.lib;

import com.navfirst.adjust.lib.domains.*;
import com.navfirst.adjust.lib.services.AdjustLibService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AdjustJavaLibApplicationTests {

    private final AdjustLibService adjustLibService;

    @Test
    void getVersion() {
        String version = this.adjustLibService.getVersion();
        assertThat(version).isNotBlank();
    }

    @Test
    void startAdjust() {
        ENUNetworkProblem problem = ENUNetworkProblem.builder()
                .stations(List.of(
                        ENUNetworkProblem.Station.builder().id("A").fixed(true)
                                .knownENU(new Geometry.ENU(0, 0, 0)).build(),
                        ENUNetworkProblem.Station.builder().id("B").build()))
                .baselines(List.of(
                        ENUNetworkProblem.Baseline.builder().id("AB1").from("A").to("B")
                                .vector(new Geometry.ENU(10, 2, 0.5))
                                .covariance(Geometry.Matrix3.fromStdDev(1, 1, 1)).build(),
                        ENUNetworkProblem.Baseline.builder().id("AB2").from("A").to("B")
                                .vector(new Geometry.ENU(12, 4, 1.5))
                                .covariance(Geometry.Matrix3.fromStdDev(2, 2, 2)).build()))
                .build();

        ENUNetworkResult result = this.adjustLibService.startAdjust(problem);
        result.getStations().forEach(station ->
                System.out.println(station.getId() + ": " + station.getPosition()));
    }

    @Test
    void startAdjustWgs84() {
        Geometry.WGS84Coordinate origin = new Geometry.WGS84Coordinate(30.5, 114.3, 50);
        GeodeticNetworkProblem wgs84Problem = GeodeticNetworkProblem.builder()
                .stations(List.of(
                        GeodeticNetworkProblem.Station.builder().id("A").exactWGS84(origin).build(),
                        GeodeticNetworkProblem.Station.builder().id("B").build()))
                .baselines(List.of(
                        GeodeticNetworkProblem.Baseline.builder().id("AB1").from("A").to("B")
                                .frameWGS84(origin).vectorENU(new Geometry.ENU(10, 2, 0.5))
                                .covarianceENU(Geometry.Matrix3.fromStdDev(0.01, 0.01, 0.02)).build(),
                        GeodeticNetworkProblem.Baseline.builder().id("AB2").from("A").to("B")
                                .frameWGS84(origin).vectorENU(new Geometry.ENU(10.01, 2.01, 0.52))
                                .covarianceENU(Geometry.Matrix3.fromStdDev(0.01, 0.01, 0.02)).build()))
                .build();

        GeodeticNetworkResult wgs84Result = this.adjustLibService.startAdjustWgs84(wgs84Problem);
        wgs84Result.getStations().forEach(station -> {
            System.out.println(station.getPositionWGS84());
            System.out.println(station.getPositionECEF());
            if (wgs84Result.getDiagnostics().isStationCovarianceAvailable()) {
                System.out.println(station.getStdDevXYZ());
                System.out.println(station.getMXYZ());
            }
        });
    }

}
