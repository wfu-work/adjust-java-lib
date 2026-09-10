# Adjust SDK 报告验证

这些脚本用于重现本轮文档中的确定性合成算例与边界实验，不修改 SDK 源码或内置库。

要求：有可用的 `libAdjust`、JDK 20+、Python 3 和 NumPy。Maven 通过项目 Wrapper 获取。2026-09-10 测量使用 JDK 21.0.9、Wrapper 3.9.16、macOS arm64。

```bash
python3 -m pip install numpy
python3 scripts/report-tests/run_checks.py \
  --source "$PWD" \
  --output /tmp/adjust-report-new-run
```

输出目录必须为空。脚本复制源码到输出目录后执行 `clean verify`，构造依赖 classpath，从普通 JAR 编译并运行 `AdjustReportProbe`。34 组探针含显式断言；失败时返回非零退出码，保留 JSON 和日志。本轮结果位于 `docs/test-evidence/2026-09-10-generated`，全部通过；先前结果保留在 `docs/test-evidence/2026-09-10`。复测应使用新目录。

| 文件 | 用途 |
| --- | --- |
| `make_fixture.py` | 用独立 NumPy GLS 生成 4 站 7 基线相关协方差闭环及参考答案 |
| `AdjustReportProbe.java` | 数值、WGS84、鲁棒、协议/参数/字符边界、矩阵、Spring、重复、并发、规模性能 |
| `run_checks.py` | 隔离源码、构建、编译、执行并保留结果 |

数值容差：加权双站 1e-11 m；闭环坐标/标准差/残差 1e-9 m；WGS84 独立 ECEF 对照 1e-6 m；规模样本全部自由站坐标 1e-8 m。正常样本还断言收敛、精度和残差诊断可用、整体检验通过（置信度 0.95）、无额外告警。它们检查合成参考一致性，不能表示现场精度等级。

闭环扰动由固定整数序列生成，三轴尺度分别为 0.008、0.008、0.012 m；完整公式、输入协方差和独立 NumPy 答案保存在 `correlated-fixture.json`。规模样本每个自由站的两条观测取参考坐标加减 `(0.02,0.02,0.03)/sqrt(2)` m，协方差由标准差 `(0.02,0.02,0.03)` m 构造；解析预期 `sigma0=1`，每次调用均断言其误差不超过 1e-9。

性能从强类型 `startAdjust` 调用前计时到同步返回，包含 JSON/JNA/原生/Gson；不含网络构造、断言和文件写入。每个规模 18 次，前 3 次预热、后 15 次计量。最近秩 P95 是本轮最大值。4 线程 80 次并发只有一个批次，不用于吞吐承诺。

鲁棒数据以 `(10,2,0.5)` m 为目标，10 条观测交替加减 `(0.01,-0.01,0.01)` m，再加入 `(10.2,2.2,0.7)` m 粗差，所有观测标准差均为 0.02 m。普通解须检出整体模型异常；默认 Huber 须收敛、正常观测权值为 1、粗差权值在 `(0.05,0.25)`、每轴偏差小于 0.005 m 且至少减半。完整输入及两种结果保存于 `probe-results.json` 的 `robust` 字段。

Huber 降权后按协议省略整体检验；探针断言 `globalTest=null` 且仅有相应说明，不允许出现迭代上限告警。本轮粗差权值约 0.146454，每轴偏差约 0.002887 m，所有断言通过。64 MiB 样本只送入 Java 传输检查；超时验证仅覆盖参数上下界。

本轮另行验证了无 Spring/Lombok 的最小外部消费者和开发指南两个完整示例的编译，日志在证据目录。脚本中的核心功能回归直接从普通 SDK JAR 运行；`run_checks.py` 不重新生成 Word/PDF 或那些独立示例。

当前 Java 未释放原生 `C.CString` 返回值。这些实验保持有界调用次数；长期压力、峰值 RSS 与正式容量验收应在返回内存所有权修复后进行。
