package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.*;

/**
 * 站网平差选项，用于配置基准定义、数值求解、协方差输出，以及可选的鲁棒估计和方差分量估计。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ENUNetworkOptions {

    /** 鲁棒平差配置；为 null 表示不启用鲁棒重加权。 */
    @SerializedName("robust")
    private Robust robust;

    /** 方差分量估计配置；为 null 表示不估计基线组协方差倍率。 */
    @SerializedName("variance_components")
    private VarianceComponents varianceComponents;

    /** 线性方程求解器配置。 */
    @SerializedName("solver")
    private Solver solver;

    /** 协方差输出模式：FULL 输出完整矩阵，STATION_BLOCKS 输出站点块，NONE 不输出协方差诊断。 */
    @SerializedName("covariance")
    private CovarianceMode covariance;

    /** 观测协方差策略：REQUIRED 校验并使用输入协方差，UNIT 使用等权单位方差。 */
    @SerializedName("covariance_policy")
    private CovariancePolicy covariancePolicy;

    /** UNIT 策略下采用的单位方差，必须为正；默认 1。 */
    @SerializedName("unit_variance")
    private double unitVariance;

    /** 基准模式：EXTERNAL 使用外部控制，FREE_CENTROID 对无锚定分量施加质心内部基准约束。 */
    @SerializedName("datum")
    private DatumMode datum;

    /** 整体模型卡方检验的置信度，范围为 (0, 1)，默认 0.95。 */
    @SerializedName("confidence")
    private double confidence;

    /** 协方差矩阵对称性绝对容差，默认 1e-12。 */
    @SerializedName("symmetry_tolerance")
    private double symmetryTolerance;

    /** 输入协方差独立方差的最小阈值，默认 1e-20。 */
    @SerializedName("minimum_variance")
    private double minimumVariance;

    /** 条件数告警阈值，超过该值会写入 warnings，默认 1e12。 */
    @SerializedName("condition_warn_limit")
    private double conditionWarnLimit;

    /**
     * 求解器配置，支持稠密直接求解、自动选择和稀疏迭代求解。
     * 未设置时采用稠密求解器及 Go 库默认的 PCG 容差。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Solver {

        /** 求解方法：DENSE 稠密直接分解，AUTO 按规模和协方差模式自动选择，PCG 使用预条件共轭梯度。 */
        @SerializedName("method")
        private SolverMethod method;

        /** AUTO 模式采用稠密求解的最大参数数量，默认 300。 */
        @SerializedName("dense_threshold")
        private int denseThreshold;

        /** PCG 预条件器：JACOBI、BLOCK_JACOBI 或 IC0。 */
        @SerializedName("preconditioner")
        private PreconditionerMethod preconditioner;

        /** IC0 预条件器的相对对角稳定化量，必须非负。 */
        @SerializedName("preconditioner_shift")
        private double preconditionerShift;

        /** PCG 最大迭代次数；0 使用默认值 max(100, 10×参数数)。 */
        @SerializedName("max_iterations")
        private int maxIterations;

        /** PCG 相对残差容差，默认 1e-10。 */
        @SerializedName("relative_tolerance")
        private double relativeTolerance;

        /** PCG 绝对残差容差，默认 1e-12。 */
        @SerializedName("absolute_tolerance")
        private double absoluteTolerance;
    }

    /**
     * 按整条基线进行鲁棒重加权的配置。数值参数为零时采用 Go 库默认值。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Robust {

        /** 鲁棒估计方法，目前支持 HUBER。 */
        @SerializedName("method")
        private RobustMethod method;

        /** Huber 重加权阈值，默认 2.5，必须为正。 */
        @SerializedName("threshold")
        private double threshold;

        /** 鲁棒重加权最大迭代次数，默认 10。 */
        @SerializedName("max_iterations")
        private int maxIterations;

        /** 相邻迭代基线权值的最大变化容差，默认 1e-3。 */
        @SerializedName("tolerance")
        private double tolerance;

        /** 鲁棒权值下限，范围为 (0, 1]，默认 0.05。 */
        @SerializedName("min_weight")
        private double minWeight;
    }

    /**
     * 按基线分组估计协方差倍率的配置。每个不同的 group 值对应一个方差分量，
     * 空分组名也可作为默认分量。数值参数为零时采用 Go 库默认值。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianceComponents {

        /** 方差分量估计最大迭代次数，默认 10。 */
        @SerializedName("max_iterations")
        private int maxIterations;

        /** 相邻迭代方差倍率的相对变化容差，默认 1e-3。 */
        @SerializedName("tolerance")
        private double tolerance;

        /** 协方差倍率下限，默认 1e-4。 */
        @SerializedName("min_scale")
        private double minScale;

        /** 协方差倍率上限，默认 1e4。 */
        @SerializedName("max_scale")
        private double maxScale;

        /** 估计方差分量所需的最小组冗余度，默认 1e-6。 */
        @SerializedName("minimum_redundancy")
        private double minimumRedundancy;
    }

    /** Go 库 SolverMethod 的协议取值。 */
    @Getter
    public enum SolverMethod {
        /** DENSE：稠密直接求解。 */
        @SerializedName("dense")
        DENSE("dense"),
        /** AUTO：根据问题规模和协方差模式自动选择。 */
        @SerializedName("auto")
        AUTO("auto"),
        /** PCG：预条件共轭梯度迭代求解。 */
        @SerializedName("pcg")
        PCG("pcg");

        /** 与 Go 库协议对应的字符串值：dense、auto 或 pcg。 */
        private final String value;

        SolverMethod(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Go 库 PreconditionerMethod 的协议取值。 */
    @Getter
    public enum PreconditionerMethod {
        /** JACOBI：标量 Jacobi 预条件器。 */
        @SerializedName("jacobi")
        JACOBI("jacobi"),
        /** BLOCK_JACOBI：按站点坐标块构造的 Jacobi 预条件器。 */
        @SerializedName("block-jacobi")
        BLOCK_JACOBI("block-jacobi"),
        /** IC0：不完全 Cholesky 预条件器。 */
        @SerializedName("ic0")
        IC0("ic0");

        /** 与 Go 库协议对应的字符串值：jacobi、block-jacobi 或 ic0。 */
        private final String value;

        PreconditionerMethod(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Go 库 RobustMethod 的协议取值。 */
    @Getter
    public enum RobustMethod {
        /** HUBER：Huber 鲁棒估计。 */
        @SerializedName("huber")
        HUBER("huber");

        /** 与 Go 库协议对应的字符串值，目前为 huber。 */
        private final String value;

        RobustMethod(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Go 库 CovarianceMode 的协议取值。 */
    @Getter
    public enum CovarianceMode {
        /** FULL：输出完整参数协方差矩阵。 */
        @SerializedName("full")
        FULL("full"),
        /** STATION_BLOCKS：仅输出站点协方差块及相关诊断。 */
        @SerializedName("station-blocks")
        STATION_BLOCKS("station-blocks"),
        /** NONE：不输出协方差相关结果。 */
        @SerializedName("none")
        NONE("none");

        /** 与 Go 库协议对应的协方差输出字符串值。 */
        private final String value;

        CovarianceMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Go 库 CovariancePolicy 的协议取值。 */
    @Getter
    public enum CovariancePolicy {
        /** REQUIRED：要求并使用输入观测协方差。 */
        @SerializedName("required")
        REQUIRED("required"),
        /** UNIT：忽略输入观测协方差并使用等权单位方差。 */
        @SerializedName("unit")
        UNIT("unit");

        /** 与 Go 库协议对应的观测协方差策略字符串值。 */
        private final String value;

        CovariancePolicy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Go 库 DatumMode 的协议取值。 */
    @Getter
    public enum DatumMode {
        /** EXTERNAL：使用固定站或控制先验提供的外部基准。 */
        @SerializedName("external")
        EXTERNAL("external"),
        /** FREE_CENTROID：对无外部基准的分量施加质心内部约束。 */
        @SerializedName("free-centroid")
        FREE_CENTROID("free-centroid");

        /** 与 Go 库协议对应的基准模式字符串值。 */
        private final String value;

        DatumMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
