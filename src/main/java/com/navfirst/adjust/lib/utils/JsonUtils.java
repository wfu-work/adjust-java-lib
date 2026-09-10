package com.navfirst.adjust.lib.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.navfirst.adjust.lib.domains.AdjustOptions;
import com.navfirst.adjust.lib.domains.ENUNetworkProblem;
import com.navfirst.adjust.lib.domains.GeodeticNetworkProblem;
import com.navfirst.adjust.lib.exceptions.AdjustException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建：馥溪凝
 * 日期：2026/9/10 13:04
 * 描述：统一处理 JSON 序列化、请求传输校验和 Go 响应解析。
 */
public final class JsonUtils {
    private static final int MAX_REQUEST_BYTES = 64 * 1024 * 1024;
    private static final long MAX_TIMEOUT_MS = Long.MAX_VALUE / 1_000_000;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtils() {
    }

    /** 将 ENU 输入和核心选项组装成 Go 请求信封。 */
    public static String toRequestJson(ENUNetworkProblem problem, AdjustOptions options) {
        return toRequestJson("problem", problem, options);
    }

    /** 将 WGS84 输入和核心选项组装成 Go 请求信封。 */
    public static String toRequestJson(GeodeticNetworkProblem problem, AdjustOptions options) {
        return toRequestJson("geodetic_problem", problem, options);
    }

    private static String toRequestJson(String field, Object problem, AdjustOptions options) {
        if (problem == null) {
            throw new AdjustException("invalid_request", "站网输入不能为空", field, null);
        }
        Long timeout = options == null ? null : options.getTimeoutMs();
        if (timeout != null && (timeout < 0 || timeout > MAX_TIMEOUT_MS)) {
            throw new AdjustException("invalid_request", "超时毫秒数必须在 0 到 " + MAX_TIMEOUT_MS + " 之间",
                    "timeout_ms", null);
        }

        Map<String, Object> nativeOptions = new LinkedHashMap<>();
        nativeOptions.put("solver", Map.of("method", "dense"));
        nativeOptions.put("datum", "external");
        nativeOptions.put("covariance_policy", "required");
        nativeOptions.put("covariance", "station-blocks");
        if (options != null && options.isRobust()) {
            // 空配置对象启用 Go 默认 Huber 参数；关闭时完全省略 robust。
            nativeOptions.put("robust", Map.of());
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put(field, problem);
        request.put("options", nativeOptions);
        if (timeout != null) {
            request.put("timeout_ms", timeout);
        }
        return toJson(request);
    }

    /** 将对象序列化为 JSON，拒绝 NaN、Infinity 等无法通过协议传输的值。 */
    public static String toJson(Object value) {
        try {
            return GSON.toJson(value);
        } catch (IllegalArgumentException e) {
            throw new AdjustException("invalid_request", "Request contains a non-JSON value: " + e.getMessage(), e);
        }
    }

    /** 校验请求的 UTF-8 字节长度及 C 字符串传输限制，JSON 语法由 Go 校验。 */
    public static void validateInput(String json) {
        if (json == null || json.isBlank()) {
            throw new AdjustException("invalid_request", "Request JSON must not be empty");
        }
        if (json.length() > MAX_REQUEST_BYTES) {
            throw new AdjustException("request_too_large", "Request JSON exceeds 64 MiB");
        }
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\0') {
                throw new AdjustException("invalid_request", "Request JSON must not contain literal NUL characters");
            }
            if (Character.isHighSurrogate(ch)) {
                if (++i >= json.length() || !Character.isLowSurrogate(json.charAt(i))) {
                    throw new AdjustException("invalid_request", "Request JSON contains an unpaired surrogate");
                }
            } else if (Character.isLowSurrogate(ch)) {
                throw new AdjustException("invalid_request", "Request JSON contains an unpaired surrogate");
            }
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_REQUEST_BYTES) {
            throw new AdjustException("request_too_large", "Request JSON exceeds 64 MiB");
        }
    }

    /** 解析 Go 返回的 JSON 信封；业务失败时抛出包含原始错误信息的异常。 */
    public static Response parseResponse(String json) {
        if (json == null) {
            throw invalidResponse("Native adjustment returned null");
        }
        try {
            JsonElement root = readJson(json);
            if (!root.isJsonObject()) {
                throw invalidResponse("Native response must be a JSON object");
            }
            JsonObject envelope = root.getAsJsonObject();
            JsonElement ok = envelope.get("ok");
            if (ok == null || !ok.isJsonPrimitive() || !ok.getAsJsonPrimitive().isBoolean()) {
                throw invalidResponse("Native response is missing boolean ok");
            }
            if (!ok.getAsBoolean()) {
                JsonElement errorValue = envelope.get("error");
                if (errorValue == null || !errorValue.isJsonObject()) {
                    throw invalidResponse("Native failure is missing error");
                }
                JsonObject error = errorValue.getAsJsonObject();
                String code = stringField(error, "code", true);
                String message = stringField(error, "message", true);
                throw new AdjustException(code, message,
                        stringField(error, "field", false), stringField(error, "id", false));
            }
            JsonElement result = envelope.get("result");
            JsonElement error = envelope.get("error");
            if (result == null || !result.isJsonObject() || (error != null && !error.isJsonNull())) {
                throw invalidResponse("Native success is missing result or contains error");
            }
            return new Response(json, result.getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new AdjustException("invalid_response", "Cannot decode native JSON response", e);
        }
    }

    /** 将动态库返回的 JSON 信封解析为结果对象，错误信封转换为 AdjustException。 */
    public static <T> T decode(String json, Class<T> type) {
        return decode(parseResponse(json), type);
    }

    /** 将成功信封中的 result 转换为指定的结果类型。 */
    public static <T> T decode(Response response, Class<T> type) {
        try {
            return GSON.fromJson(response.result(), type);
        } catch (JsonParseException | IllegalStateException e) {
            throw new AdjustException("invalid_response", "Cannot decode " + type.getSimpleName(), e);
        }
    }

    private static JsonElement readJson(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            JsonElement root = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw invalidResponse("Unexpected content after native JSON response");
            }
            return root;
        }
    }

    private static String stringField(JsonObject object, String field, boolean required) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            if (!required) {
                return null;
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && (!required || !value.getAsString().isBlank())) {
            return value.getAsString();
        }
        throw invalidResponse("Invalid error." + field + " in native response");
    }

    private static AdjustException invalidResponse(String message) {
        return new AdjustException("invalid_response", message);
    }

    /** 成功响应的原始 JSON 和 result 对象。 */
    public record Response(String json, JsonObject result) {
    }
}
