#!/usr/bin/env python3
"""check_strings.py — verify DE/EN string parity across all modules.

For every module that ships string resources, the key set of
`src/main/res/values/strings.xml` (English, the default locale) must match
exactly with every other locale directory (values-de/, values-fr/, ...).

Exit codes:
    0  all locale key sets match
    1  at least one mismatch, malformed file, or missing default strings.xml

Run from the repo root:  python3 tools/check_strings.py
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
# Android resource entries use the *unprefixed* name attribute
# (<string name="...">); aapt2 silently drops android:name on <string>.
NAME_ATTR = "name"
# <string name="..."> and <plurals name="...">, nothing else carries a name.
RESOURCE_TAGS = {"string", "plurals"}
EXCLUDED_DIRS = {"build", "third_party"}


def module_dirs() -> list[Path]:
    """All Gradle module directories (a module has a build.gradle or build.gradle.kts)."""
    modules = []
    for gradle_file in REPO_ROOT.rglob("build.gradle*"):
        if any(part in EXCLUDED_DIRS for part in gradle_file.parts):
            continue
        modules.append(gradle_file.parent)
    return sorted(set(modules))


def extract_names(path: Path) -> set[str]:
    """Parse a strings.xml and return the set of resource names it declares."""
    try:
        tree = ET.parse(path)
    except ET.ParseError as exc:
        raise SystemExit(f"ERROR: malformed XML: {path}: {exc}") from None
    names = set()
    for element in tree.getroot():
        if element.tag in RESOURCE_TAGS:
            name = element.get(NAME_ATTR)
            if not name:
                raise SystemExit(f"ERROR: {element.tag} without name in {path}")
            names.add(name)
    return names


def locale_values_dirs(module: Path) -> dict[str, Path]:
    """Map locale dir name -> values dir for every dir that has a strings.xml."""
    result = {}
    res_dir = module / "src" / "main" / "res"
    if not res_dir.is_dir():
        return result
    for values_dir in res_dir.glob("values*"):
        if (values_dir / "strings.xml").is_file():
            result[values_dir.name] = values_dir
    return result


def main() -> int:
    problems: list[str] = []
    modules_checked = 0

    for module in module_dirs():
        locales = locale_values_dirs(module)
        if not locales:
            continue
        if "values" not in locales:
            problems.append(f"{module.name}: has locale strings (values-de) but no default values/strings.xml")
            continue
        modules_checked += 1
        default_names = extract_names(locales["values"] / "strings.xml")
        for locale, values_dir in sorted(locales.items()):
            if locale == "values":
                continue
            locale_names = extract_names(values_dir / "strings.xml")
            missing = sorted(default_names - locale_names)
            extra = sorted(locale_names - default_names)
            if missing:
                problems.append(f"{module.name}/{locale}: missing keys: {', '.join(missing)}")
            if extra:
                problems.append(f"{module.name}/{locale}: extra keys: {', '.join(extra)}")
        if not default_names:
            problems.append(f"{module.name}/values: strings.xml declares no resources")

    if problems:
        for problem in problems:
            print(f"FAIL: {problem}")
        print(f"check_strings: {len(problems)} problem(s) in {modules_checked} module(s)")
        return 1

    print(f"check_strings: OK ({modules_checked} module(s) checked)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
