package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 请求信封。problem 与 geodeticProblem 必须且只能设置一个。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustRequest {

    /** 公共 ENU 坐标系下的站网输入，与 geodeticProblem 必须且只能设置一个。 */
    @SerializedName("problem")
    private ENUNetworkProblem problem;

    /** WGS84 站点及站心 ENU 基线输入，与 problem 必须且只能设置一个。 */
    @SerializedName("geodetic_problem")
    private GeodeticNetworkProblem geodeticProblem;

    /** 平差选项；为 null 时采用 Go 库默认配置。 */
    @SerializedName("options")
    private ENUNetworkOptions options;

    /** 协作式超时，单位毫秒；null 或 0 表示不限时，不能强制中断正在进行的稠密矩阵分解。 */
    @SerializedName("timeout_ms")
    private Long timeoutMs;
}
