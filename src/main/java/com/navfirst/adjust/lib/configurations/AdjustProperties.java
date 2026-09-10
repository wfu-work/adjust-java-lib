package com.navfirst.adjust.lib.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SDK 配置。创建默认服务时加载动态库。 */
@Data
@ConfigurationProperties(prefix = "adjust")
public class AdjustProperties {

    /** 是否注册默认 AdjustLibService。 */
    private boolean enabled = true;

}
