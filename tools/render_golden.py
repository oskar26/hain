#!/usr/bin/env python3
"""Render the golden prompt files for LfmPromptBuilder (T-041).

Runs the official LFM3 chat template (prompts/lfm3-1.7b_chat_template.jinja) once per
scenario and writes, next to each other:

    core-llm/src/test/resources/golden/G-xx_<name>.json   scenario input (read by the Kotlin test)
    core-llm/src/test/resources/golden/G-xx_<name>.txt    expected prompt, byte-for-byte

The Jinja environment mirrors Hugging Face transformers / llama.cpp:
- `tojson` = json.dumps(ensure_ascii=False, sort_keys=False) with the default ", " / ": "
  separators (plain jinja2 would sort keys and HTML-escape, which runtimes do not do),
- `{% generation %}` blocks render their body unchanged,
- `raise_exception()` raises.

Scenario JSON format (also what LfmPromptBuilderTest parses):
{
  "system": "..." | null,
  "tools": [{"name": .., "description": .., "parameters": {..}}],
  "messages": [{"role": "user|assistant|tool|system", "content": "..",
                "reasoning": ".." (optional), "tool_calls": [{"name": .., "arguments": {..}}],
                "tool_call_id": ".." (optional)}],
  "add_generation_prompt": true,
  "enable_thinking": true
}

Usage: python3 tools/render_golden.py          (needs: pip install jinja2)
       python3 tools/render_golden.py --omni   (T-260: the same scenarios with thinking off through
                                                prompts/lfm3-omni-2b_chat_template.jinja → golden/omni/O-xx;
                                                LFM3-Omni 2B uses LfmPromptBuilder, these prove it fits)
"""

from __future__ import annotations

import json
from pathlib import Path

import jinja2
from jinja2 import nodes
from jinja2.ext import Extension

ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / "prompts" / "lfm3-1.7b_chat_template.jinja"
OUT = ROOT / "core-llm" / "src" / "test" / "resources" / "golden"
BOS = "<|startoftext|>"


class GenerationExtension(Extension):
    """`{% generation %}...{% endgeneration %}` (HF training marker) renders its body."""

    tags = {"generation"}

    def parse(self, parser):
        lineno = next(parser.stream).lineno
        body = parser.parse_statements(("name:endgeneration",), drop_needle=True)
        return nodes.Scope(body, lineno=lineno)


def tojson(value, indent=None):
    return json.dumps(value, ensure_ascii=False, indent=indent, sort_keys=False)


def raise_exception(message):
    raise jinja2.exceptions.TemplateError(message)


WEATHER = {
    "name": "weather",
    "description": "Weather forecast for a place. Use for weather, rain, temperature.",
    "parameters": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "City name, e.g. Köln"},
            "days": {"type": "integer", "description": "Forecast days, 1-7"},
        },
        "required": ["city"],
    },
}
SEARCH = {
    "name": "web_search",
    "description": "Search the web for current information.",
    "parameters": {
        "type": "object",
        "properties": {"query": {"type": "string", "description": "Search words"}},
        "required": ["query"],
    },
}
SYSTEM = "You are Hain, a friendly helper that runs fully on this phone.\n\n# Context\nNow: Sonntag, 1. November 2026, 19:40 (Europe/Berlin). Place: Köln."

SCENARIOS = [
    (
        "G-01_plain",
        {
            "system": None,
            "tools": [],
            "messages": [{"role": "user", "content": "Hallo Pim!"}],
            "add_generation_prompt": True,
            "enable_thinking": True,
        },
    ),
    (
        "G-02_system",
        {
            "system": SYSTEM,
            "tools": [],
            "messages": [{"role": "user", "content": "Was ist ein Hain?"}],
            "add_generation_prompt": True,
            "enable_thinking": True,
        },
    ),
    (
        "G-03_tools",
        {
            "system": SYSTEM,
            "tools": [WEATHER, SEARCH],
            "messages": [{"role": "user", "content": "Wie wird das Wetter morgen in Köln?"}],
            "add_generation_prompt": True,
            "enable_thinking": True,
        },
    ),
    (
        "G-04_toolloop",
        {
            "system": SYSTEM,
            "tools": [WEATHER, SEARCH],
            "messages": [
                {"role": "user", "content": "Wie wird das Wetter morgen in Köln?"},
                {
                    "role": "assistant",
                    "content": "Ich schaue nach.\n",
                    "reasoning": "Der Nutzer will das Wetter für morgen. Ich rufe weather auf.",
                    "tool_calls": [
                        {"name": "weather", "arguments": {"city": "Köln", "days": 2}},
                        {"name": "web_search", "arguments": {"query": "Köln Wetter \"morgen\"", "strict": True, "filter": {"lang": "de", "max": 3}}},
                    ],
                },
                {"role": "tool", "content": "{\"city\": \"Köln\", \"tomorrow\": {\"max\": 12, \"min\": 6, \"code\": 61}}", "tool_call_id": "call_0"},
                {"role": "tool", "content": "[1] wetter.com: Köln morgen Regen, 12 °C", "tool_call_id": "call_1"},
            ],
            "add_generation_prompt": True,
            "enable_thinking": True,
        },
    ),
    (
        "G-05_multiturn_thinking_dropped",
        {
            "system": SYSTEM,
            "tools": [],
            "messages": [
                {"role": "user", "content": "Wie viel ist 17 * 23?"},
                {"role": "assistant", "content": "17 * 23 = 391.", "reasoning": "17*20=340, 17*3=51, 340+51=391."},
                {"role": "user", "content": "Und geteilt durch 2?"},
                {"role": "assistant", "content": "Das sind 195,5.", "reasoning": "391/2 = 195.5"},
                {"role": "user", "content": "Danke! Erklär mir kurz, warum."},
            ],
            "add_generation_prompt": True,
            "enable_thinking": True,
        },
    ),
    (
        "G-06_nothink",
        {
            "system": SYSTEM,
            "tools": [],
            "messages": [
                {"role": "user", "content": "Sag Hallo."},
                {"role": "assistant", "content": "Hallo! 🌱"},
                {"role": "user", "content": "Noch mal auf Englisch, bitte."},
            ],
            "add_generation_prompt": True,
            "enable_thinking": False,
        },
    ),
]


def to_template_messages(scenario: dict) -> list[dict]:
    messages: list[dict] = []
    if scenario["system"] is not None:
        messages.append({"role": "system", "content": scenario["system"]})
    for m in scenario["messages"]:
        out = {"role": m["role"], "content": m["content"]}
        if "reasoning" in m:
            out["reasoning"] = m["reasoning"]
        if m.get("tool_calls"):
            out["tool_calls"] = [{"function": {"name": c["name"], "arguments": c["arguments"]}} for c in m["tool_calls"]]
        if "tool_call_id" in m:
            out["tool_call_id"] = m["tool_call_id"]
        messages.append(out)
    return messages


def main() -> None:
    import sys

    omni = "--omni" in sys.argv[1:]
    template_path = ROOT / "prompts" / "lfm3-omni-2b_chat_template.jinja" if omni else TEMPLATE
    out_dir = OUT / "omni" if omni else OUT
    env = jinja2.Environment(extensions=[GenerationExtension, "jinja2.ext.loopcontrols"], keep_trailing_newline=True)
    env.policies["json.dumps_function"] = json.dumps
    env.filters["tojson"] = tojson
    env.globals["raise_exception"] = raise_exception
    template = env.from_string(template_path.read_text(encoding="utf-8"))

    out_dir.mkdir(parents=True, exist_ok=True)
    for name, scenario in SCENARIOS:
        if omni:
            # Omni is a non-reasoning model: thinking always off, no reasoning text in old answers
            name = "O" + name[1:]
            scenario = dict(scenario, enable_thinking=False)
            scenario["messages"] = [{k: v for k, v in m.items() if k != "reasoning"} for m in scenario["messages"]]
        tools = [{"type": "function", "function": t} for t in scenario["tools"]]
        prompt = template.render(
            messages=to_template_messages(scenario),
            tools=tools or None,
            bos_token=BOS,
            add_generation_prompt=scenario["add_generation_prompt"],
            enable_thinking=scenario["enable_thinking"],
        )
        (out_dir / f"{name}.json").write_text(json.dumps(scenario, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (out_dir / f"{name}.txt").write_bytes(prompt.encode("utf-8"))
        print(f"{name}: {len(prompt)} chars")


if __name__ == "__main__":
    main()
