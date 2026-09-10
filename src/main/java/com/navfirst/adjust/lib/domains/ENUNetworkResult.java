package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** ENU 平差的核心结果，包括站点坐标、精度、基线残差和质量诊断。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ENUNetworkResult {

    /** 各固定站及自由站的 ENU 坐标和精度结果。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各基线的 ENU 平差向量、残差和鲁棒权值。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 单位权标准差、自由度、收敛状态、精度可用性及整体检验摘要。 */
    @SerializedName("diagnostics")
    private NetworkDiagnostics diagnostics;

    /** 不导致本次平差失败的告警信息，例如条件数过大或迭代未收敛。 */
    @SerializedName("warnings")
    private List<String> warnings;

    /** 公共 ENU 坐标系中的站点坐标与精度；固定站标准差为零。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，与输入站点对应。 */
        @SerializedName("id")
        private String id;

        /** 公共 ENU 坐标系中的固定坐标或平差坐标，单位米。 */
        @SerializedName("position")
        private Geometry.ENU position;

        /** E、N、U 三方向的坐标标准差，单位米；需 stationCovarianceAvailable 为 true。 */
        @SerializedName("stddev")
        private Geometry.ENU stdDev;
    }

    /** ENU 基线平差结果；残差为观测值减去平差值。 */
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

        /** 平差后的 ENU 基线向量，等于终点坐标减起点坐标，单位米。 */
        @SerializedName("adjusted")
        private Geometry.ENU adjusted;

        /** ENU 残差，等于观测值减去平差值，单位米。 */
        @SerializedName("residual")
        private Geometry.ENU residual;

        /** E、N、U 各分量残差除以其标准差后的标准化值，无量纲；需 residualDiagnosticsAvailable 为 true。 */
        @SerializedName("standardized")
        private Geometry.ENU standardized;

        /** 作用于整条基线的鲁棒权值；未启用鲁棒平差时为 1。 */
        @SerializedName("weight")
        private double weight;
    }
}
