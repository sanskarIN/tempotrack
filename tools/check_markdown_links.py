#!/usr/bin/env python3
"""Fail when repository-local Markdown links point at missing paths."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
LINK_PATTERN = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
SKIPPED_PREFIXES = ("http://", "https://", "mailto:", "tel:", "#", "data:")
SKIPPED_SCHEMES = ("ftp://", "ssh://", "git://")


def markdown_files() -> list[Path]:
    ignored = {".git", ".gradle", "build", "node_modules"}
    return sorted(
        path
        for path in ROOT.rglob("*.md")
        if not any(part in ignored for part in path.relative_to(ROOT).parts)
    )


def normalize_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and target.endswith(">"):
        target = target[1:-1].strip()
    # Markdown destinations may include an optional title after whitespace.
    if " " in target and not target.startswith(("http://", "https://")):
        target = target.split(" ", 1)[0]
    return unquote(target.split("#", 1)[0])


def local_target(source: Path, raw_target: str) -> Path | None:
    target = normalize_target(raw_target)
    if not target:
        return None
    lowered = target.lower()
    if lowered.startswith(SKIPPED_PREFIXES) or lowered.startswith(SKIPPED_SCHEMES):
        return None

    if target.startswith("/"):
        return ROOT / target.lstrip("/")
    return source.parent / target


def main() -> int:
    failures: list[str] = []
    for source in markdown_files():
        text = source.read_text(encoding="utf-8")
        for match in LINK_PATTERN.finditer(text):
            raw_target = match.group(1)
            target = local_target(source, raw_target)
            if target is None:
                continue
            if not target.resolve().exists():
                failures.append(
                    f"{source.relative_to(ROOT)} -> {raw_target} (missing {target.resolve()})",
                )

    if failures:
        print("Broken repository-local Markdown links:", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print(f"Checked {len(markdown_files())} Markdown files: local links are valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
