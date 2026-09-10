package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站网平差和数值求解的诊断汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkDiagnostics {

    /** 站点总数。 */
    @SerializedName("station_count")
    private int stationCount;

    /** 固定站数量。 */
    @SerializedName("fixed_station_count")
    private int fixedStationCount;

    /** 自由站数量。 */
    @SerializedName("free_station_count")
    private int freeStationCount;

    /** 基线数量。 */
    @SerializedName("baseline_count")
    private int baselineCount;

    /** 坐标先验数量。 */
    @SerializedName("prior_count")
    private int priorCount;

    /** 参与平差的观测分量数量，基线和先验各提供 3 个分量。 */
    @SerializedName("observation_count")
    private int observationCount;

    /** 自由坐标参数数量。 */
    @SerializedName("parameter_count")
    private int parameterCount;

    /** 法方程或约束系统的数值秩。 */
    @SerializedName("rank")
    private int rank;

    /** 观测自由度，等于有效方程数加内部约束数减参数数。 */
    @SerializedName("degrees_of_freedom")
    private int degreesOfFreedom;

    /** 加权残差平方和，通常为残差按观测协方差加权后的平方和。 */
    @SerializedName("objective")
    private double objective;

    /** 后验单位权标准差；无正自由度时通常为 1。 */
    @SerializedName("sigma0")
    private double sigma0;

    /** 求解系统的条件数；不可用时不要据此判断数值稳定性。 */
    @SerializedName("condition_number")
    private double conditionNumber;

    /** 是否成功计算 conditionNumber。 */
    @SerializedName("condition_number_available")
    private boolean conditionNumberAvailable;

    /** 实际使用的求解器名称。 */
    @SerializedName("solver")
    private String solver;

    /** 实际使用的 PCG 预条件器名称；非 PCG 时可能为空。 */
    @SerializedName("solver_preconditioner")
    private String solverPreconditioner;

    /** PCG 求解器迭代次数；非迭代求解时通常为 0。 */
    @SerializedName("solver_iterations")
    private int solverIterations;

    /** PCG 最终相对残差；非迭代求解时可能为 0。 */
    @SerializedName("solver_relative_residual")
    private double solverRelativeResidual;

    /** 实际协方差输出模式。 */
    @SerializedName("covariance_mode")
    private ENUNetworkOptions.CovarianceMode covarianceMode;

    /** 实际采用的观测协方差策略。 */
    @SerializedName("covariance_policy")
    private ENUNetworkOptions.CovariancePolicy covariancePolicy;

    /** 实际使用的单位方差。 */
    @SerializedName("unit_variance")
    private double unitVariance;

    /** 是否提供完整参数协方差矩阵。 */
    @SerializedName("full_covariance_available")
    private boolean fullCovarianceAvailable;

    /** 是否提供站点协方差块。 */
    @SerializedName("station_covariance_available")
    private boolean stationCovarianceAvailable;

    /** 是否提供残差协方差、标准化残差和冗余度。 */
    @SerializedName("residual_diagnostics_available")
    private boolean residualDiagnosticsAvailable;

    /** 实际采用的基准模式。 */
    @SerializedName("datum_mode")
    private ENUNetworkOptions.DatumMode datumMode;

    /** 施加内部质心基准的无锚定连通分量数量。 */
    @SerializedName("free_datum_component_count")
    private int freeDatumComponentCount;

    /** 内部基准约束数量，通常为每个自由分量 3 个。 */
    @SerializedName("internal_datum_constraint_count")
    private int internalDatumConstraintCount;

    /** 站网外层平差迭代次数；普通平差通常为 1，包含鲁棒重加权时反映其迭代次数。 */
    @SerializedName("iterations")
    private int iterations;

    /** 外层平差是否达到收敛条件；鲁棒迭代达到上限时为 false。 */
    @SerializedName("converged")
    private boolean converged;

    /** 方差分量估计迭代次数；未启用时通常为 0。 */
    @SerializedName("variance_component_iterations")
    private int varianceComponentIterations;

    /** 是否启用了方差分量估计并生成相关结果。 */
    @SerializedName("variance_components_available")
    private boolean varianceComponentsAvailable;

    /** 方差分量估计是否收敛。 */
    @SerializedName("variance_components_converged")
    private boolean varianceComponentsConverged;

    /** 整体随机模型卡方检验结果；启用鲁棒重加权或方差分量估计时通常为空。 */
    @SerializedName("global_test")
    private GlobalTest globalTest;

    /**
     * 用于检验整体随机模型的双侧卡方检验结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalTest {

        /** 整体模型检验统计量。 */
        @SerializedName("statistic")
        private double statistic;

        /** 检验使用的自由度。 */
        @SerializedName("degrees_of_freedom")
        private int degreesOfFreedom;

        /** 检验置信度。 */
        @SerializedName("confidence")
        private double confidence;

        /** 给定置信度下卡方统计量的下界。 */
        @SerializedName("lower")
        private double lower;

        /** 给定置信度下卡方统计量的上界。 */
        @SerializedName("upper")
        private double upper;

        /** 统计量是否落在 [lower, upper] 区间内。 */
        @SerializedName("passed")
        private boolean passed;
    }

    /**
     * 一个基线组的协方差倍率估计结果。标准差倍率 stdDevScale 为协方差倍率 scale 的平方根。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianceComponent {

        /** 基线分组名称；结果按分组名称排序。 */
        @SerializedName("group")
        private String group;

        /** 该组观测协方差的估计倍率，无量纲。 */
        @SerializedName("scale")
        private double scale;

        /** 该组标准差倍率，等于 sqrt(scale)，无量纲。 */
        @SerializedName("stddev_scale")
        private double stdDevScale;

        /** 该组基线数量。 */
        @SerializedName("baseline_count")
        private int baselineCount;

        /** 该组观测分量数量。 */
        @SerializedName("observation_count")
        private int observationCount;

        /** 该组用于估计方差倍率的加权残差目标值。 */
        @SerializedName("objective")
        private double objective;

        /** 该组用于估计方差倍率的总冗余度。 */
        @SerializedName("redundancy")
        private double redundancy;
    }
}
