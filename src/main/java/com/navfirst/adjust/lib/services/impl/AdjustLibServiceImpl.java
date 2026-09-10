package com.navfirst.adjust.lib.services.impl;

import com.navfirst.adjust.lib.domains.AdjustOptions;
import com.navfirst.adjust.lib.domains.ENUNetworkProblem;
import com.navfirst.adjust.lib.domains.ENUNetworkResult;
import com.navfirst.adjust.lib.domains.GeodeticNetworkProblem;
import com.navfirst.adjust.lib.domains.GeodeticNetworkResult;
import com.navfirst.adjust.lib.library.AdjustLibrary;
import com.navfirst.adjust.lib.services.AdjustLibService;
import com.navfirst.adjust.lib.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 创建：馥溪凝
 * 日期：2026/9/10 13:24
 * 描述：调用动态库并返回核心平差结果，协议组装和解析统一交给 JsonUtils。
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdjustLibServiceImpl implements AdjustLibService {

    private final AdjustLibrary adjustLibrary = AdjustLibrary.INSTANCE;

    @Override
    public String getVersion() {
        return this.adjustLibrary.getVersion();
    }

    @Override
    public String startAdjust(String requestJson) {
        JsonUtils.validateInput(requestJson);
        return this.adjustLibrary.startAdjust(requestJson);
    }

    @Override
    public String startAdjustWgs84(String requestJson) {
        JsonUtils.validateInput(requestJson);
        return this.adjustLibrary.startAdjustWgs84(requestJson);
    }

    @Override
    public ENUNetworkResult startAdjust(ENUNetworkProblem problem, AdjustOptions options) {
        String json = JsonUtils.toRequestJson(problem, options);
        return JsonUtils.decode(startAdjust(json), ENUNetworkResult.class);
    }

    @Override
    public GeodeticNetworkResult startAdjustWgs84(GeodeticNetworkProblem problem, AdjustOptions options) {
        String json = JsonUtils.toRequestJson(problem, options);
        return JsonUtils.decode(startAdjustWgs84(json), GeodeticNetworkResult.class);
    }

}
