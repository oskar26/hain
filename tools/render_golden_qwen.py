#!/usr/bin/env python3
"""Render the golden prompt files for QwenPromptBuilder (T-241).

Runs the Qwen3 chat template as stored in the Jan-v3.5-4B GGUF (prompts/jan-v3.5-4b_chat_template.jinja,
janhq/Jan-v3.5-4B-gguf Q4_K_M, revision 2b0fcab0) once per scenario and writes
core-llm/src/test/resources/golden/qwen/Q-xx_<name>.json (input) + .txt (expected prompt).
Same scenario format and Jinja setup as tools/render_golden.py. The template has no thinking switch;
Q-06 checks that enable_thinking=True changes nothing.

Hain's one deliberate difference (ADR-24 §6): with tools, the tool block comes first in the system turn
and the system text follows after "\n\n" (cacheable fixed part, T-242). The golden files with tools are
therefore the template's own output for the conversation without the system message, with
"\n\n" + system text inserted before the first <|im_end|>; everything else is the template unchanged.

Usage: python3 tools/render_golden_qwen.py   (needs: pip install jinja2)
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import jinja2  # noqa: E402
from render_golden import SEARCH, WEATHER, raise_exception, tojson  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / "prompts" / "jan-v3.5-4b_chat_template.jinja"
OUT = ROOT / "core-llm" / "src" / "test" / "resources" / "golden" / "qwen"
IM_END = "<|im_end|>"
SYSTEM = "Du bist Pim, ein freundlicher Helfer, der ganz auf diesem Handy läuft.\n\n# Kontext\nJetzt: Donnerstag, 5. November 2026, 07:30."

SCENARIOS = [
    ("Q-01_plain", {"system": None, "tools": [], "messages": [{"role": "user", "content": "Hallo Pim!"}],
                    "add_generation_prompt": True, "enable_thinking": False}),
    ("Q-02_system", {"system": SYSTEM, "tools": [], "messages": [{"role": "user", "content": "Was ist ein Hain?"}],
                     "add_generation_prompt": True, "enable_thinking": False}),
    ("Q-03_tools", {"system": SYSTEM, "tools": [WEATHER, SEARCH],
                    "messages": [{"role": "user", "content": "Wie wird das Wetter morgen in Köln?"}],
                    "add_generation_prompt": True, "enable_thinking": False}),
    ("Q-04_toolloop", {
        "system": SYSTEM, "tools": [WEATHER, SEARCH],
        "messages": [
            {"role": "user", "content": "Wie wird das Wetter morgen in Köln?"},
            {"role": "assistant", "content": "Ich schaue nach.", "reasoning": "",
             "tool_calls": [
                 {"name": "weather", "arguments": {"city": "Köln", "days": 2}},
                 {"name": "web_search", "arguments": {"query": "Köln Wetter \"morgen\"", "filter": {"lang": "de", "max": 3}}},
             ]},
            {"role": "tool", "content": "{\"city\": \"Köln\", \"tomorrow\": {\"max\": 12, \"min\": 6}}", "tool_call_id": "call_0"},
            {"role": "tool", "content": "[1] wetter.com: Köln morgen Regen, 12 °C", "tool_call_id": "call_1"},
        ],
        "add_generation_prompt": True, "enable_thinking": False}),
    ("Q-05_multiturn", {
        "system": SYSTEM, "tools": [WEATHER],
        "messages": [
            {"role": "user", "content": "Stell mir einen Wecker um 6:45."},
            {"role": "assistant", "content": "", "tool_calls": [{"name": "weather", "arguments": {"city": "Köln"}}]},
            {"role": "tool", "content": "ok", "tool_call_id": "call_0"},
            {"role": "assistant", "content": "Erledigt, der Wecker ist vorbereitet."},
            {"role": "user", "content": "Eine Viertelstunde früher, bitte."},
        ],
        "add_generation_prompt": True, "enable_thinking": False}),
    ("Q-06_thinking", {"system": SYSTEM, "tools": [], "messages": [{"role": "user", "content": "Warum ist der Himmel blau?"}],
                       "add_generation_prompt": True, "enable_thinking": True}),
]


def to_template_messages(scenario: dict) -> list[dict]:
    messages: list[dict] = []
    if scenario["system"] is not None:
        messages.append({"role": "system", "content": scenario["system"]})
    for m in scenario["messages"]:
        out = {"role": m["role"], "content": m["content"]}
        if "reasoning" in m:
            out["reasoning_content"] = m["reasoning"]
        if m.get("tool_calls"):
            out["tool_calls"] = [{"function": {"name": c["name"], "arguments": c["arguments"]}} for c in m["tool_calls"]]
        messages.append(out)
    return messages


def main() -> None:
    env = jinja2.Environment(extensions=["jinja2.ext.loopcontrols"], keep_trailing_newline=True)
    env.policies["json.dumps_function"] = json.dumps
    env.filters["tojson"] = tojson
    env.globals["raise_exception"] = raise_exception
    template = env.from_string(TEMPLATE.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    for name, scenario in SCENARIOS:
        tools = [{"type": "function", "function": t} for t in scenario["tools"]]
        messages = to_template_messages(scenario)
        system = scenario["system"]
        moved = bool(tools) and system is not None
        prompt = template.render(
            messages=messages[1:] if moved else messages,
            tools=tools or None,
            add_generation_prompt=scenario["add_generation_prompt"],
            enable_thinking=scenario["enable_thinking"],
        )
        if moved:
            end = prompt.index(IM_END)
            prompt = prompt[:end] + "\n\n" + system + prompt[end:]
        (OUT / f"{name}.json").write_text(json.dumps(scenario, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (OUT / f"{name}.txt").write_bytes(prompt.encode("utf-8"))
        print(f"{name}: {len(prompt)} chars")


if __name__ == "__main__":
    main()
