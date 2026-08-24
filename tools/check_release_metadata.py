#!/usr/bin/env python3
"""Validate TempoTrack release metadata and optional semantic release tag."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE_PROPERTIES = ROOT / "gradle.properties"
README = ROOT / "README.md"
CHANGELOG = ROOT / "CHANGELOG.md"
ROADMAP = ROOT / "ROADMAP.md"

SEMVER_RE = re.compile(r"^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$")
TAG_RE = re.compile(r"^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$")
CHANGELOG_DATE_RE = re.compile(r"^## \[(?P<version>[^]]+)] - \d{4}-\d{2}-\d{2}$", re.MULTILINE)
MAX_ANDROID_VERSION_CODE = 2_100_000_000


def read(path: Path) -> str:
    if not path.is_file():
        raise ValueError(f"Missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def parse_gradle_properties(text: str) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def parse_source_version() -> tuple[str, int]:
    properties = parse_gradle_properties(read(GRADLE_PROPERTIES))
    version = properties.get("appVersion")
    version_code_text = properties.get("appVersionCode")
    if version is None:
        raise ValueError("gradle.properties is missing appVersion")
    if version_code_text is None:
        raise ValueError("gradle.properties is missing appVersionCode")
    if not version_code_text.isdigit():
        raise ValueError("gradle.properties appVersionCode must be a positive decimal integer")
    return version, int(version_code_text)


def derive_android_version_code(version: str) -> int:
    match = SEMVER_RE.fullmatch(version)
    if not match:
        raise ValueError(
            "appVersion must use canonical MAJOR.MINOR.PATCH without leading zeros"
        )

    major_text, minor_text, patch_text = match.groups()
    if len(major_text) > 6 or len(minor_text) > 2 or len(patch_text) > 2:
        raise ValueError("Version components are too large for the Android versionCode mapping")

    major = int(major_text)
    minor = int(minor_text)
    patch = int(patch_text)
    if minor > 99 or patch > 99:
        raise ValueError("Android versionCode mapping requires MINOR and PATCH in 0..99")

    version_code = major * 10_000 + minor * 100 + patch
    if not 1 <= version_code <= MAX_ANDROID_VERSION_CODE:
        raise ValueError(
            f"Derived Android versionCode {version_code} is outside 1..{MAX_ANDROID_VERSION_CODE}"
        )
    return version_code


def validate_document_markers(version: str) -> None:
    readme = read(README)
    changelog = read(CHANGELOG)
    roadmap = read(ROADMAP)

    if f"Current release line: **{version}**" not in readme:
        raise ValueError(f"README.md current release marker does not identify {version}")

    changelog_versions = {
        match.group("version") for match in CHANGELOG_DATE_RE.finditer(changelog)
    }
    if version not in changelog_versions:
        raise ValueError(
            f"CHANGELOG.md must contain a dated release heading for {version}"
        )

    if not re.search(rf"^## {re.escape(version)} — .+$", roadmap, flags=re.MULTILINE):
        raise ValueError(f"ROADMAP.md must contain a release section for {version}")


def validate_tag(tag: str, source_version: str) -> None:
    match = TAG_RE.fullmatch(tag)
    if not match:
        raise ValueError(
            "Release tag must use canonical vMAJOR.MINOR.PATCH without leading zeros"
        )
    tagged_version = ".".join(match.groups())
    if tagged_version != source_version:
        raise ValueError(
            f"Release tag {tag} does not match gradle.properties appVersion={source_version}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate source release metadata and, optionally, a release tag."
    )
    parser.add_argument(
        "--tag",
        help="Optional release tag to validate, for example v2.12.4.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        version, source_version_code = parse_source_version()
        expected_version_code = derive_android_version_code(version)
        if source_version_code != expected_version_code:
            raise ValueError(
                f"appVersionCode={source_version_code} does not match appVersion={version}; "
                f"expected {expected_version_code}"
            )
        validate_document_markers(version)
        if args.tag:
            validate_tag(args.tag, version)
    except ValueError as error:
        print(f"Release metadata check failed: {error}")
        return 1

    tag_suffix = f" and tag {args.tag}" if args.tag else ""
    print(
        f"Release metadata is consistent for {version} / Android versionCode "
        f"{source_version_code}{tag_suffix}."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
