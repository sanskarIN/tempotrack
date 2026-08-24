#!/usr/bin/env python3
"""Verify TempoTrack uses one Gradle version across bootstrap and automation."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WRAPPER_PROPERTIES = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"
UNIX_LAUNCHER = ROOT / "gradlew"
WINDOWS_LAUNCHER = ROOT / "gradlew.bat"
WORKFLOWS_DIR = ROOT / ".github" / "workflows"
SETUP_GRADLE_ACTION = "gradle/actions/setup-gradle"

VERSION_PATTERN = r"[0-9]+\.[0-9]+\.[0-9]+"


def read(path: Path) -> str:
    if not path.is_file():
        raise ValueError(f"Missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def wrapper_version(properties: str) -> str:
    match = re.search(
        rf"^distributionUrl=.*gradle-({VERSION_PATTERN})-bin\.zip$",
        properties,
        flags=re.MULTILINE,
    )
    if not match:
        raise ValueError("Unable to read Gradle version from distributionUrl")
    return match.group(1)


def require_distribution_checksum(properties: str) -> None:
    if not re.search(
        r"^distributionSha256Sum=([0-9a-f]{64})$",
        properties,
        flags=re.MULTILINE,
    ):
        raise ValueError("gradle-wrapper.properties must pin a 64-character SHA-256 checksum")


def require_retry_policy(properties: str) -> None:
    retries = re.search(r"^retries=([0-9]+)$", properties, flags=re.MULTILINE)
    backoff = re.search(r"^retryBackOffMs=([0-9]+)$", properties, flags=re.MULTILINE)
    if not retries or int(retries.group(1)) < 1:
        raise ValueError("gradle-wrapper.properties must configure at least one download retry")
    if not backoff or int(backoff.group(1)) < 1:
        raise ValueError("gradle-wrapper.properties must configure a positive retry backoff")


def require_launcher_version(path: Path, text: str, expected: str) -> None:
    if path.name == "gradlew":
        pattern = rf'^REQUIRED_GRADLE_VERSION="({VERSION_PATTERN})"$'
    else:
        pattern = rf"^set REQUIRED_GRADLE_VERSION=({VERSION_PATTERN})$"

    match = re.search(pattern, text, flags=re.MULTILINE)
    if not match:
        raise ValueError(f"Unable to read required Gradle version from {path.relative_to(ROOT)}")
    if match.group(1) != expected:
        raise ValueError(
            f"{path.relative_to(ROOT)} requires Gradle {match.group(1)}, expected {expected}"
        )


def gradle_workflows() -> list[tuple[Path, str]]:
    if not WORKFLOWS_DIR.is_dir():
        raise ValueError(f"Missing workflows directory: {WORKFLOWS_DIR.relative_to(ROOT)}")

    workflows: list[tuple[Path, str]] = []
    for pattern in ("*.yml", "*.yaml"):
        for path in sorted(WORKFLOWS_DIR.glob(pattern)):
            text = read(path)
            if SETUP_GRADLE_ACTION in text:
                workflows.append((path, text))

    if not workflows:
        raise ValueError("No workflow using gradle/actions/setup-gradle was found")
    return workflows


def require_workflow_versions(path: Path, text: str, expected: str) -> None:
    versions = re.findall(rf'gradle-version:\s*["\']?({VERSION_PATTERN})["\']?', text)
    if not versions:
        raise ValueError(
            f"{path.relative_to(ROOT)} uses {SETUP_GRADLE_ACTION} but does not pin gradle-version"
        )

    mismatches = sorted({version for version in versions if version != expected})
    if mismatches:
        raise ValueError(
            f"{path.relative_to(ROOT)} contains mismatched Gradle versions: {', '.join(mismatches)}; "
            f"expected {expected}"
        )


def main() -> int:
    try:
        properties = read(WRAPPER_PROPERTIES)
        expected = wrapper_version(properties)
        require_distribution_checksum(properties)
        require_retry_policy(properties)
        require_launcher_version(UNIX_LAUNCHER, read(UNIX_LAUNCHER), expected)
        require_launcher_version(WINDOWS_LAUNCHER, read(WINDOWS_LAUNCHER), expected)
        workflows = gradle_workflows()
        for workflow, text in workflows:
            require_workflow_versions(workflow, text, expected)
    except ValueError as error:
        print(f"Gradle alignment check failed: {error}")
        return 1

    workflow_names = ", ".join(path.name for path, _ in workflows)
    print(
        f"Gradle {expected} is aligned across wrapper metadata, launchers and "
        f"Gradle-bearing workflows: {workflow_names}."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
