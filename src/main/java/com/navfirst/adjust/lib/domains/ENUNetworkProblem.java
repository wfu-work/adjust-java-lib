package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ENU 站网平差的完整输入，包括站点、基线观测和可选的坐标先验。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ENUNetworkProblem {

    /** 站网名称，可选，原样返回到平差结果。 */
    @SerializedName("name")
    private String name;

    /** 站点列表，包括固定站和参与求解的自由站。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 基线向量观测列表，全部采用相同的 ENU 坐标系。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 自由站的随机控制坐标先验列表，可选。 */
    @SerializedName("priors")
    private List<Prior> priors;

    /**
     * ENU 站网中的站点。固定站必须提供 knownENU；自由站直接参与求解，无需提供初始坐标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {

        /** 站点唯一标识，用于基线端点和坐标先验引用。 */
        @SerializedName("id")
        private String id;

        /** 站点名称，可选。 */
        @SerializedName("name")
        private String name;

        /** 是否为固定站；为 true 时 knownENU 作为精确约束，不参与坐标估计。 */
        @SerializedName("fixed")
        private boolean fixed;

        /** 固定站的已知 ENU 坐标，单位米；固定站必填，自由站无需提供。 */
        @SerializedName("known_enu")
        private Geometry.ENU knownENU;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 从 from 站指向 to 站的相对向量观测。一个站网中的所有基线必须使用相同的 ENU 坐标轴，单位为米。
     */
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

        /** 观测向量的 3×3 ENU 协方差，单位平方米；REQUIRED 策略要求有限、对称且正定，UNIT 策略忽略该值。 */
        @SerializedName("covariance")
        private Geometry.Matrix3 covariance;

        /** 基线分组，用于方差分量估计；空分组也作为一个分量。 */
        @SerializedName("group")
        private String group;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 自由站的随机控制坐标先验。position 和 covariance 与所有基线使用同一 ENU 坐标系。
     * 先验参与平差，其残差可以非零；固定站坐标则作为精确约束。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prior {

        /** 坐标先验记录的唯一标识。 */
        @SerializedName("id")
        private String id;

        /** 先验约束的自由站 ID，必须对应 stations 中的站点。 */
        @SerializedName("station_id")
        private String stationID;

        /** 先验 ENU 坐标，单位米，与基线使用相同的公共坐标系。 */
        @SerializedName("position")
        private Geometry.ENU position;

        /** 先验坐标的 3×3 ENU 协方差，单位平方米；REQUIRED 策略要求有限、对称且正定，UNIT 策略忽略该值。 */
        @SerializedName("covariance")
        private Geometry.Matrix3 covariance;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }
}
