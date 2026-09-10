#!/usr/bin/env python3
"""Reject known text-corruption patterns without banning valid domain terms."""

from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
IGNORED_DIRS = {".git", ".idea", "target", "__pycache__"}
FORBIDDEN_PATTERNS = (
    "数据存存储",
    "数据存查询",
    "数据存连接",
    "数据存会话",
    "数据存十进制",
    "查询数据存",
    "务务",
)
FORBIDDEN_PATTERN = re.compile("|".join(re.escape(item) for item in FORBIDDEN_PATTERNS))


def markdown_files() -> list[Path]:
    return [
        path
        for path in ROOT.rglob("*.md")
        if not any(part in IGNORED_DIRS for part in path.relative_to(ROOT).parts)
    ]


def main() -> int:
    violations: list[tuple[Path, int, str]] = []

    for path in markdown_files():
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            violations.extend(
                (path, line_number, match.group(0))
                for match in FORBIDDEN_PATTERN.finditer(line)
            )

    if violations:
        for path, line_number, text in violations:
            print("FORBIDDEN_TEXT:")
            print(f"{path.relative_to(ROOT).as_posix()}:{line_number}")
            print(text)
        return 1

    print(f"PASS: files={len(markdown_files())}, forbidden_patterns=0")
    return 0


if __name__ == "__main__":
    sys.exit(main())
