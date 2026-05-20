# 智能体 Demo

一个最小可运行的命令行智能体示例，内置工具调用、任务规划、记忆文件和交互式对话。

## 功能

- 支持交互式聊天
- 自动选择工具：时间查询、计算器、文件列表、笔记记忆
- 无需 API Key，默认使用本地规则引擎演示智能体流程
- 结构清晰，方便后续接入 OpenAI、Claude、DeepSeek、Qwen 等模型

## 快速开始

```bash
python3 agent.py
```

也可以直接执行单轮任务：

```bash
python3 agent.py "帮我计算 23 * 19"
python3 agent.py "记住：我的项目叫 AI Coding Platform"
python3 agent.py "现在几点"
```

## 示例

```text
你: 帮我计算 1024 / 8
智能体: 计算结果：128.0

你: 记住：后端使用 Spring Boot
智能体: 已记住：后端使用 Spring Boot

你: 看看当前目录
智能体: 当前目录文件：
- README.md
- agent.py
- memory.json
```

## 文件说明

| 文件 | 说明 |
|---|---|
| `agent.py` | 智能体主程序 |
| `memory.json` | 自动生成的本地记忆文件 |

