package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * WGS84 站网平差输入，包括 WGS84 站点和站心 ENU 基线，共用 ENUNetworkOptions 求解选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeodeticNetworkProblem {

    /** 站网名称，可选，原样返回到平差结果。 */
    @SerializedName("name")
    private String name;

    /** 公共 ECEF 偏移坐标的参考原点，可选；仅用于坐标表达，不作为控制约束。 */
    @SerializedName("common_frame_origin_wgs84")
    private Geometry.WGS84Coordinate commonFrameOriginWGS84;

    /** WGS84 站点列表，可包含精确控制站和待估计站。 */
    @SerializedName("stations")
    private List<Station> stations;

    /** 各自采用站心 ENU 坐标系的基线观测列表。 */
    @SerializedName("baselines")
    private List<Baseline> baselines;

    /** 随机 WGS84 控制坐标先验列表，可选。 */
    @SerializedName("priors")
    private List<Prior> priors;

    /**
     * WGS84 站网站点。可选的 approximateWGS84 用于显示或线性坐标系的近似位置，
     * exactWGS84 则将该站设为精确控制点。每条基线可单独提供站心坐标系，因此监测站无需提供 WGS84 坐标。
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

        /** 近似 WGS84 坐标，可选，用于显示或确定基线坐标系，不作为控制约束。 */
        @SerializedName("approximate_wgs84")
        private Geometry.WGS84Coordinate approximateWGS84;

        /** 精确 WGS84 控制坐标；设置后该站成为固定站。 */
        @SerializedName("exact_wgs84")
        private Geometry.WGS84Coordinate exactWGS84;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 从 from 站指向 to 站的基线向量，采用 frameWGS84 处的站心 ENU 坐标系。
     * frameWGS84 为空时，使用起点站的近似坐标确定坐标系。
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

        /** 从起点到终点的站心 ENU 观测向量，单位米，坐标轴由 frameWGS84 确定。 */
        @SerializedName("vector_enu")
        private Geometry.ENU vectorENU;

        /** 该基线站心 ENU 坐标系中的 3×3 协方差，单位平方米；REQUIRED 策略要求有限、对称且正定，UNIT 策略忽略该值。 */
        @SerializedName("covariance_enu")
        private Geometry.Matrix3 covarianceENU;

        /** 确定本基线站心 ENU 坐标轴的 WGS84 原点；为空时使用起点站的近似坐标。 */
        @SerializedName("frame_wgs84")
        private Geometry.WGS84Coordinate frameWGS84;

        /** 基线分组，用于方差分量估计；空分组也作为一个分量。 */
        @SerializedName("group")
        private String group;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }

    /**
     * 随机 WGS84 控制坐标先验，其协方差采用该坐标处的站心 ENU 坐标系。
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

        /** 随机控制点的 WGS84 先验坐标，经纬度单位度，椭球高单位米。 */
        @SerializedName("position_wgs84")
        private Geometry.WGS84Coordinate positionWGS84;

        /** 先验坐标处站心 ENU 的 3×3 协方差，单位平方米；REQUIRED 策略要求有限、对称且正定，UNIT 策略忽略该值。 */
        @SerializedName("covariance_enu")
        private Geometry.Matrix3 covarianceENU;

        /** 用户自定义元数据，不参与平差计算，并随结果返回。 */
        @SerializedName("metadata")
        private Map<String, String> metadata;
    }
}
