#!/usr/bin/env python3
"""gen_licenses.py — the open-source licences page of Hain (T-285).

Reads tools/licenses/oss.toml (the curated list), gradle/libs.versions.toml (versions),
core-models/src/main/assets/catalog.json (models) and writes

    feature-settings/src/main/assets/licenses/licenses.json
    feature-settings/src/main/assets/licenses/texts/*.txt   (copied from tools/licenses/texts/)

The app shows them offline in Settings → Über Hain → Open-source-Lizenzen. No network.

    python3 tools/licenses/gen_licenses.py           write the assets
    python3 tools/licenses/gen_licenses.py --check   CI: fail if anything is missing or stale

--check fails when
  * a runtime Gradle dependency (implementation/api/runtimeOnly of a module) has no entry,
  * a model of the catalogue or a font file in core-ui has no entry,
  * an entry names a Gradle alias, a licence text or a LICENSES.md row that does not exist,
  * the llama.cpp submodule moved away from the commit its entry names,
  * the assets in feature-settings differ from what this script would write.
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
SRC = ROOT / "tools/licenses/oss.toml"
TEXTS = ROOT / "tools/licenses/texts"
OUT = ROOT / "feature-settings/src/main/assets/licenses"
VERSIONS = ROOT / "gradle/libs.versions.toml"
CATALOG = ROOT / "core-models/src/main/assets/catalog.json"
FONTS = ROOT / "core-ui/src/main/res/font"
LICENSES_MD = ROOT / "docs/specs/LICENSES.md"

GROUPS = ["libraries", "models", "fonts", "icons", "engine", "content"]
# Modules that never reach the APK (test helpers).
TEST_ONLY_MODULES = {"core-testing"}
RUNTIME_CONFIGS = ("implementation", "api", "runtimeOnly", "releaseImplementation")
DEP_LINE = re.compile(r"^\s*(" + "|".join(RUNTIME_CONFIGS) + r")\((.*)\)\s*$")
LIBS_REF = re.compile(r"libs\.([A-Za-z0-9_.]+)")


def norm(alias: str) -> str:
    """Gradle's accessor for a catalogue alias: `room-runtime` and `libs.room.runtime` meet here."""
    return re.sub(r"[-_.]", ".", alias).lower()


def runtime_aliases(lib_aliases: dict[str, str]) -> dict[str, list[str]]:
    """Catalogue aliases used at runtime → modules that use them."""
    found: dict[str, list[str]] = {}
    for build in sorted(ROOT.glob("*/build.gradle.kts")):
        module = build.parent.name
        if module in TEST_ONLY_MODULES:
            continue
        for line in build.read_text().splitlines():
            m = DEP_LINE.match(line)
            if not m:
                continue
            for ref in LIBS_REF.findall(m.group(2)):
                alias = lib_aliases.get(norm(ref))
                if alias is None:
                    continue  # e.g. libs.versions…
                found.setdefault(alias, []).append(module)
    return found


def submodule_sha(path: str) -> str | None:
    try:
        out = subprocess.run(
            ["git", "-C", str(ROOT), "ls-tree", "HEAD", path], capture_output=True, text=True, check=True
        ).stdout.split()
    except (OSError, subprocess.CalledProcessError):
        return None
    return out[2] if len(out) >= 3 else None


def build(errors: list[str]) -> tuple[str, dict[str, bytes]]:
    src = tomllib.loads(SRC.read_text())
    toml = tomllib.loads(VERSIONS.read_text())
    versions = toml["versions"]
    lib_aliases = {norm(a): a for a in toml["libraries"]}
    md = LICENSES_MD.read_text()

    texts: dict[str, bytes] = {}

    def take_texts(owner: str, names: list[str]) -> list[str]:
        if not names:
            errors.append(f"{owner}: no licence text")
        for n in names:
            f = TEXTS / n
            if not f.is_file():
                errors.append(f"{owner}: licence text {n} missing in tools/licenses/texts/")
            else:
                texts[n] = f.read_bytes()
        return names

    def version(owner: str, v: str) -> str:
        if v.startswith("toml:"):
            key = v[len("toml:"):]
            if key not in versions:
                errors.append(f"{owner}: version key {key} not in libs.versions.toml")
                return ""
            return str(versions[key])
        return v

    app = src["app"]
    out_app = {
        "name": app["name"],
        "licence": app["licence"],
        "url": app["url"],
        "texts": take_texts("app", app["texts"]),
    }

    groups: dict[str, list[dict]] = {g: [] for g in GROUPS}
    covered_gradle: set[str] = set()
    covered_catalog: set[str] = set()
    font_prefixes: list[str] = []
    for e in src["entry"]:
        name = e["name"]
        if e["group"] not in groups:
            errors.append(f"{name}: unknown group {e['group']}")
            continue
        for key in ("licence", "url", "md"):
            if not e.get(key):
                errors.append(f"{name}: '{key}' is empty")
        if e.get("md") and e["md"] not in md:
            errors.append(f"{name}: no row with '{e['md']}' in docs/specs/LICENSES.md")
        if e.get("url") and not e["url"].startswith("https://"):
            errors.append(f"{name}: url must be https")
        for alias in e.get("gradle", []):
            if alias not in toml["libraries"]:
                errors.append(f"{name}: gradle alias {alias} not in libs.versions.toml")
            covered_gradle.add(alias)
        covered_catalog.update(e.get("catalog", []))
        font_prefixes.extend(e.get("fonts", []))
        if "submodule" in e:
            sha = submodule_sha(e["submodule"])
            if sha is None:
                print(f"note: could not read the submodule pointer of {e['submodule']}", file=sys.stderr)
            elif sha != e["sha"]:
                errors.append(f"{name}: {e['submodule']} is at {sha[:7]}, oss.toml says {e['sha'][:7]} – update version")
        groups[e["group"]].append(
            {
                "name": name,
                "version": version(name, e.get("version", "")),
                "licence": e["licence"],
                "url": e["url"],
                "texts": take_texts(name, e.get("texts", [])),
            }
        )

    for alias, modules in sorted(runtime_aliases(lib_aliases).items()):
        if alias not in covered_gradle:
            errors.append(f"runtime dependency {alias} ({', '.join(sorted(set(modules)))}) has no entry in oss.toml")

    catalog = json.loads(CATALOG.read_text())
    for model in catalog["models"]:
        if model["id"] not in covered_catalog:
            errors.append(f"catalogue model {model['id']} has no entry in oss.toml")
    for font in sorted(FONTS.glob("*")):
        if not any(font.name.startswith(p) for p in font_prefixes):
            errors.append(f"font {font.name} has no entry in oss.toml")

    data = {
        "app": out_app,
        "groups": [
            {"id": g, "entries": sorted(groups[g], key=lambda x: x["name"].lower())} for g in GROUPS if groups[g]
        ],
    }
    return json.dumps(data, ensure_ascii=False, indent=1) + "\n", texts


def main() -> int:
    check = "--check" in sys.argv[1:]
    errors: list[str] = []
    js, texts = build(errors)
    if check:
        target = OUT / "licenses.json"
        if not target.is_file() or target.read_text() != js:
            errors.append("feature-settings/src/main/assets/licenses/licenses.json is stale – run gen_licenses.py")
        have = {p.name for p in (OUT / "texts").glob("*")} if (OUT / "texts").is_dir() else set()
        for n, b in texts.items():
            p = OUT / "texts" / n
            if not p.is_file() or p.read_bytes() != b:
                errors.append(f"assets text {n} missing or stale – run gen_licenses.py")
        for extra in sorted(have - set(texts)):
            errors.append(f"assets text {extra} is not used by any entry – run gen_licenses.py")
    if errors:
        print("Open-source licences: problems found", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1
    if not check:
        (OUT / "texts").mkdir(parents=True, exist_ok=True)
        for old in (OUT / "texts").glob("*"):
            if old.name not in texts:
                old.unlink()
        for n, b in texts.items():
            (OUT / "texts" / n).write_bytes(b)
        (OUT / "licenses.json").write_text(js)
    count = sum(len(g["entries"]) for g in json.loads(js)["groups"])
    print(f"Open-source licences: {count} entries, {len(texts)} texts – {'ok' if check else 'written'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
