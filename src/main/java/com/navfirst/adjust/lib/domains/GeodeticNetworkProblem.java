package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** WGS84 站网输入，由控制坐标和各自采用站心 ENU 坐标系的基线组成。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeodeticNetworkProblem {

    /** WGS84 站点列表，可包含精确控制站和待估计站。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各自采用站心 ENU 坐标系的基线观测列表。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** WGS84 站点；提供 exactWGS84 表示精确控制站，自由站只需提供 ID。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，用于基线端点引用。 */
        @SerializedName("id")
        private String id;

        /** 精确 WGS84 控制坐标；设置后该站成为固定站。 */
        @SerializedName("exact_wgs84")
        private Geometry.WGS84Coordinate exactWGS84;
    }

    /** 站心 ENU 基线；每条基线必须明确提供 frameWGS84。 */
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

        /** 从起点到终点的站心 ENU 观测向量，单位米，坐标轴由 frameWGS84 确定。 */
        @SerializedName("vector_enu")
        private Geometry.ENU vectorENU;

        /** 本基线站心 ENU 坐标系中的 3×3 协方差，单位平方米，必填且要求有限、对称、正定。 */
        @SerializedName("covariance_enu")
        private Geometry.Matrix3 covarianceENU;

        /** 确定本基线站心 ENU 坐标轴的 WGS84 原点，必填；与精确控制坐标分别设置。 */
        @SerializedName("frame_wgs84")
        private Geometry.WGS84Coordinate frameWGS84;
    }
}
