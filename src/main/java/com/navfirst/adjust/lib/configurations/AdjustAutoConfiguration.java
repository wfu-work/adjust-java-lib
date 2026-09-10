package com.navfirst.adjust.lib.configurations;

import com.navfirst.adjust.lib.services.AdjustLibService;
import com.navfirst.adjust.lib.services.impl.AdjustLibServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(AdjustProperties.class)
@ConditionalOnProperty(prefix = "adjust", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdjustAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AdjustLibService.class)
    public AdjustLibService adjustLibService() {
        return new AdjustLibServiceImpl();
    }

}
