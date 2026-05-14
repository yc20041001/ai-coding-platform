# Workflow: AI Coding Task

## 目标

将平台内的 AI Coding 任务从创建、执行、产物生成到审查形成闭环。

## 流程

1. 用户创建任务，指定项目、Agent、优先级和需求描述。
2. Product Agent 澄清任务目标和验收标准。
3. Architect Agent 判断影响模块和实现方案。
4. Backend Agent 或 Frontend Agent 生成代码变更。
5. Tool Runtime 应用 Patch 并运行测试。
6. Test Agent 分析测试结果。
7. Review Agent 审查 Diff。
8. 用户审批后执行 Commit、Push、PR。

## 状态流转

```text
PENDING -> RUNNING -> REVIEWING -> COMPLETED
PENDING -> RUNNING -> FAILED -> PENDING
PENDING -> CANCELED
RUNNING -> CANCELED
```

## 关键约束

- AI 任务必须记录执行日志。
- 代码变更必须以 Patch 或 Diff 形式展示。
- Git 写操作必须审批。
- 失败任务必须保存失败原因和重试建议。

