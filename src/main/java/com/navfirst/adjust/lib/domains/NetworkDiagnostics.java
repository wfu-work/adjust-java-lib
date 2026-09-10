package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 核心质量诊断；使用精度和标准化残差前应检查相应的可用性标志。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkDiagnostics {

    /** 后验单位权标准差；无正自由度时通常为 1。 */
    @SerializedName("sigma0")
    private double sigma0;

    /** 观测自由度；核心入口为基线观测分量数减自由坐标参数数。 */
    @SerializedName("degrees_of_freedom")
    private int degreesOfFreedom;

    /** 平差是否收敛；普通平差成功时为 true，鲁棒迭代达到上限时为 false。 */
    @SerializedName("converged")
    private boolean converged;

    /** 站点精度是否可用；为 false 时不能用标准差零值表示高精度。 */
    @SerializedName("station_covariance_available")
    private boolean stationCovarianceAvailable;

    /** 标准化残差是否可用；为 false 时仅可使用原始残差。 */
    @SerializedName("residual_diagnostics_available")
    private boolean residualDiagnosticsAvailable;

    /** 整体随机模型检验；无正自由度、发生鲁棒降权或估计方差分量时为空，表示未执行检验。 */
    @SerializedName("global_test")
    private GlobalTest globalTest;

    /** 整体随机模型的双侧卡方检验摘要。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalTest {

        /** 整体模型是否通过给定置信度的双侧卡方检验。 */
        @SerializedName("passed")
        private boolean passed;

        /** 检验置信度。 */
        @SerializedName("confidence")
        private double confidence;
    }
}
