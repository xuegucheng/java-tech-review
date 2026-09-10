#!/usr/bin/env python3
"""Validate repository Markdown links and fenced code blocks."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parents[1]
IGNORED_DIRS = {".git", ".idea", "target", "__pycache__"}
LINK_PATTERN = re.compile(r"!?\[[^\]]*\]\(([^)\n]+)\)")
FENCE_PATTERN = re.compile(r"^\s*(```|~~~)")


def markdown_files() -> list[Path]:
    return [
        path
        for path in ROOT.rglob("*.md")
        if not any(part in IGNORED_DIRS for part in path.relative_to(ROOT).parts)
    ]


def link_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        target = target[1 : target.index(">")]
    else:
        target = target.split(maxsplit=1)[0]
    return unquote(target.split("#", maxsplit=1)[0])


def main() -> int:
    broken_links: list[str] = []
    odd_fences: list[str] = []
    total_fences = 0
    files = markdown_files()

    for path in files:
        content = path.read_text(encoding="utf-8")

        fence_count = 0
        in_fence = False
        for line_number, line in enumerate(content.splitlines(), start=1):
            if FENCE_PATTERN.match(line):
                fence_count += 1
                in_fence = not in_fence
                continue
            if in_fence:
                continue
            for match in LINK_PATTERN.finditer(line):
                target = link_target(match.group(1))
                if not target or target.startswith(("#", "http://", "https://", "mailto:", "tel:", "data:")):
                    continue
                candidate = (path.parent / target).resolve()
                if not candidate.exists():
                    broken_links.append(f"{path.relative_to(ROOT)}:{line_number} -> {target}")

        total_fences += fence_count
        if fence_count % 2:
            odd_fences.append(f"{path.relative_to(ROOT)} ({fence_count})")

    if broken_links or odd_fences:
        for item in broken_links:
            print(f"BROKEN_LINK: {item}")
        for item in odd_fences:
            print(f"ODD_FENCE_COUNT: {item}")
        print(f"FAILED: files={len(files)}, fences={total_fences}")
        return 1

    print(f"PASS: files={len(files)}, broken_links=0, fences={total_fences}, odd_fence_files=0")
    return 0


if __name__ == "__main__":
    sys.exit(main())
