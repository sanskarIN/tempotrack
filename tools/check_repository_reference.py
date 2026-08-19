#!/usr/bin/env python3
"""Ensure every tracked repository file is documented in docs/repository-reference.md."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "docs" / "repository-reference.md"


def tracked_files() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=ROOT,
        check=True,
        capture_output=True,
    )
    return sorted(
        path.decode("utf-8")
        for path in result.stdout.split(b"\0")
        if path
    )


def documented_paths(reference_text: str) -> set[str]:
    documented: set[str] = set()
    for tracked in tracked_files():
        if f"`{tracked}`" in reference_text:
            documented.add(tracked)
    return documented


def main() -> int:
    if not REFERENCE.is_file():
        print(f"Missing repository reference: {REFERENCE.relative_to(ROOT)}")
        return 1

    reference_text = REFERENCE.read_text(encoding="utf-8")
    tracked = tracked_files()
    documented = documented_paths(reference_text)
    missing = [path for path in tracked if path not in documented]

    if missing:
        print("Tracked files missing from docs/repository-reference.md:")
        for path in missing:
            print(f"- {path}")
        print("Document each tracked file with its exact path in backticks.")
        return 1

    print(f"Repository reference covers all {len(tracked)} tracked files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
