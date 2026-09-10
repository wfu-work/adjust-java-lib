package com.navfirst.adjust.lib.services;

import com.navfirst.adjust.lib.domains.AdjustOptions;
import com.navfirst.adjust.lib.domains.ENUNetworkProblem;
import com.navfirst.adjust.lib.domains.ENUNetworkResult;
import com.navfirst.adjust.lib.domains.GeodeticNetworkProblem;
import com.navfirst.adjust.lib.domains.GeodeticNetworkResult;

/**
 * 同步站网平差 SDK，可作为普通 Java 对象或 Spring Bean 使用。
 * 调用之间无共享解算状态，可以并发调用；调用期间不要修改输入对象。
 * 原始字符串入口返回动态库响应；强类型入口将错误信封转换为 AdjustException。
 */
public interface AdjustLibService {

    /** 返回 C ABI 版本。 */
    String getVersion();

    /** 原始 JSON 入口，原样返回完整的 ok/result/error 信封。支持 ENU 或 geodetic_problem。 */
    String startAdjust(String requestJson);

    /** WGS84 JSON 入口，接受直接问题或请求信封，原样返回完整响应信封。 */
    String startAdjustWgs84(String requestJson);

    /** 使用默认核心配置执行 ENU 平差。 */
    default ENUNetworkResult startAdjust(ENUNetworkProblem problem) {
        return startAdjust(problem, null);
    }

    /** 执行 ENU 平差，可设置超时和鲁棒开关；options 可为 null。 */
    ENUNetworkResult startAdjust(ENUNetworkProblem problem, AdjustOptions options);

    /** 使用默认核心配置执行 WGS84 平差；每条基线必须提供 frameWGS84。 */
    default GeodeticNetworkResult startAdjustWgs84(GeodeticNetworkProblem problem) {
        return startAdjustWgs84(problem, null);
    }

    /** 执行 WGS84 平差，可设置超时和鲁棒开关；options 可为 null。 */
    GeodeticNetworkResult startAdjustWgs84(GeodeticNetworkProblem problem, AdjustOptions options);

}
