# adjust-java-lib

通过 JNA 调用 `nav-adjust-go-lib` 的 Java SDK。核心对象用于固定控制点与基线站网平差，输出坐标、精度、基线残差和必要的质量诊断；随机控制先验、自由网、方差分量估计及完整矩阵通过原始 JSON 接口使用。

数值解算和站网业务校验由 Go 库负责。Java 原生映射直接返回 `String`，JSON 组装、传输校验和结果解析集中在 `utils.JsonUtils`。

## 安装与构建

Java 编译目标为 20，推荐使用 JDK 21 和 Maven 3.6.3+：

```bash
mvn clean verify
mvn install
```

产物为普通依赖 JAR：`target/adjust-java-lib-2.0.0.jar`。

```xml
<dependency>
    <groupId>com.navfirst</groupId>
    <artifactId>adjust-java-lib</artifactId>
    <version>2.0.0</version>
</dependency>
```

普通 Java 程序依赖 JNA 和 Gson；Spring Boot 支持为可选依赖。使用方无需安装 Lombok。`getVersion()` 返回 Go 动态库的 ABI 版本，与 Java SDK 版本分别管理。

## 核心模型

`domains` 保留 7 个文件。站点、基线、坐标和检验摘要继续使用静态内部类。

| 文件 | 核心内容 |
| --- | --- |
| `AdjustOptions` | `timeoutMs`、`robust` |
| `ENUNetworkProblem` | `stations`、`baselines`；ENU 控制坐标、基线向量和观测协方差 |
| `GeodeticNetworkProblem` | `stations`、`baselines`；WGS84 控制坐标、站心 ENU 基线及坐标系原点 |
| `ENUNetworkResult` | 站点 ENU 坐标和标准差、基线结果、诊断与告警 |
| `GeodeticNetworkResult` | 站点 WGS84/ECEF 坐标和 XYZ 精度、基线结果、诊断与告警 |
| `Geometry` | `ENU`、`WGS84Coordinate`、`ECEFCoordinate`、`Matrix3` |
| `NetworkDiagnostics` | 自由度、单位权标准差、收敛状态、精度可用性和整体检验摘要 |

名称、元数据由业务系统根据 `id` 关联。核心输入不提供随机坐标先验、分组、自由网或数值求解器配置。每个独立连通分量都需要固定控制点。

## ENU 平差

所有站点和基线使用同一套 ENU 坐标轴，向量单位为米。固定站设置 `fixed=true` 和 `knownENU`，自由站只需设置 ID。

```java
import com.navfirst.adjust.lib.domains.*;
import com.navfirst.adjust.lib.domains.Geometry.*;
import com.navfirst.adjust.lib.services.AdjustLibService;
import com.navfirst.adjust.lib.services.impl.AdjustLibServiceImpl;
import java.util.List;

AdjustLibService sdk = new AdjustLibServiceImpl();
ENUNetworkProblem problem = ENUNetworkProblem.builder()
    .stations(List.of(
        ENUNetworkProblem.Station.builder().id("A").fixed(true)
            .knownENU(new ENU(0, 0, 0)).build(),
        ENUNetworkProblem.Station.builder().id("B").build()))
    .baselines(List.of(
        ENUNetworkProblem.Baseline.builder().id("AB1").from("A").to("B")
            .vector(new ENU(10, 2, 0.5))
            .covariance(Matrix3.fromStdDev(1, 1, 1)).build(),
        ENUNetworkProblem.Baseline.builder().id("AB2").from("A").to("B")
            .vector(new ENU(12, 4, 1.5))
            .covariance(Matrix3.fromStdDev(2, 2, 2)).build()))
    .build();

ENUNetworkResult result = sdk.startAdjust(problem);
result.getStations().forEach(station ->
    System.out.println(station.getId() + ": " + station.getPosition()));
// B 的加权平差坐标为 (10.4, 2.4, 0.7)。
```

观测协方差必填，要求有限、对称且正定。`Matrix3` 存放 9 个行优先元素，单位为平方米；`diagonal` 接收方差，`fromStdDev` 接收标准差。三个标准差均应大于零。相关观测可以通过 `new Matrix3(double[9])` 提供完整的 3×3 协方差。核心入口不会把缺失协方差自动替换成等权。

## WGS84 平差

经纬度单位为度，椭球高单位为米。站点的 `exactWGS84` 表示精确控制坐标；自由站只需 ID。每条基线必须提供 `frameWGS84`，明确该观测向量的站心 ENU 坐标轴。控制坐标与基线原点分别设置，不再使用 `approximateWGS84` 回退。

```java
WGS84Coordinate origin = new WGS84Coordinate(30.5, 114.3, 50);
GeodeticNetworkProblem wgs84Problem = GeodeticNetworkProblem.builder()
    .stations(List.of(
        GeodeticNetworkProblem.Station.builder().id("A").exactWGS84(origin).build(),
        GeodeticNetworkProblem.Station.builder().id("B").build()))
    .baselines(List.of(
        GeodeticNetworkProblem.Baseline.builder().id("AB1").from("A").to("B")
            .frameWGS84(origin).vectorENU(new ENU(10, 2, 0.5))
            .covarianceENU(Matrix3.fromStdDev(0.01, 0.01, 0.02)).build(),
        GeodeticNetworkProblem.Baseline.builder().id("AB2").from("A").to("B")
            .frameWGS84(origin).vectorENU(new ENU(10.01, 2.01, 0.52))
            .covarianceENU(Matrix3.fromStdDev(0.01, 0.01, 0.02)).build()))
    .build();

GeodeticNetworkResult wgs84Result = sdk.startAdjustWgs84(wgs84Problem);
wgs84Result.getStations().forEach(station -> {
    System.out.println(station.getPositionWGS84());
    System.out.println(station.getPositionECEF());
    if (wgs84Result.getDiagnostics().isStationCovarianceAvailable()) {
        System.out.println(station.getStdDevXYZ());
        System.out.println(station.getMXYZ());
    }
});
```

`stdDevXYZ` 是全局 ECEF X、Y、Z 三方向标准差，单位米；`mXYZ` 是三维点位中误差，等于 `sqrt(σX² + σY² + σZ²)`。`wgs84Absolute` 标识该站是否锚定到绝对 WGS84 基准。基线的 `adjustedENU`、`residualENU`、`standardized` 始终使用输入基线的站心 ENU 坐标轴。

## 超时与鲁棒平差

两种入口共用 `AdjustOptions`：

```java
AdjustOptions options = AdjustOptions.builder()
    .timeoutMs(5000L)
    .robust(true)
    .build();

ENUNetworkResult result = sdk.startAdjust(problem, options);
GeodeticNetworkResult wgs84Result = sdk.startAdjustWgs84(wgs84Problem, options);
```

不传选项或传入 `null` 均表示不设超时、关闭鲁棒平差。`robust=true` 启用 Go 默认 Huber 参数；`JsonUtils` 将该开关转换为 `options.robust={}`。

`timeoutMs` 位于 Go 请求信封顶层，合法范围为 `0` 到 `9223372036854`，`null` 或 `0` 表示不限时。超时由 Go 协作式执行，不能强制中断正在进行的稠密矩阵分解；Java 线程中断不会自动取消 Go 调用。

核心入口固定采用稠密求解、外部控制基准、输入观测协方差和 `station-blocks` 协方差模式。该模式计算站点精度及残差诊断，省去完整参数协方差矩阵的构造和输出。协议中的其他返回字段由 Gson 忽略。

## 使用结果与质量信息

两种结果顶层都只有 `stations`、`baselines`、`diagnostics`、`warnings`。

| 结果 | 核心字段 |
| --- | --- |
| ENU 站点 | `id`、`position`、`stdDev` |
| WGS84 站点 | `id`、`positionWGS84`、`positionECEF`、`stdDevXYZ`、`mXYZ`、`wgs84Absolute` |
| ENU 基线 | `id`、`from`、`to`、`adjusted`、`residual`、`standardized`、`weight` |
| WGS84 基线 | `id`、`from`、`to`、`adjustedENU`、`residualENU`、`standardized`、`weight` |

残差为“观测值 − 平差值”，单位米；标准化残差无量纲。`weight < 1` 表示整条基线被鲁棒降权。

`NetworkDiagnostics` 保留 `sigma0`、`degreesOfFreedom`、`converged`、`stationCovarianceAvailable`、`residualDiagnosticsAvailable`、`globalTest`：

- 读取站点精度前检查 `stationCovarianceAvailable`；读取标准化残差前检查 `residualDiagnosticsAvailable`。不可用时的零值不代表高精度。
- `globalTest` 仅包含 `passed`、`confidence`。没有正自由度、发生鲁棒降权或估计方差分量时为空，表示本次未执行整体检验。
- `converged=false` 或非空 `warnings` 需要业务方检查；`globalTest.passed=false` 表示未通过整体随机模型检验。

## 高级 JSON 接口与错误处理

原生映射仍为三个方法：

```java
String getVersion();
String startAdjust(String requestJson);
String startAdjustWgs84(String requestJson);
```

服务的字符串入口原样传递请求，不注入核心默认配置，并原样返回 Go 的完整 `ok/result/error` 信封。可使用完整协方差、稀疏求解、自由网、随机控制先验、等权策略和方差分量估计等 Go 协议能力。

例如，用核心输入请求完整协方差：

```java
import com.navfirst.adjust.lib.utils.JsonUtils;
import java.util.Map;

String requestJson = JsonUtils.toJson(Map.of(
    "problem", problem,
    "options", Map.of("covariance", "full")));
String responseJson = sdk.startAdjust(requestJson);
// 保留完整响应供业务解析；不会裁剪协方差、先验等高级字段。
```

WGS84 使用 `geodetic_problem` 信封或直接问题对象。原始 WGS84 JSON 仍支持 Go 协议的 `approximate_wgs84` 和基线坐标系回退。

字符串入口保留 `ok=false` 错误信封；强类型入口将错误转换为 `AdjustException`：

```java
try {
    sdk.startAdjust(problem);
} catch (com.navfirst.adjust.lib.exceptions.AdjustException e) {
    System.err.printf("code=%s field=%s id=%s message=%s%n",
        e.getCode(), e.getField(), e.getId(), e.getMessage());
}
```

Go 错误代码及相关字段原样保留。请求上限为 64 MiB UTF-8 字节，拒绝非法代理字符和原始 NUL 字符；非有限数字也不能通过对象序列化发送。JNA 动态库加载或链接错误直接向调用方抛出。

## 从 1.x 迁移

2.0.0 精简了公开 Java 模型，删除的 getter、builder 字段及类型属于不兼容变更。Go JSON 协议和字符串入口保持兼容。

| 原调用方式 | 2.0.0 调整 |
| --- | --- |
| `startAdjust(AdjustRequest)` | `startAdjust(problem)` 或 `startAdjust(problem, options)` |
| `startAdjustWgs84(AdjustRequest)` | `startAdjustWgs84(problem)` 或 `startAdjustWgs84(problem, options)` |
| `ENUNetworkOptions`、嵌套求解器和枚举 | `AdjustOptions`，只提供 `timeoutMs` 与 `robust` |
| `approximateWGS84` 推导基线坐标系 | 每条核心 WGS84 基线明确设置 `frameWGS84` |
| `Prior`、`VarianceComponent`、`Geometry.Matrix`、完整协方差等 | 通过完整 JSON 协议使用和解析 |
| `name`、`metadata` | 业务系统按 ID 关联，或通过原始 JSON 传递 |
| `downweighted` | 读取 `weight < 1` |

已有随机控制先验业务应迁移到 JSON 入口，不能将随机先验改成精确固定站，否则会改变解算约束。核心结果不会提供误差传播所需的完整相关协方差；这类应用应继续读取完整 JSON 结果。

## Spring Boot 3 与动态库

普通 Spring Boot 应用可通过自动配置注入 `AdjustLibService`。配置 `adjust.enabled=false` 可关闭自动配置注册；也可以提供自定义服务 Bean。

JAR 包含以下动态库资源，JNA 根据当前平台选择：

| 平台 | 资源 |
| --- | --- |
| macOS ARM64 | `darwin-aarch64/libAdjust.dylib` |
| Linux ARM64 | `linux-aarch64/libAdjust.so` |
| Linux x86-64 | `linux-x86-64/libAdjust.so` |

当前实现通过 `AdjustLibrary.INSTANCE` 加载库名 `Adjust`。需要使用外部动态库时，在 JVM 启动时设置 `-Djna.library.path=/动态库所在目录`。Java 服务调用同步执行，调用期间不要修改传入对象。

当前 Go 实现通过 `C.CString` 分配返回值，JNA 的 `String` 映射不会释放对应的原生分配。

从 Go 源码构建当前平台动态库：

```bash
./scripts/build-native.sh /path/to/nav-adjust-go-lib
# 例如 macOS ARM64，测试刚生成的库：
mvn -Djna.library.path="$PWD/target/native/darwin-arm64" test
```

生成目录为 `target/native/<GOOS>-<GOARCH>`，脚本不会覆盖内置资源。使用该目录测试前不要运行 `mvn clean`。

## 验证范围

测试通过真实动态库对比核心入口与完整协方差入口的 ENU/WGS84 坐标、精度、残差和质量摘要，并验证非等权解算、鲁棒降权、错误透传、WGS84 基线原点要求及高级 JSON 能力。另有请求选项映射、超时边界、输入检查和 Spring 注入测试。

真实动态库测试需要当前平台存在可用的 `Adjust` 库；上述验证不代表已在所有打包平台运行测试。
