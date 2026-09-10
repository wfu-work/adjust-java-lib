package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * WGS84 站网平差的完整结果。完整协方差矩阵采用各站的全局 ECEF XYZ 参数块，
 * 按 parameterKeys 排列，并保留站点之间的协方差项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeodeticNetworkResult {

    /** 输入站网的名称。 */
    @SerializedName("name")
    private String name;

    /** 本次平差使用的公共 ECEF 偏移坐标原点，以 WGS84 大地坐标表示。 */
    @SerializedName("common_frame_origin_wgs84")
    private Geometry.WGS84Coordinate commonFrameOriginWGS84;

    /** 各站点的 WGS84、ECEF 坐标及精度结果。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各基线在其输入站心 ENU 坐标系中的平差结果和质量诊断。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 各随机 WGS84 控制坐标先验的平差结果；没有先验时为空。 */
    @SerializedName("priors")
    private List<Prior> priors;

    /** 完整 ECEF 协方差矩阵的参数顺序，每个自由站对应 x、y、z 三个参数。 */
    @SerializedName("parameter_keys")
    private List<String> parameterKeys;

    /** 单位权方差取 1 时的完整 ECEF XYZ 形式协方差，单位平方米，按 parameterKeys 排列；仅 FULL 模式输出。 */
    @SerializedName("formal_covariance_xyz")
    private Geometry.Matrix formalCovarianceXYZ;

    /** 完整 ECEF XYZ 实际协方差，等于 formalCovarianceXYZ 乘以 sigma0²，单位平方米；仅 FULL 模式输出。 */
    @SerializedName("covariance_xyz")
    private Geometry.Matrix covarianceXYZ;

    /** 站网规模、自由度、求解器、收敛状态及结果可用性诊断。 */
    @SerializedName("diagnostics")
    private NetworkDiagnostics diagnostics;

    /** 各基线组的方差分量估计结果；未启用估计时为空。 */
    @SerializedName("variance_components")
    private List<NetworkDiagnostics.VarianceComponent> varianceComponents;

    /** 不导致本次平差失败的告警信息，例如条件数过大或迭代未收敛。 */
    @SerializedName("warnings")
    private List<String> warnings;

    /** 底层 ENU 模型承载的中间平差结果；其中 east、north、up 实际对应公共原点的 ECEF X、Y、Z 偏移量。 */
    @SerializedName("common_enu_result")
    private ENUNetworkResult commonENUResult;

    /**
     * 平差后的 WGS84 站点坐标及其协方差，协方差采用全局 WGS84 ECEF XYZ 坐标系。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，与输入站点对应。 */
        @SerializedName("id")
        private String id;

        /** 站点名称。 */
        @SerializedName("name")
        private String name;

        /** 是否为固定站；固定站坐标保持精确控制值，协方差及标准差为零。 */
        @SerializedName("fixed")
        private boolean fixed;

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

        /** 单位权方差取 1 时本站的 3×3 ECEF XYZ 形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_covariance_xyz")
        private Geometry.Matrix3 formalCovarianceXYZ;

        /** 本站的 3×3 ECEF XYZ 实际协方差，等于 formalCovarianceXYZ 乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("covariance_xyz")
        private Geometry.Matrix3 covarianceXYZ;

        /** 该站所在连通分量是否由固定站或随机控制点锚定到绝对 WGS84 基准。 */
        @SerializedName("wgs84_absolute")
        private boolean wgs84Absolute;

        /** 输入站点的自定义元数据。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 基线的平差和质量诊断结果，采用输入基线的站心 ENU 坐标系。
     */
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

        /** 基线所属分组，用于对应方差分量估计结果。 */
        @SerializedName("group")
        private String group;

        /** 确定本基线结果站心 ENU 坐标轴的 WGS84 原点。 */
        @SerializedName("frame_wgs84")
        private Geometry.WGS84Coordinate frameWGS84;

        /** 输入的站心 ENU 基线观测向量，单位米。 */
        @SerializedName("observed_enu")
        private Geometry.ENU observedENU;

        /** 平差后投影到本基线站心 ENU 坐标系的基线向量，单位米。 */
        @SerializedName("adjusted_enu")
        private Geometry.ENU adjustedENU;

        /** 站心 ENU 基线残差，等于 observedENU 减去 adjustedENU，单位米。 */
        @SerializedName("residual_enu")
        private Geometry.ENU residualENU;

        /** 站心 E、N、U 各分量的残差标准差，单位米；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("residual_stddev_enu")
        private Geometry.ENU residualStdDevENU;

        /** E、N、U 各分量残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** E、N、U 各分量的观测冗余度，无量纲，范围 [0, 1]；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("redundancy")
        private Geometry.ENU redundancy;

        /** 单位权方差取 1 时的 3×3 站心 ENU 残差形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_residual_covariance_enu")
        private Geometry.Matrix3 formalResidualCovarianceENU;

        /** 3×3 站心 ENU 残差实际协方差，等于形式协方差乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("residual_covariance_enu")
        private Geometry.Matrix3 residualCovarianceENU;

        /** 作用于整条基线的鲁棒权值；未启用鲁棒平差时为 1。 */
        @SerializedName("weight")
        private double weight;

        /** 是否因鲁棒估计被降权，即 weight 小于 1。 */
        @SerializedName("downweighted")
        private boolean downweighted;

        /** 基线组的协方差倍率，无量纲；未启用方差分量估计时为 1，观测协方差按该倍率缩放后再除以 weight。 */
        @SerializedName("variance_scale")
        private double varianceScale;

        /** 输入基线的自定义元数据。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 随机控制坐标先验的残差及质量诊断，采用该先验坐标处的站心 ENU 坐标系。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prior {

        /** 坐标先验记录的唯一标识。 */
        @SerializedName("id")
        private String id;

        /** 该先验对应的站点 ID。 */
        @SerializedName("station_id")
        private String stationID;

        /** 输入的随机 WGS84 控制坐标，经纬度单位度，椭球高单位米。 */
        @SerializedName("observed_wgs84")
        private Geometry.WGS84Coordinate observedWGS84;

        /** 该先验对应站点的平差后 WGS84 坐标，经纬度单位度，椭球高单位米。 */
        @SerializedName("adjusted_wgs84")
        private Geometry.WGS84Coordinate adjustedWGS84;

        /** 观测坐标减去平差坐标所得的空间残差，投影到先验坐标处的站心 ENU，单位米。 */
        @SerializedName("residual_enu")
        private Geometry.ENU residualENU;

        /** 站心 E、N、U 各分量的残差标准差，单位米；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("residual_stddev_enu")
        private Geometry.ENU residualStdDevENU;

        /** E、N、U 各分量先验残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** 先验 E、N、U 各分量的观测冗余度，无量纲，范围 [0, 1]；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("redundancy")
        private Geometry.ENU redundancy;

        /** 单位权方差取 1 时的 3×3 站心 ENU 残差形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_residual_covariance_enu")
        private Geometry.Matrix3 formalResidualCovarianceENU;

        /** 3×3 站心 ENU 残差实际协方差，等于形式协方差乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("residual_covariance_enu")
        private Geometry.Matrix3 residualCovarianceENU;

        /** 输入坐标先验的自定义元数据。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }
}
