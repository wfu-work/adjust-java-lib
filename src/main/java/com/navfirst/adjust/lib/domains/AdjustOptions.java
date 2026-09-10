package com.navfirst.adjust.lib.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 核心调用选项。SDK 负责转换为 Go 请求信封，无需调用方拼装协议字段。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustOptions {

    /** 协作式超时，单位毫秒；null 或 0 表示不限时，最大值为 9223372036854。 */
    private Long timeoutMs;

    /** 是否启用 Huber 基线鲁棒平差，默认关闭；启用时使用 Go 默认阈值和迭代参数。 */
    private boolean robust;
}
