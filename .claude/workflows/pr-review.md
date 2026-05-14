# Workflow: PR Review

## 目标

对 Pull Request 或任务 Diff 进行 AI Review，识别 Bug、安全、性能、权限、数据隔离和测试缺口。

## 流程

1. Repository 模块获取 PR Diff。
2. RAG Agent 检索相关代码、规范和历史问题。
3. Review Agent 分析 Diff。
4. Test Agent 补充测试建议。
5. Review Agent 输出问题列表和合并建议。

## Review 重点

- 权限绕过。
- 跨项目数据访问。
- API 契约不兼容。
- 数据库迁移风险。
- 事务范围过大。
- 敏感信息泄露。
- AI Tool Runtime 绕过。
- 缺少测试。

## 输出结论

```text
结论：可合并 / 修复后合并 / 阻塞合并
```

