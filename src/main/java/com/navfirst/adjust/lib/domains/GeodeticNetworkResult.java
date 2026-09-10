package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** WGS84 平差的核心结果；站点精度采用 ECEF XYZ，基线残差采用输入站心 ENU。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeodeticNetworkResult {

    /** 各站点的 WGS84、ECEF 坐标及精度结果。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各基线在其输入站心 ENU 坐标系中的平差结果和质量诊断。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 单位权标准差、自由度、收敛状态、精度可用性及整体检验摘要。 */
    @SerializedName("diagnostics")
    private NetworkDiagnostics diagnostics;

    /** 不导致本次平差失败的告警信息，例如条件数过大或迭代未收敛。 */
    @SerializedName("warnings")
    private List<String> warnings;

    /** 站点的 WGS84、ECEF 坐标与精度；固定站标准差为零。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，与输入站点对应。 */
        @SerializedName("id")
        private String id;

        /** 平差后的 WGS84 大地坐标，经纬度单位度，椭球高单位米。 */
        @SerializedName("position_wgs84")
        private Geometry.WGS84Coordinate positionWGS84;

        /** 平差后的 WGS84 地心地固 XYZ 坐标，单位米。 */
        @SerializedName("position_ecef")
        private Geometry.ECEFCoordinate positionECEF;

        /** ECEF X、Y、Z 三方向的坐标标准差，单位米；需 stationCovarianceAvailable 为 true。 */
        @SerializedName("stddev_xyz")
        private Geometry.ECEFCoordinate stdDevXYZ;

        /** 三维点位中误差，等于 sqrt(σX² + σY² + σZ²)，单位米；需 stationCovarianceAvailable 为 true。 */
        @SerializedName("m_xyz")
        private double mXYZ;

        /** 该站所在连通分量是否由固定站或随机控制点锚定到绝对 WGS84 基准。 */
        @SerializedName("wgs84_absolute")
        private boolean wgs84Absolute;
    }

    /** 基线平差结果，坐标轴由输入基线的 frameWGS84 确定。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Baseline {

        /** 基线观测记录的唯一标识。 */
        @SerializedName("id")
        private String id;

        /** 基线起点站 ID。 */
        @SerializedName("from")
        private String from;

        /** 基线终点站 ID。 */
        @SerializedName("to")
        private String to;

        /** 平差后投影到本基线站心 ENU 坐标系的基线向量，单位米。 */
        @SerializedName("adjusted_enu")
        private Geometry.ENU adjustedENU;

        /** 站心 ENU 基线残差，等于输入观测向量减去 adjustedENU，单位米。 */
        @SerializedName("residual_enu")
        private Geometry.ENU residualENU;

        /** E、N、U 各分量残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** 作用于整条基线的鲁棒权值；未启用鲁棒平差时为 1。 */
        @SerializedName("weight")
        private double weight;
    }
}
