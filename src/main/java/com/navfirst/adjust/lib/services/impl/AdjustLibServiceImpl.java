package com.navfirst.adjust.lib.services.impl;

import com.navfirst.adjust.lib.domains.AdjustRequest;
import com.navfirst.adjust.lib.domains.ENUNetworkResult;
import com.navfirst.adjust.lib.domains.GeodeticNetworkResult;
import com.navfirst.adjust.lib.exceptions.AdjustException;
import com.navfirst.adjust.lib.library.AdjustLibrary;
import com.navfirst.adjust.lib.services.AdjustLibService;
import com.navfirst.adjust.lib.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 创建：馥溪凝
 * 日期：2026/9/10 13:24
 * 描述：AdjustLibServiceImpl
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdjustLibServiceImpl implements AdjustLibService {

    private static final long MAX_TIMEOUT_MS = Long.MAX_VALUE / 1_000_000;

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
    public ENUNetworkResult startAdjust(AdjustRequest request) {
        validateRequest(request);
        if (request.getProblem() == null) {
            throw new AdjustException("invalid_request", "ENU adjustment requires problem", "problem", null);
        }
        String json = JsonUtils.toJson(request);
        return JsonUtils.decode(startAdjust(json), ENUNetworkResult.class);
    }

    @Override
    public GeodeticNetworkResult startAdjustWgs84(AdjustRequest request) {
        validateRequest(request);
        if (request.getGeodeticProblem() == null) {
            throw new AdjustException("invalid_request", "WGS84 adjustment requires geodetic_problem",
                    "geodetic_problem", null);
        }
        String json = JsonUtils.toJson(request);
        return JsonUtils.decode(startAdjustWgs84(json), GeodeticNetworkResult.class);
    }

    private void validateRequest(AdjustRequest request) {
        if (request == null || (request.getProblem() == null) == (request.getGeodeticProblem() == null)) {
            throw new AdjustException("invalid_request", "Exactly one of problem and geodetic_problem is required",
                    "problem", null);
        }
        Long timeout = request.getTimeoutMs();
        if (timeout != null && (timeout < 0 || timeout > MAX_TIMEOUT_MS)) {
            throw new AdjustException("invalid_request", "timeout_ms must be between 0 and " + MAX_TIMEOUT_MS,
                    "timeout_ms", null);
        }
    }
}
