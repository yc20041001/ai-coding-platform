# Milestone 7: RAG Knowledge Base 基础模块实施文档

## 1. 背景与目标

当前项目已经完成 P0 后端主链路：

- Foundation 基础设施
- Auth 登录认证与 JWT
- Project + Member 项目与成员权限
- Repository 仓库绑定与只读 Git 操作
- Task + Agent 任务与 Agent 基础管理
- Chat + SSE 会话、消息与 Mock 流式输出
- Agent Orchestrator + Mock Model Gateway 编排执行闭环

Milestone 7 的目标是补齐 AI Coding 平台的 RAG 基础能力：

> 实现项目级知识库、文档上传元数据、文档切片、Mock Embedding、Mock 检索和 Chat/Agent 引用能力。

当前阶段不接真实向量数据库，不接真实 Embedding 模型，不做复杂文件解析，只完成 RAG 基础闭环。

## 2. 实施边界

### 2.1 本阶段要做

- 项目级 Knowledge Base 管理
- 默认知识库创建能力
- 知识库启用/禁用
- 知识库统计
- 文档上传元数据
- 文档内容保存
- Markdown / Text / Code 文档入库
- 文档内容切片
- Mock Embedding
- MySQL LIKE 简单检索
- Mock score 排序
- RAG Search API
- 检索结果可映射为 Chat / Agent references

### 2.2 本阶段不做

- 不接真实 Embedding 模型
- 不接 Milvus / Pinecone / Elasticsearch 等外部检索服务
- 不做 PDF / Word 深度解析
- 不接真实 OCR
- 不做复杂代码语义解析
- 不做真实向量相似度计算
- 不强行改造 Chat sendMessage 流程
- 不强行改造 Agent Orchestrator 执行流程
- 不改动已验证通过模块的核心逻辑

## 3. 约束要求

- 遵循现有项目规范：
  - Spring Boot 3.x
  - MyBatis-Plus
  - 无 Lombok
  - 构造器注入
  - 手写 getter/setter
  - ApiResponse
  - BizException
  - ErrorCode
- IDs 对外仍保持 String
- 权限校验复用 `ProjectPermissionService`
- 不破坏 `ChatMessageReference` 结构
- RAG 检索结果应能转换为 Chat / Agent reference
- 不接真实大模型或真实 Embedding
- Mock Embedding 使用稳定可重复结果

## 4. 模块目标

实现 4 个基础能力。

### 4.1 Knowledge Base

- 项目级知识库管理
- 默认知识库创建
- 知识库启用/禁用
- 知识库统计

### 4.2 Knowledge Document

- 上传文档元数据
- 保存文档内容
- 支持 Markdown / Text / Code
- 记录解析状态

### 4.3 Document Chunk

- 文档内容切片
- Mock Embedding
- Chunk 元数据存储
- 简单关键词检索

### 4.4 RAG Retrieval

- 项目内知识检索
- 返回引用结果
- 为 Chat / Agent 提供 references 数据结构

## 5. 新增目录结构

```text
backend/src/main/java/com/aicoding/platform/
└── rag/
    ├── controller/
    │   ├── KnowledgeBaseController.java
    │   ├── KnowledgeDocumentController.java
    │   └── RagSearchController.java
    ├── application/
    │   ├── KnowledgeBaseApplicationService.java
    │   ├── KnowledgeDocumentApplicationService.java
    │   ├── DocumentChunkService.java
    │   └── RagSearchApplicationService.java
    ├── domain/
    │   ├── KnowledgeBaseEntity.java
    │   ├── KnowledgeDocumentEntity.java
    │   ├── DocumentChunkEntity.java
    │   ├── KnowledgeBaseStatus.java
    │   ├── KnowledgeDocumentStatus.java
    │   ├── KnowledgeDocumentSourceType.java
    │   └── KnowledgeDocumentType.java
    ├── dto/
    │   ├── CreateKnowledgeBaseRequest.java
    │   ├── UpdateKnowledgeBaseRequest.java
    │   ├── KnowledgeBaseResponse.java
    │   ├── UploadKnowledgeDocumentRequest.java
    │   ├── KnowledgeDocumentResponse.java
    │   ├── DocumentChunkResponse.java
    │   ├── RagSearchRequest.java
    │   ├── RagSearchResultResponse.java
    │   └── RagSearchResponse.java
    └── infrastructure/
        ├── KnowledgeBaseMapper.java
        ├── KnowledgeDocumentMapper.java
        └── DocumentChunkMapper.java

backend/src/main/resources/db/migration/
└── V8__init_rag_tables.sql
```

## 6. 数据库设计

新增迁移文件：

```text
backend/src/main/resources/db/migration/V8__init_rag_tables.sql
```

新增 3 张表：

- `knowledge_base`
- `knowledge_document`
- `document_chunk`

无物理外键。

### 6.1 knowledge_base

项目知识库表。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PRIMARY KEY | 主键 |
| project_id | BIGINT NOT NULL | 项目 ID |
| name | VARCHAR(128) NOT NULL | 知识库名称 |
| description | VARCHAR(512) NULL | 描述 |
| status | VARCHAR(32) NOT NULL | 状态 |
| embedding_provider | VARCHAR(64) NULL | Embedding Provider |
| embedding_model | VARCHAR(128) NULL | Embedding Model |
| chunk_size | INT NOT NULL DEFAULT 1000 | 切片大小 |
| chunk_overlap | INT NOT NULL DEFAULT 100 | 切片重叠 |
| document_count | BIGINT NOT NULL DEFAULT 0 | 文档数 |
| chunk_count | BIGINT NOT NULL DEFAULT 0 | 切片数 |
| create_time | DATETIME NOT NULL | 创建时间 |
| update_time | DATETIME NOT NULL | 更新时间 |
| deleted | TINYINT NOT NULL DEFAULT 0 | 逻辑删除 |

索引：

```sql
uk_kb_project_name(project_id, name, deleted)
idx_kb_project_status(project_id, status)
idx_kb_create_time(create_time)
```

建表建议：

```sql
CREATE TABLE knowledge_base (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    name VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description VARCHAR(512) NULL COMMENT '描述',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    embedding_provider VARCHAR(64) NULL COMMENT 'Embedding Provider',
    embedding_model VARCHAR(128) NULL COMMENT 'Embedding Model',
    chunk_size INT NOT NULL DEFAULT 1000 COMMENT '切片大小',
    chunk_overlap INT NOT NULL DEFAULT 100 COMMENT '切片重叠',
    document_count BIGINT NOT NULL DEFAULT 0 COMMENT '文档数',
    chunk_count BIGINT NOT NULL DEFAULT 0 COMMENT '切片数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_kb_project_name (project_id, name, deleted),
    KEY idx_kb_project_status (project_id, status),
    KEY idx_kb_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目知识库表';
```

### 6.2 knowledge_document

知识库文档表。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PRIMARY KEY | 主键 |
| project_id | BIGINT NOT NULL | 项目 ID |
| knowledge_base_id | BIGINT NOT NULL | 知识库 ID |
| title | VARCHAR(256) NOT NULL | 文档标题 |
| source_type | VARCHAR(32) NOT NULL | 来源类型 |
| document_type | VARCHAR(32) NOT NULL | 文档类型 |
| file_name | VARCHAR(256) NULL | 文件名 |
| file_path | VARCHAR(1024) NULL | 文件路径 |
| content_hash | VARCHAR(128) NULL | 内容哈希 |
| content | MEDIUMTEXT NULL | 原始内容 |
| status | VARCHAR(32) NOT NULL | 状态 |
| error_message | TEXT NULL | 错误信息 |
| chunk_count | BIGINT NOT NULL DEFAULT 0 | 切片数 |
| token_count | BIGINT NOT NULL DEFAULT 0 | Token 数 |
| create_time | DATETIME NOT NULL | 创建时间 |
| update_time | DATETIME NOT NULL | 更新时间 |
| deleted | TINYINT NOT NULL DEFAULT 0 | 逻辑删除 |

索引：

```sql
idx_doc_project_kb(project_id, knowledge_base_id)
idx_doc_status(status)
idx_doc_type(document_type)
idx_doc_hash(content_hash)
```

建表建议：

```sql
CREATE TABLE knowledge_document (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库 ID',
    title VARCHAR(256) NOT NULL COMMENT '文档标题',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
    document_type VARCHAR(32) NOT NULL COMMENT '文档类型',
    file_name VARCHAR(256) NULL COMMENT '文件名',
    file_path VARCHAR(1024) NULL COMMENT '文件路径',
    content_hash VARCHAR(128) NULL COMMENT '内容哈希',
    content MEDIUMTEXT NULL COMMENT '原始内容',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    error_message TEXT NULL COMMENT '错误信息',
    chunk_count BIGINT NOT NULL DEFAULT 0 COMMENT '切片数',
    token_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Token 数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_doc_project_kb (project_id, knowledge_base_id),
    KEY idx_doc_status (status),
    KEY idx_doc_type (document_type),
    KEY idx_doc_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';
```

### 6.3 document_chunk

文档切片表。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PRIMARY KEY | 主键 |
| project_id | BIGINT NOT NULL | 项目 ID |
| knowledge_base_id | BIGINT NOT NULL | 知识库 ID |
| document_id | BIGINT NOT NULL | 文档 ID |
| chunk_index | INT NOT NULL | 切片序号 |
| content | TEXT NOT NULL | 切片内容 |
| content_hash | VARCHAR(128) NULL | 内容哈希 |
| token_count | BIGINT NOT NULL DEFAULT 0 | Token 数 |
| embedding_mock | TEXT NULL | Mock Embedding |
| metadata | JSON NULL | 元数据 |
| create_time | DATETIME NOT NULL | 创建时间 |

索引：

```sql
idx_chunk_project_kb(project_id, knowledge_base_id)
idx_chunk_document(document_id, chunk_index)
idx_chunk_content_hash(content_hash)
```

建表建议：

```sql
CREATE TABLE document_chunk (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库 ID',
    document_id BIGINT NOT NULL COMMENT '文档 ID',
    chunk_index INT NOT NULL COMMENT '切片序号',
    content TEXT NOT NULL COMMENT '切片内容',
    content_hash VARCHAR(128) NULL COMMENT '内容哈希',
    token_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Token 数',
    embedding_mock TEXT NULL COMMENT 'Mock Embedding',
    metadata JSON NULL COMMENT '元数据',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_chunk_project_kb (project_id, knowledge_base_id),
    KEY idx_chunk_document (document_id, chunk_index),
    KEY idx_chunk_content_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';
```

## 7. Domain 设计

### 7.1 KnowledgeBaseStatus

```java
public enum KnowledgeBaseStatus {
    ACTIVE,
    DISABLED,
    DELETED
}
```

### 7.2 KnowledgeDocumentStatus

```java
public enum KnowledgeDocumentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

### 7.3 KnowledgeDocumentSourceType

```java
public enum KnowledgeDocumentSourceType {
    UPLOAD,
    MANUAL,
    REPOSITORY,
    URL
}
```

### 7.4 KnowledgeDocumentType

```java
public enum KnowledgeDocumentType {
    MARKDOWN,
    TEXT,
    CODE,
    PDF,
    WORD
}
```

当前阶段只真正支持：

- MARKDOWN
- TEXT
- CODE

PDF / WORD 只保留枚举，不实现深度解析。

### 7.5 KnowledgeBaseEntity

对应 `knowledge_base`。

要求：

- `@TableName("knowledge_base")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableLogic` 标注 `deleted`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- `@TableField(fill = FieldFill.INSERT_UPDATE)` 标注 `updateTime`
- 不继承 `BaseEntity`
- 不使用 Lombok
- 手写 getter/setter

### 7.6 KnowledgeDocumentEntity

对应 `knowledge_document`。

要求：

- `@TableName("knowledge_document")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableLogic` 标注 `deleted`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- `@TableField(fill = FieldFill.INSERT_UPDATE)` 标注 `updateTime`
- 不继承 `BaseEntity`
- 不使用 Lombok
- 手写 getter/setter

### 7.7 DocumentChunkEntity

对应 `document_chunk`。

要求：

- `@TableName("document_chunk")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- 不继承 `BaseEntity`
- 不使用 Lombok
- 手写 getter/setter

## 8. Mapper 设计

新增：

```text
rag/infrastructure/KnowledgeBaseMapper.java
rag/infrastructure/KnowledgeDocumentMapper.java
rag/infrastructure/DocumentChunkMapper.java
```

示例：

```java
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
}
```

其他 Mapper 同理。

## 9. DTO 设计

### 9.1 CreateKnowledgeBaseRequest

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 知识库名称 |
| description | String | 否 | 描述 |
| chunkSize | Integer | 否 | 切片大小 |
| chunkOverlap | Integer | 否 | 切片重叠 |

校验：

- `name` 使用 `@NotBlank`

### 9.2 UpdateKnowledgeBaseRequest

字段：

| 字段 | 类型 |
|---|---|
| name | String |
| description | String |
| status | String |
| chunkSize | Integer |
| chunkOverlap | Integer |

### 9.3 KnowledgeBaseResponse

字段：

| 字段 | 类型 |
|---|---|
| id | String |
| projectId | String |
| name | String |
| description | String |
| status | String |
| embeddingProvider | String |
| embeddingModel | String |
| chunkSize | Integer |
| chunkOverlap | Integer |
| documentCount | Long |
| chunkCount | Long |
| createTime | LocalDateTime |
| updateTime | LocalDateTime |

### 9.4 UploadKnowledgeDocumentRequest

字段：

| 字段 | 类型 | 必填 |
|---|---|---|
| knowledgeBaseId | String | 是 |
| title | String | 是 |
| documentType | String | 是 |
| sourceType | String | 否 |
| fileName | String | 否 |
| filePath | String | 否 |
| content | String | 是 |

校验：

- `knowledgeBaseId` 使用 `@NotBlank`
- `title` 使用 `@NotBlank`
- `documentType` 使用 `@NotBlank`
- `content` 使用 `@NotBlank`

### 9.5 KnowledgeDocumentResponse

字段：

| 字段 | 类型 |
|---|---|
| id | String |
| projectId | String |
| knowledgeBaseId | String |
| title | String |
| sourceType | String |
| documentType | String |
| fileName | String |
| filePath | String |
| status | String |
| errorMessage | String |
| chunkCount | Long |
| tokenCount | Long |
| createTime | LocalDateTime |
| updateTime | LocalDateTime |

### 9.6 DocumentChunkResponse

字段：

| 字段 | 类型 |
|---|---|
| id | String |
| projectId | String |
| knowledgeBaseId | String |
| documentId | String |
| chunkIndex | Integer |
| content | String |
| tokenCount | Long |
| metadata | String |
| createTime | LocalDateTime |

### 9.7 RagSearchRequest

字段：

| 字段 | 类型 | 必填 | 默认 |
|---|---|---|---|
| query | String | 是 | - |
| knowledgeBaseId | String | 否 | null |
| limit | Integer | 否 | 10 |
| includeContent | Boolean | 否 | true |

校验：

- `query` 使用 `@NotBlank`

### 9.8 RagSearchResultResponse

字段：

| 字段 | 类型 |
|---|---|
| chunkId | String |
| documentId | String |
| knowledgeBaseId | String |
| title | String |
| content | String |
| score | BigDecimal |
| referenceType | String |
| filePath | String |
| startLine | Integer |
| endLine | Integer |

### 9.9 RagSearchResponse

字段：

| 字段 | 类型 |
|---|---|
| query | String |
| results | List<RagSearchResultResponse> |
| total | Long |
| elapsedMs | Long |

## 10. Application Service 设计

## 10.1 KnowledgeBaseApplicationService

### createKnowledgeBase

```java
KnowledgeBaseResponse createKnowledgeBase(Long projectId, CreateKnowledgeBaseRequest request);
```

权限：

- OWNER 或 MAINTAINER

逻辑：

1. 校验项目权限
2. 校验同项目下 name 不重复
3. 默认 status = `ACTIVE`
4. 默认 embeddingProvider = `MOCK`
5. 默认 embeddingModel = `mock-embedding-v1`
6. chunkSize 默认 1000
7. chunkOverlap 默认 100
8. documentCount = 0
9. chunkCount = 0
10. 保存并返回

### listKnowledgeBases

```java
PageResult<KnowledgeBaseResponse> listKnowledgeBases(Long projectId, PageQuery pageQuery);
```

权限：

- VIEWER+

### getKnowledgeBase

```java
KnowledgeBaseResponse getKnowledgeBase(Long knowledgeBaseId);
```

权限：

- 通过 knowledgeBase.projectId 校验 VIEWER+

### updateKnowledgeBase

```java
KnowledgeBaseResponse updateKnowledgeBase(Long knowledgeBaseId, UpdateKnowledgeBaseRequest request);
```

权限：

- MAINTAINER+

### deleteKnowledgeBase

```java
void deleteKnowledgeBase(Long knowledgeBaseId);
```

权限：

- OWNER

逻辑：

- 逻辑删除知识库

### getOrCreateDefaultKnowledgeBase

```java
KnowledgeBaseEntity getOrCreateDefaultKnowledgeBase(Long projectId);
```

内部方法：

- 如果项目没有默认知识库，则创建 `Default Knowledge Base`

## 10.2 KnowledgeDocumentApplicationService

### uploadDocument

```java
KnowledgeDocumentResponse uploadDocument(Long projectId, UploadKnowledgeDocumentRequest request);
```

权限：

- DEVELOPER+

逻辑：

1. 校验项目权限
2. 校验 knowledgeBase 属于 project
3. 校验 documentType 当前支持 MARKDOWN / TEXT / CODE
4. 创建 document，status = `PROCESSING`
5. 计算 contentHash
6. 调用 `DocumentChunkService` 切片
7. 保存 chunks
8. 更新 document：
   - status = `COMPLETED`
   - chunkCount
   - tokenCount
9. 更新 knowledge_base：
   - documentCount + 1
   - chunkCount + chunkCount
10. 如果失败：
   - document.status = `FAILED`
   - errorMessage 保存异常

### listDocuments

```java
PageResult<KnowledgeDocumentResponse> listDocuments(Long knowledgeBaseId, PageQuery pageQuery);
```

权限：

- VIEWER+

### getDocument

```java
KnowledgeDocumentResponse getDocument(Long documentId);
```

权限：

- VIEWER+

### deleteDocument

```java
void deleteDocument(Long documentId);
```

权限：

- MAINTAINER+

逻辑：

- 逻辑删除 document
- 当前阶段推荐物理删除 chunks
- 更新 knowledge_base 统计

### listChunks

```java
List<DocumentChunkResponse> listChunks(Long documentId);
```

权限：

- VIEWER+

## 10.3 DocumentChunkService

### splitIntoChunks

```java
List<String> splitIntoChunks(String content, int chunkSize, int chunkOverlap);
```

当前阶段规则：

- 简单字符切片
- chunkSize 默认 1000
- chunkOverlap 默认 100
- 防止 overlap >= chunkSize
- 空内容返回空列表

### estimateTokens

```java
long estimateTokens(String content);
```

简化规则：

```java
Math.max(1, content.length() / 3)
```

### mockEmbedding

```java
String mockEmbedding(String content);
```

返回格式：

```text
mock-embedding:{contentHash}:{tokenCount}
```

### hashContent

```java
String hashContent(String content);
```

使用 SHA-256。

## 10.4 RagSearchApplicationService

### search

```java
RagSearchResponse search(Long projectId, RagSearchRequest request);
```

权限：

- VIEWER+

逻辑：

1. 校验项目权限
2. query 非空
3. 如果 knowledgeBaseId 不为空，校验其属于 project
4. 查询 `document_chunk`：
   - project_id = projectId
   - knowledge_base_id 可选
   - content LIKE `%query%`
   - limit 默认 10
5. 如果 LIKE 无结果：
   - 返回空 results
6. score mock：
   - content 包含 query：0.95
   - title 包含 query：0.85
   - 默认：0.5
7. 拼装 `RagSearchResponse`
8. elapsedMs 记录耗时

### toChatReferences

```java
List<ChatMessageReferenceResponse> toChatReferences(List<RagSearchResultResponse> results);
```

用于后续 Chat / Agent 引用接入。

当前阶段可以先预留内部转换方法。

## 11. Controller 设计

### 11.1 KnowledgeBaseController

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/knowledge-bases` | MAINTAINER+ | 创建知识库 |
| GET | `/api/projects/{projectId}/knowledge-bases` | VIEWER+ | 查询项目知识库列表 |
| GET | `/api/knowledge-bases/{knowledgeBaseId}` | VIEWER+ | 查询知识库详情 |
| PUT | `/api/knowledge-bases/{knowledgeBaseId}` | MAINTAINER+ | 更新知识库 |
| DELETE | `/api/knowledge-bases/{knowledgeBaseId}` | OWNER | 删除知识库 |

### 11.2 KnowledgeDocumentController

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/knowledge-documents` | DEVELOPER+ | 上传文档 |
| GET | `/api/knowledge-bases/{knowledgeBaseId}/documents` | VIEWER+ | 查询知识库文档列表 |
| GET | `/api/knowledge-documents/{documentId}` | VIEWER+ | 查询文档详情 |
| DELETE | `/api/knowledge-documents/{documentId}` | MAINTAINER+ | 删除文档 |
| GET | `/api/knowledge-documents/{documentId}/chunks` | VIEWER+ | 查询文档切片 |

### 11.3 RagSearchController

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/rag/search` | VIEWER+ | 项目内 RAG 检索 |

## 12. 与 Chat / Agent 的关系

Milestone 7 当前阶段只做预留，不强行改造 Chat 和 Orchestrator。

但是 RAG search 返回结果结构必须能映射到 Chat references：

```json
{
  "referenceType": "DOCUMENT",
  "referenceId": "chunkId",
  "title": "document title",
  "filePath": "docs/api-design.md",
  "startLine": null,
  "endLine": null,
  "score": 0.95,
  "snippet": "chunk content..."
}
```

后续 Milestone 8 可接入：

- Chat sendMessage 自动检索知识库
- Agent executeTask 自动拼接 RAG context
- Agent output 自动生成引用

## 13. 错误处理

| 场景 | 错误码 |
|---|---|
| 未登录 | UNAUTHORIZED |
| 无项目权限 | PROJECT_ACCESS_DENIED |
| knowledgeBase 不存在 | NOT_FOUND |
| document 不存在 | NOT_FOUND |
| documentType 不支持 | BAD_REQUEST |
| content 为空 | BAD_REQUEST |
| chunkSize 非法 | BAD_REQUEST |
| duplicate knowledge base name | CONFLICT |
| knowledgeBase 不属于当前 project | PROJECT_ACCESS_DENIED 或 BAD_REQUEST |

如果 `ErrorCode` 暂时缺少更细业务码，优先复用已有通用错误码，不要为了本阶段大范围扩展错误码。

## 14. 验收标准

### 14.1 编译测试

必须通过：

```bash
cd backend
mvn compile
mvn test
```

### 14.2 手动接口验证

前置：

1. 启动 MySQL
2. 启动后端
3. 登录 admin
4. 创建 project

### 14.3 创建知识库

请求：

```http
POST /api/projects/{projectId}/knowledge-bases
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "name": "Project Docs",
  "description": "项目文档知识库",
  "chunkSize": 200,
  "chunkOverlap": 20
}
```

期望：

- status = `ACTIVE`
- embeddingProvider = `MOCK`
- embeddingModel = `mock-embedding-v1`
- documentCount = 0
- chunkCount = 0

### 14.4 上传 Markdown 文档

请求：

```http
POST /api/projects/{projectId}/knowledge-documents
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "knowledgeBaseId": "{knowledgeBaseId}",
  "title": "API Design",
  "documentType": "MARKDOWN",
  "sourceType": "MANUAL",
  "fileName": "api-design.md",
  "filePath": "docs/api-design.md",
  "content": "# API Design\n\n项目接口设计包含 Auth、Project、Task、Agent Orchestrator。"
}
```

期望：

- document.status = `COMPLETED`
- chunkCount > 0
- tokenCount > 0

### 14.5 查询知识库列表

```http
GET /api/projects/{projectId}/knowledge-bases
Authorization: Bearer <token>
```

期望：

- total >= 1
- documentCount = 1
- chunkCount > 0

### 14.6 查询文档列表

```http
GET /api/knowledge-bases/{knowledgeBaseId}/documents
Authorization: Bearer <token>
```

期望：

- 返回上传的文档

### 14.7 查询文档切片

```http
GET /api/knowledge-documents/{documentId}/chunks
Authorization: Bearer <token>
```

期望：

- 返回 chunks
- content 非空
- tokenCount > 0

### 14.8 RAG 搜索

请求：

```http
POST /api/projects/{projectId}/rag/search
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "query": "Agent Orchestrator",
  "knowledgeBaseId": "{knowledgeBaseId}",
  "limit": 5,
  "includeContent": true
}
```

期望：

- results 非空
- score > 0
- referenceType = `DOCUMENT`
- snippet/content 包含查询相关内容

### 14.9 搜索无结果

请求体：

```json
{
  "query": "不存在的关键词XYZ",
  "knowledgeBaseId": "{knowledgeBaseId}",
  "limit": 5
}
```

期望：

- results = []
- total = 0

### 14.10 上传不支持类型 PDF

请求体：

```json
{
  "knowledgeBaseId": "{knowledgeBaseId}",
  "title": "PDF Doc",
  "documentType": "PDF",
  "content": "fake pdf content"
}
```

期望：

- BAD_REQUEST

### 14.11 无 token

期望：

- UNAUTHORIZED

### 14.12 无项目权限

期望：

- PROJECT_ACCESS_DENIED

## 15. 完成报告模板

完成后请按以下格式输出：

```markdown
# Milestone 7 完成报告

## 1. 新增/修改文件清单

...

## 2. 数据库表和索引清单

...

## 3. 新增 API 清单

...

## 4. Knowledge Base 流程

...

## 5. Document Upload + Chunk 流程

...

## 6. Mock Embedding 策略

...

## 7. RAG Search 策略

...

## 8. 与 Chat / Agent references 的预留设计

...

## 9. mvn compile / mvn test 结果

...

## 10. 手动接口验证结果

...

## 11. 是否可以进入 Milestone 8：RAG 接入 Chat + Agent Orchestrator

...
```

## 16. Milestone 8 预告

如果 Milestone 7 验证通过，下一阶段进入：

```text
Milestone 8: RAG 接入 Chat + Agent Orchestrator
```

建议范围：

- Chat sendMessage 自动执行 RAG Search
- ChatMessageReference 自动落库
- Agent executeTask 自动拼接 RAG context
- AgentExecution.inputPrompt 保存 RAG context
- ModelRequest.context 填入检索结果
- Orchestrator 输出引用结果

