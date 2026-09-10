package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ENU 站网平差的完整结果。完整协方差模式下，两个协方差矩阵均按 parameterKeys 排列，
 * 并包含站点之间的协方差项。精简模式下，顶层矩阵为空，具体可用性由 diagnostics 标识。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ENUNetworkResult {

    /** 输入站网的名称。 */
    @SerializedName("name")
    private String name;

    /** 各固定站及自由站的 ENU 坐标和精度结果。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各基线的 ENU 观测值、平差值、残差及质量诊断。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 各随机控制坐标先验的平差结果；没有先验时为空。 */
    @SerializedName("priors")
    private List<Prior> priors;

    /** 自由站坐标参数的排列顺序，对应完整协方差矩阵的行、列。 */
    @SerializedName("parameter_keys")
    private List<String> parameterKeys;

    /** 单位权方差取 1 时的完整参数形式协方差，单位平方米，按 parameterKeys 排列；仅 FULL 模式输出。 */
    @SerializedName("formal_covariance")
    private Geometry.Matrix formalCovariance;

    /** 完整参数实际协方差，等于 formalCovariance 乘以 sigma0²，单位平方米；仅 FULL 模式输出。 */
    @SerializedName("covariance")
    private Geometry.Matrix covariance;

    /** 站网规模、自由度、求解器、收敛状态及结果可用性诊断。 */
    @SerializedName("diagnostics")
    private NetworkDiagnostics diagnostics;

    /** 各基线组的方差分量估计结果；未启用估计时为空。 */
    @SerializedName("variance_components")
    private List<NetworkDiagnostics.VarianceComponent> varianceComponents;

    /** 不导致本次平差失败的告警信息，例如条件数过大或迭代未收敛。 */
    @SerializedName("warnings")
    private List<String> warnings;

    /**
     * 公共 ENU 坐标系中的固定站或平差后站点结果。
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

        /** 公共 ENU 坐标系中的固定坐标或平差坐标，单位米。 */
        @SerializedName("position")
        private Geometry.ENU position;

        /** E、N、U 三方向的坐标标准差，单位米；需 stationCovarianceAvailable 为 true。 */
        @SerializedName("stddev")
        private Geometry.ENU stdDev;

        /** 单位权方差取 1 时本站的 3×3 ENU 形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_covariance")
        private Geometry.Matrix3 formalCovariance;

        /** 本站的 3×3 ENU 实际协方差，等于 formalCovariance 乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("covariance")
        private Geometry.Matrix3 covariance;

        /** 输入站点的自定义元数据。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 基线的观测向量、平差向量及质量诊断。残差为观测值减去平差值。
     * weight 是作用于整条 ENU 向量的鲁棒权值，未启用鲁棒平差时为 1。
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

        /** 输入的 ENU 基线观测向量，单位米。 */
        @SerializedName("observed")
        private Geometry.ENU observed;

        /** 平差后的 ENU 基线向量，等于终点坐标减起点坐标，单位米。 */
        @SerializedName("adjusted")
        private Geometry.ENU adjusted;

        /** ENU 残差，等于观测值减去平差值，单位米。 */
        @SerializedName("residual")
        private Geometry.ENU residual;

        /** E、N、U 各分量的残差标准差，单位米；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("residual_stddev")
        private Geometry.ENU residualStdDev;

        /** E、N、U 各分量残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** E、N、U 各分量的观测冗余度，无量纲，范围 [0, 1]；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("redundancy")
        private Geometry.ENU redundancy;

        /** 单位权方差取 1 时的 3×3 ENU 残差形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_residual_covariance")
        private Geometry.Matrix3 formalResidualCovariance;

        /** 3×3 ENU 残差实际协方差，等于形式协方差乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("residual_covariance")
        private Geometry.Matrix3 residualCovariance;

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
     * 随机控制坐标先验对应的平差坐标和质量诊断，残差为观测值减去平差值。
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

        /** 输入的 ENU 随机控制坐标，单位米。 */
        @SerializedName("observed")
        private Geometry.ENU observed;

        /** 该先验对应站点的平差后 ENU 坐标，单位米。 */
        @SerializedName("adjusted")
        private Geometry.ENU adjusted;

        /** ENU 残差，等于观测值减去平差值，单位米。 */
        @SerializedName("residual")
        private Geometry.ENU residual;

        /** E、N、U 各分量的残差标准差，单位米；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("residual_stddev")
        private Geometry.ENU residualStdDev;

        /** E、N、U 各分量先验残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** 先验 E、N、U 各分量的观测冗余度，无量纲，范围 [0, 1]；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("redundancy")
        private Geometry.ENU redundancy;

        /** 单位权方差取 1 时的 3×3 ENU 残差形式协方差，单位平方米；NONE 模式下不可用。 */
        @SerializedName("formal_residual_covariance")
        private Geometry.Matrix3 formalResidualCovariance;

        /** 3×3 ENU 残差实际协方差，等于形式协方差乘以 sigma0²，单位平方米；NONE 模式下不可用。 */
        @SerializedName("residual_covariance")
        private Geometry.Matrix3 residualCovariance;

        /** 输入坐标先验的自定义元数据。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }
}
