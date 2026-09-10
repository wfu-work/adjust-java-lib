package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 公共 ENU 站网输入，由固定控制点、自由站和基线观测组成。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ENUNetworkProblem {

    /** 站点列表，包括固定站和参与求解的自由站。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 基线向量观测列表，全部采用相同的 ENU 坐标系。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** ENU 站点；固定站必须提供已知坐标，自由站只需提供 ID。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，用于基线端点引用。 */
        @SerializedName("id")
        private String id;

        /** 是否为固定站；为 true 时 knownENU 作为精确约束，不参与坐标估计。 */
        @SerializedName("fixed")
        private boolean fixed;

        /** 固定站的已知 ENU 坐标，单位米；固定站必填，自由站无需提供。 */
        @SerializedName("known_enu")
        private Geometry.ENU knownENU;
    }

    /** 从起点到终点的观测向量；所有基线使用相同的 ENU 坐标轴。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Baseline {

        /** 基线观测记录的唯一标识。 */
        @SerializedName("id")
        private String id;

        /** 起点站 ID，必须对应 stations 中的站点。 */
        @SerializedName("from")
        private String from;

        /** 终点站 ID，必须对应 stations 中的站点。 */
        @SerializedName("to")
        private String to;

        /** 从起点到终点的 ENU 观测向量，单位米，即终点坐标减起点坐标。 */
        @SerializedName("vector")
        private Geometry.ENU vector;

        /** 观测向量的 3×3 ENU 协方差，单位平方米，必填且要求有限、对称、正定。 */
        @SerializedName("covariance")
        private Geometry.Matrix3 covariance;
    }
}
