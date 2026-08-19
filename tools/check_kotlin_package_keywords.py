#!/usr/bin/env python3
"""Fail when TempoTrack Kotlin source uses the keyword `in` as an unescaped package segment."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = (
    ROOT / "shared" / "src",
    ROOT / "androidApp" / "src",
    ROOT / "desktopApp" / "src",
)
UNESCAPED_PREFIXES = (
    "package in.sanskar.",
    "import in.sanskar.",
)


def find_violations() -> list[str]:
    violations: list[str] = []
    for source_root in SOURCE_ROOTS:
        if not source_root.exists():
            continue
        for path in sorted(source_root.rglob("*.kt")):
            for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
                stripped = line.lstrip()
                if stripped.startswith(UNESCAPED_PREFIXES):
                    relative = path.relative_to(ROOT)
                    violations.append(f"{relative}:{line_number}: {stripped}")
    return violations


def main() -> int:
    violations = find_violations()
    if violations:
        print("Kotlin source uses keyword `in` as an unescaped package/import segment:")
        for violation in violations:
            print(f"- {violation}")
        print("Use `in`.sanskar... in Kotlin source; this preserves the runtime package name.")
        return 1

    print("Kotlin package keyword check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
