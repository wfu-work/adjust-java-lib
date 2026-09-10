# adjust-java-lib

`adjust-java-lib` 的 Java SDK，通过 JNA 调用稳定 C ABI，使用 UTF-8 JSON 传输站网输入和结果。包含 ENU 平差、WGS84 站心 ENU 平差、先验控制、自由网、鲁棒估计、方差分量估计、稀疏求解和完整质量诊断模型。数值算法与站网业务校验由 Go 库负责。

## 构建和依赖

保持项目原有的 Java 20 编译目标，推荐使用 JDK 21 构建。Maven 3.6.3+：

```bash
mvn clean verify
mvn install
```

产物为普通依赖 JAR：`target/adjust-java-lib-1.0.0.jar`，可被其他项目直接引用，不是 Spring Boot 可执行包。

```xml
<dependency>
    <groupId>com.navfirst</groupId>
    <artifactId>adjust-java-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

普通 Java 程序只需要 SDK 及其传递依赖 JNA、Gson；Spring Boot 支持为可选依赖。

## ENU 平差

所有基线必须使用同一套 ENU 坐标轴，向量单位为米。下面使用两条等权观测，测站 B 的平差结果为 `(11, 3, 1)`：

```java
import com.navfirst.adjust.lib.domains.*;
import com.navfirst.adjust.lib.domains.Geometry.*;
import com.navfirst.adjust.lib.domains.ENUNetworkOptions.*;
import com.navfirst.adjust.lib.services.AdjustLibService;
import com.navfirst.adjust.lib.services.impl.AdjustLibServiceImpl;
import java.util.List;

AdjustLibService sdk = new AdjustLibServiceImpl();
Matrix3 covariance = Matrix3.fromStdDev(1, 1, 1);
ENUNetworkProblem problem = ENUNetworkProblem.builder()
    .name("示例站网")
    .stations(List.of(
        ENUNetworkProblem.Station.builder().id("A").fixed(true).knownENU(new ENU(0, 0, 0)).build(),
        ENUNetworkProblem.Station.builder().id("B").build()))
    .baselines(List.of(
        ENUNetworkProblem.Baseline.builder().id("AB1").from("A").to("B")
            .vector(new ENU(10, 2, 0.5)).covariance(covariance).build(),
        ENUNetworkProblem.Baseline.builder().id("AB2").from("A").to("B")
            .vector(new ENU(12, 4, 1.5)).covariance(covariance).build()))
    .build();

System.out.println(sdk.getVersion());
ENUNetworkResult result = sdk.startAdjust(problem);
result.getStations().forEach(station ->
    System.out.println(station.getId() + ": " + station.getPosition()));
System.out.println(result.getDiagnostics().getSigma0());
```

`domains` 已从 37 个独立文件合并为 8 个。站点、基线、先验等使用所属对象的静态内部类，仍提供构造器、getter/setter 和 `builder()`；使用方不需要添加 Lombok。

| 文件 | 包含内容 |
| --- | --- |
| `AdjustRequest` | 请求信封和超时 |
| `ENUNetworkOptions` | 公用选项，内部 `Solver`、`Robust`、`VarianceComponents` 及枚举 |
| `ENUNetworkProblem` | ENU 输入，内部 `Station`、`Baseline`、`Prior` |
| `GeodeticNetworkProblem` | WGS84 输入，内部 `Station`、`Baseline`、`Prior` |
| `ENUNetworkResult` | ENU 结果，内部 `Station`、`Baseline`、`Prior` |
| `GeodeticNetworkResult` | WGS84 结果，内部 `Station`、`Baseline`、`Prior` |
| `Geometry` | `ENU`、`WGS84Coordinate`、`ECEFCoordinate`、`Matrix3`、`Matrix` |
| `NetworkDiagnostics` | 公共诊断，内部 `GlobalTest`、`VarianceComponent` |

原来使用 `Station.builder()` 的代码改为 `ENUNetworkProblem.Station.builder()`，`ENUBaseline` 改为 `ENUNetworkProblem.Baseline`；几何类型改从 `domains.Geometry` 导入，选项枚举改从 `domains.ENUNetworkOptions` 导入。JSON 字段及顶层服务方法保持一致。

`Matrix3` 用 9 个行优先元素表示 3×3 协方差，`diagonal` 接收方差，`fromStdDev` 接收标准差。协方差单位为平方米。默认要求有限、对称、正定的协方差；省略协方差时可显式选择 `CovariancePolicy.UNIT`，不要用零矩阵表示等权。

## WGS84 平差

WGS84 角度单位为度，椭球高单位为米。基线向量采用该基线 `frameWGS84` 的站心 ENU 坐标系；未指定时由 Go 使用起点的近似坐标。`exactWGS84` 表示精确控制，`approximateWGS84` 仅提供近似位置/坐标系。无近似坐标的测站也可以参与求解。

```java
WGS84Coordinate origin = new WGS84Coordinate(30.5, 114.3, 50);
GeodeticNetworkProblem problem = GeodeticNetworkProblem.builder()
    .stations(List.of(
        GeodeticNetworkProblem.Station.builder().id("A").exactWGS84(origin)
            .approximateWGS84(origin).build(),
        GeodeticNetworkProblem.Station.builder().id("B").build()))
    .baselines(List.of(
        GeodeticNetworkProblem.Baseline.builder().id("AB").from("A").to("B")
            .frameWGS84(origin).vectorENU(new ENU(10, 2, 0.5))
            .covarianceENU(Matrix3.fromStdDev(0.01, 0.01, 0.02)).build()))
    .build();

GeodeticNetworkResult result = sdk.startAdjustWgs84(problem);
result.getStations().forEach(station -> {
    System.out.println(station.getPositionWGS84());
    System.out.println(station.getPositionECEF());
});
```

WGS84 结果的 `covarianceXYZ`、`formalCovarianceXYZ`、`stdDevXYZ` 在全局 ECEF XYZ 坐标系中；基线残差仍在原基线的站心 ENU 坐标系中。是否为绝对 WGS84 解由 `isWgs84Absolute()` 标识。

## 解算选项和超时

两种站网共用 `ENUNetworkOptions`。不传选项时采用 Go 默认行为：稠密求解、完整协方差、外部基准、必需观测协方差。可通过完整请求设置毫秒超时：

```java
AdjustRequest request = AdjustRequest.builder()
    .problem(problem) // WGS84 输入改用 .geodeticProblem(problem)
    .options(ENUNetworkOptions.builder()
        .solver(Solver.builder().method(SolverMethod.AUTO).build())
        .covariance(CovarianceMode.STATION_BLOCKS)
        .build())
    .timeoutMs(5000L)
    .build();
ENUNetworkResult result = sdk.startAdjust(request);
```

`problem` 和 `geodeticProblem` 必须且只能设置一个。强类型 `startAdjust` 接受 ENU；强类型 `startAdjustWgs84` 接受 WGS84。`timeoutMs` 为 null 或 0 表示不限时，最大值为 `9223372036854`。超时由 Go 协作式执行，不能异步强制打断稠密矩阵分解；Java 线程中断不会自动取消 Go 调用。

| 需求 | 配置/模型 |
| --- | --- |
| 完整、站块或不计算协方差 | `CovarianceMode.FULL / STATION_BLOCKS / NONE` |
| 等权平差，无需输入观测协方差 | `covariancePolicy(UNIT)`、`unitVariance(...)` |
| 自动/稠密/稀疏求解 | `SolverMethod.AUTO / DENSE / PCG` |
| PCG 预条件器 | `JACOBI / BLOCK_JACOBI / IC0` |
| 无绝对控制的零质心自由网 | `datum(DatumMode.FREE_CENTROID)` |
| 随机控制约束 | `ENUNetworkProblem.Prior` / `GeodeticNetworkProblem.Prior` |
| 基线整体 Huber 降权 | `robust(Robust.builder().method(HUBER).build())` |
| 基线组方差分量估计 | `varianceComponents(new VarianceComponents())`，基线设置 `group` |

完整协方差的参数顺序由 `parameterKeys` 给出。精简协方差模式下，应检查 `diagnostics` 中的 `fullCovarianceAvailable`、`stationCovarianceAvailable` 和 `residualDiagnosticsAvailable`；零值或空矩阵不代表精度为零。残差定义为“观测值 − 平差值”，告警在 `warnings` 中。

## 原始 JSON 与错误处理

`startAdjust(String)`、`startAdjustWgs84(String)` 直接返回 Go 的完整 JSON 信封：

```json
{"ok":true,"result":{},"error":null}
```

原始 `startAdjust` 接受直接 ENU 问题或 `problem` 信封；处理 WGS84 时使用 `geodetic_problem` 信封。原始 `startAdjustWgs84` 也接受直接 WGS84 问题。字符串入口原样返回响应，包括 `ok=false` 的错误信封；强类型入口由 `JsonUtils.decode(String, Class)` 解析 `result`，遇到错误信封时抛出 `AdjustException`。

```java
try {
    sdk.startAdjust(request);
} catch (com.navfirst.adjust.lib.exceptions.AdjustException e) {
    System.err.printf("code=%s field=%s id=%s message=%s%n",
        e.getCode(), e.getField(), e.getId(), e.getMessage());
}
```

`getCode()` 为字符串，与 Go 协议一致，例如 `invalid_covariance`、`unknown_station`、`disconnected_network`、`not_converged`、`deadline_exceeded`。SDK 边界错误包括 `invalid_request`、`request_too_large`、`invalid_response`、`native_load_error`，加载库失败时底层异常保存在 `getCause()`。原生调用中的 JNA 链接错误直接向调用方抛出。请求上限 64 MiB UTF-8 字节；禁止原始 NUL 字符，SDK 不会将它静默截断。

## Spring Boot 3

在已有 Spring Boot 3 应用中添加 SDK 依赖，即可注入 `AdjustLibService`；由自动配置统一注册，不需要额外扫描 SDK 包。自定义同类型 Bean 会覆盖默认实现。

```yaml
adjust:
  enabled: true
  library-path: Adjust # 默认库名；也可指定动态库文件的绝对路径
```

创建默认服务时按 `adjust.library-path` 加载动态库，路径无效会导致服务创建失败。`adjust.enabled=false` 禁用默认服务注册；提供自定义服务 Bean 时也不会创建默认服务。

## 动态库与跨平台

JAR 内已有以下资源，JNA 根据当前平台自动选择：

| 平台 | 资源 |
| --- | --- |
| macOS ARM64 | `darwin-aarch64/libAdjust.dylib` |
| Linux ARM64 | `linux-aarch64/libAdjust.so` |
| Linux x86-64 | `linux-x86-64/libAdjust.so` |

其他平台需要提供对应架构的动态库及其系统依赖。默认构造方法使用 `AdjustLibrary.INSTANCE`；首次初始化共享实例时读取 JVM 参数 `-Dadjust.library-path=/absolute/path/libAdjust.so`，未设置则使用库名 `Adjust`。无内置库的平台应在 JVM 启动时配置该参数，或使用 `-Djna.library.path=/library/directory`。也可以通过 `new AdjustLibServiceImpl("/absolute/path/libAdjust.so")` 创建指定库的服务实例。

Java 原生接口参考 `dmonitor-java-lib`，直接返回 `String`，由 JNA 按 UTF-8 转换：

```java
String getVersion();
String startAdjust(String requestJson);
String startAdjustWgs84(String requestJson);
```

服务层直接调用动态库并校验业务参数；JSON 序列化、请求传输校验、强类型响应解析和结果转换统一由 `utils.JsonUtils` 处理。服务实例可以并发使用，调用期间不要修改传入对象。

当前 Go 实现使用 `C.CString` 分配返回字符串；JNA 的 `String` 映射只复制内容，不自动释放对应的原生内存。本 SDK 按直接 `String` 映射方式实现。

从 Go 源码重新构建当前平台的动态库（需要该 Go 项目指定的 Go 版本与 C 编译器）：

```bash
./scripts/build-native.sh /path/to/nav-adjust-go-lib
# macOS ARM64 的输出示例；不覆盖 src/main/resources 内置库
mvn -Dadjust.library-path="$PWD/target/native/darwin-arm64/libAdjust.dylib" test
```

`mvn clean` 会删除 `target/native`，使用外部构建库运行测试前不要执行 clean。发布更新时，将经过验证的产物复制到对应 JNA 资源目录再打包。

## 验证范围

测试覆盖真实动态库的 ENU/WGS84 解算、质量字段、先验、自由网、鲁棒降权、稀疏等权解算、并发调用与错误透传，以及字符串返回、输入边界和 Spring 自动配置。测试需在当前平台动态库可用的环境中执行。非内置平台请在 JVM 启动时通过 `adjust.library-path` 指定动态库；加载失败会使相关测试失败。

已删除未使用的 RTK 占位类 `AdjustTask`、`AdjustData`。业务调用使用 `AdjustRequest`、两种站网问题和对应结果类型。
