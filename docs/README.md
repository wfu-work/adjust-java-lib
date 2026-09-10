# Adjust Java SDK 文档

基线：Git `bf85d95b4555a5ba23723160075bfac2e011fb3c`，Java SDK 1.0.0，macOS C ABI 1.0.0。沿用相邻 `dmonitor-java-lib/docs` 对应文档样式。

| 文档 | Word | PDF |
| --- | --- | --- |
| SDK 开发指南 | [开发指南](Adjust-Java-SDK开发指南.docx) | [开发指南 PDF](Adjust-Java-SDK开发指南.pdf) |
| SDK 测试报告 | [测试报告](Adjust-Java-SDK测试报告.docx) | [测试报告 PDF](Adjust-Java-SDK测试报告.pdf) |

另附 [Markdown 测试报告](Adjust-Java-SDK测试报告.md)、[本轮测试数据与证据](test-evidence/2026-09-10-generated) 和 [复现说明](../scripts/report-tests/README.md)。此前证据原样保留在 `test-evidence/2026-09-10`。

本轮测试全部通过：3 项现有回归及 34 组补充验证无失败、错误或跳过。正常数值样本及三档规模的整体检验通过；默认 Huber 收敛、粗差降权，三轴偏差均约 2.887 mm，满足合成样本的 5 mm 门槛。异常用例按预期返回错误或诊断。

结论对应本轮确定性合成数据、macOS arm64 环境和有界调用，不表示现场精度或长期容量验收。具体数值、断言和运行范围见报告。
