# Milestone 29: 后端测试覆盖与质量门增强 — 验证报告

## 1. 新增/修改文件清单

### 新增文件
- `docs/backend-test-matrix.md` — 后端测试矩阵
- `docs/backend-testing-guide.md` — 后端测试指南
- `docs/backend-coverage-report-template.md` — 覆盖率报告模板
- `docs/milestone-29-validation-report-template.md` — 本验证报告模板
- `backend/src/test/java/.../security/JwtTokenProviderTest.java` — JWT Token Provider 单元测试
- `backend/src/test/java/.../task/TaskStateMachineTest.java` — Task 状态机单元测试
- `backend/src/test/java/.../rag/DocumentChunkServiceTest.java` — 文档分块服务单元测试
- `backend/src/test/java/.../support/TestDataFactory.java` — 测试数据工厂

### 修改的测试文件
| 文件 | 改动说明 |
|------|----------|
| | |

### 修改的脚本文件
| 文件 | 改动说明 |
|------|----------|
| | |

### 更新的文档
| 文件 | 改动说明 |
|------|----------|
| | |

---

## 2. Backend Test Matrix 说明

（引用 docs/backend-test-matrix.md，说明模块-风险-覆盖映射）

---

## 3. 新增测试覆盖说明

（列出所有新增测试类、测试数量和覆盖的关键场景）

---

## 4. Security / JWT 测试说明

（JwtTokenProviderTest + 增强的 AuthIntegrationTest 覆盖的 JWT token type、refresh 越权等场景）

---

## 5. Model Gateway 测试说明

（PromptSafetyServiceTest / ModelSecretMaskingServiceTest / ModelPricingServiceTest / ModelGatewayIntegrationTest 已覆盖和增强的场景）

---

## 6. RAG / Chat / Task 测试说明

（DocumentChunkServiceTest / TaskStateMachineTest / Chat / GitHub 增强的覆盖说明）

---

## 7. Quality Gate 更新说明

（run-backend-checks.sh / release-checklist.sh 的更新内容）

---

## 8. 自动化验证结果

### mvn test
```
Tests run: N, Failures: 0, Errors: 0, Skipped: 0
```

### 文档验证
```
PASS/FAIL: docs/backend-test-matrix.md
PASS/FAIL: docs/backend-testing-guide.md
PASS/FAIL: docs/backend-coverage-report-template.md
PASS/FAIL: docs/milestone-29-validation-report-template.md
```

### 脚本验证
```
PASS/FAIL: bash -n scripts/run-backend-checks.sh
PASS/FAIL: bash -n scripts/release-checklist.sh
```

---

## 9. 已知限制

（记录当前阶段的已知限制）

---

## 10. 是否可以进入 Milestone 30

（基于验证结果判断）
