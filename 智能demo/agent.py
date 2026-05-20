#!/usr/bin/env python3
"""
最小智能体 Demo。

它展示了一个智能体的核心闭环：
1. 接收用户目标
2. 判断是否需要调用工具
3. 执行工具
4. 组织最终回复
"""

from __future__ import annotations

import ast
import json
import operator
import os
import re
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Callable


MEMORY_PATH = Path(__file__).with_name("memory.json")


class SafeCalculator:
    """只允许安全数学表达式，避免 eval 执行任意代码。"""

    OPERATORS: dict[type[ast.AST], Callable[[Any, Any], Any]] = {
        ast.Add: operator.add,
        ast.Sub: operator.sub,
        ast.Mult: operator.mul,
        ast.Div: operator.truediv,
        ast.FloorDiv: operator.floordiv,
        ast.Mod: operator.mod,
        ast.Pow: operator.pow,
    }

    UNARY_OPERATORS: dict[type[ast.AST], Callable[[Any], Any]] = {
        ast.UAdd: operator.pos,
        ast.USub: operator.neg,
    }

    def evaluate(self, expression: str) -> float | int:
        tree = ast.parse(expression, mode="eval")
        return self._eval_node(tree.body)

    def _eval_node(self, node: ast.AST) -> float | int:
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return node.value

        if isinstance(node, ast.BinOp):
            op = self.OPERATORS.get(type(node.op))
            if op is None:
                raise ValueError("不支持的运算符")
            return op(self._eval_node(node.left), self._eval_node(node.right))

        if isinstance(node, ast.UnaryOp):
            op = self.UNARY_OPERATORS.get(type(node.op))
            if op is None:
                raise ValueError("不支持的一元运算符")
            return op(self._eval_node(node.operand))

        raise ValueError("表达式只能包含数字和基础数学运算")


@dataclass(frozen=True)
class Tool:
    name: str
    description: str
    run: Callable[[str], str]


class Memory:
    def __init__(self, path: Path) -> None:
        self.path = path

    def load(self) -> list[str]:
        if not self.path.exists():
            return []

        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return []

        if isinstance(data, list):
            return [str(item) for item in data]
        return []

    def add(self, note: str) -> None:
        notes = self.load()
        notes.append(note)
        self.path.write_text(
            json.dumps(notes, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )


class DemoAgent:
    def __init__(self) -> None:
        self.memory = Memory(MEMORY_PATH)
        self.calculator = SafeCalculator()
        self.tools = {
            "time": Tool("time", "查询当前时间", self._tool_time),
            "calculate": Tool("calculate", "计算数学表达式", self._tool_calculate),
            "list_files": Tool("list_files", "列出当前目录文件", self._tool_list_files),
            "remember": Tool("remember", "保存一条记忆", self._tool_remember),
            "recall": Tool("recall", "读取已有记忆", self._tool_recall),
        }

    def run(self, user_input: str) -> str:
        user_input = user_input.strip()
        if not user_input:
            return "我在，给我一个任务吧。"

        tool_name, tool_input = self._plan(user_input)
        if tool_name:
            return self.tools[tool_name].run(tool_input)

        return self._chat(user_input)

    def _plan(self, text: str) -> tuple[str | None, str]:
        lowered = text.lower()

        if any(word in text for word in ("几点", "时间", "日期")) or "time" in lowered:
            return "time", text

        if any(word in text for word in ("当前目录", "文件", "ls", "目录")):
            return "list_files", text

        if any(word in text for word in ("记住", "保存", "记录")):
            note = re.sub(r"^(请)?(帮我)?(记住|保存|记录)[:：]?", "", text).strip()
            return "remember", note or text

        if any(word in text for word in ("记忆", "你记得", "回忆")):
            return "recall", text

        expression = self._extract_expression(text)
        if expression:
            return "calculate", expression

        return None, text

    def _extract_expression(self, text: str) -> str | None:
        normalized = (
            text.replace("加", "+")
            .replace("减", "-")
            .replace("乘以", "*")
            .replace("乘", "*")
            .replace("除以", "/")
            .replace("除", "/")
            .replace("×", "*")
            .replace("÷", "/")
        )
        matches = re.findall(r"[0-9][0-9+\-*/().% ]+[0-9)]", normalized)
        return matches[0].strip() if matches else None

    def _tool_time(self, _: str) -> str:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        return f"当前时间：{now}"

    def _tool_calculate(self, expression: str) -> str:
        try:
            result = self.calculator.evaluate(expression)
        except Exception as exc:
            return f"计算失败：{exc}"
        return f"计算结果：{result}"

    def _tool_list_files(self, _: str) -> str:
        files = sorted(path.name for path in Path.cwd().iterdir())
        if not files:
            return "当前目录是空的。"
        return "当前目录文件：\n" + "\n".join(f"- {name}" for name in files)

    def _tool_remember(self, note: str) -> str:
        cleaned = note.strip()
        if not cleaned:
            return "要记住的内容是空的。"
        self.memory.add(cleaned)
        return f"已记住：{cleaned}"

    def _tool_recall(self, _: str) -> str:
        notes = self.memory.load()
        if not notes:
            return "我还没有保存任何记忆。"
        return "我记得：\n" + "\n".join(f"- {note}" for note in notes)

    def _chat(self, text: str) -> str:
        memories = self.memory.load()
        memory_hint = f"我当前记得 {len(memories)} 条信息。" if memories else "我还没有长期记忆。"
        return (
            f"收到：{text}\n"
            f"{memory_hint}\n"
            "这个 Demo 智能体目前会自动调用时间、计算、目录和记忆工具。"
        )


def interactive(agent: DemoAgent) -> None:
    print("智能体已启动。输入 exit / quit 退出。")
    while True:
        try:
            user_input = input("\n你: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n再见。")
            return

        if user_input.lower() in {"exit", "quit", "q"}:
            print("再见。")
            return

        print(f"智能体: {agent.run(user_input)}")


def main() -> None:
    agent = DemoAgent()
    if len(sys.argv) > 1:
        print(agent.run(" ".join(sys.argv[1:])))
        return

    interactive(agent)


if __name__ == "__main__":
    os.chdir(Path(__file__).parent)
    main()

