#!/usr/bin/env python3
"""Tool-calling eval for the small brain (4d, T-181).

Runs every question of tool_eval.jsonl through the real LFM3 on the host (llama.cpp, temp 0.1,
top_k 50, repeat 1.1) with the app's prompt: system prompt + tool rules + <tools> lines, the
history, the question and the turn hint. Round 1 decides the tool call; when there is one, the
question's mock result is fed back and round 2 gives the answer. No network: results are mocks.

Measures per question: right tool, valid arguments, unnecessary calls, hallucination, time.
  correct tool   the first call is one of expect.tools (no call when expect.tools is empty;
                 ok_none questions may also answer without a tool)
  valid args     required parameters there, expected values match (after the app's aliases)
  unnecessary    a call when none is needed, or a second call of the same tool
  hallucination  fact/current question answered without a tool and without saying "I don't know";
                 an answer after the mock that misses every `must` value; "looked it up" with no tool;
                 a phone action called "set"/"gestellt" although only a card was prepared

usage:
  tool_eval.py --defs legacy --rev <commit>  [--out before.json]   # prompts/schemas as they were
  tool_eval.py --defs repo                   [--out after.json]    # prompts/schemas of this tree
  options: --only de01,en03  --server PATH  --model PATH  --lib DIR  --strict (no repair)
Needs the host runner `h2` (keeps the model loaded, prints prompt/reused tokens and ms).
"""
import argparse, json, os, re, subprocess, sys, time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
BOS, S, E = "<" + "|startoftext|>", "<" + "|im_start|>", "<" + "|im_end|>"
NOW = {"de": dict(weekday="Mittwoch", date="4. November 2026"), "en": dict(weekday="Wednesday", date="November 4, 2026")}
TODAY = "2026-11-04"
CTX = 2048

# ---------------------------------------------------------------- definitions

def git_show(rev, path):
    return subprocess.run(["git", "-C", ROOT, "show", f"{rev}:{path}"], capture_output=True, text=True, check=True).stdout


def kconst(src, name):
    m = re.search(r"const val " + name + r" =\s*((?:\"[^\"]*\"\s*\+?\s*(?:PHONE_DE|PHONE_EN)?\s*)+)", src)
    body = m.group(1)
    text = "".join(re.findall(r"\"([^\"]*)\"", body))
    for ref in ("PHONE_DE", "PHONE_EN"):
        if ref in body:
            text += kconst(src, ref)
    return text


def sections(text):
    """`[name]` sections of a prompt template file (same rules as PromptTemplates.kt)."""
    out, name, buf = {}, None, []
    for line in text.splitlines():
        if line.startswith("//"):
            continue
        m = re.fullmatch(r"\[([a-z_]+)\]\s*", line)
        if m:
            if name: out[name] = "\n".join(buf).strip()
            name, buf = m.group(1), []
        elif name:
            buf.append(line)
    if name: out[name] = "\n".join(buf).strip()
    return out


def load_defs(kind, rev):
    d = {}
    if kind == "legacy":
        spec = json.load(open(os.path.join(HERE, "tools_before.json")))
        src = git_show(rev, "app/src/main/kotlin/app/hain/SystemPromptSource.kt")
        for lang in ("de", "en"):
            L = lang.upper()
            d[lang] = dict(system=git_show(rev, f"core-llm/src/main/assets/prompts/system_full.{lang}.txt"),
                           rules=kconst(src, f"TOOLS_{L}"), nudge=kconst(src, f"TOOL_NUDGE_{L}"), examples="")
        d["tools"], d["aliases"], d["names"] = spec["tools"], spec["aliases"], {}
    else:
        spec = json.load(open(os.path.join(ROOT, "core-tools/src/main/resources/app/hain/tools/tools.json")))
        for lang in ("de", "en"):
            sec = sections(open(os.path.join(ROOT, f"core-llm/src/main/assets/prompts/tools.{lang}.txt")).read())
            d[lang] = dict(system=open(os.path.join(ROOT, f"core-llm/src/main/assets/prompts/system_full.{lang}.txt")).read(),
                           rules=sec["tools"], nudge=sec["nudge"], examples=sec.get("examples", ""))
        d["tools"] = [schema_of(t) for t in spec["tools"]]
        d["aliases"] = {t["name"]: t.get("aliases", {}) for t in spec["tools"]}
        d["names"] = {a: t["name"] for t in spec["tools"] for a in t.get("names", [])}
        d["enums"] = {t["name"]: {p["name"]: p.get("aliases", {}) for p in t["params"] if "enum" in p} for t in spec["tools"]}
    return d


def schema_of(t):
    """tools.json entry -> the JSON line the brain sees (mirrors ToolSpecs.schema)."""
    props, req = {}, []
    for p in t["params"]:
        q = {"type": p["type"]}
        if "enum" in p: q["enum"] = p["enum"]
        q["description"] = p["description"]
        props[p["name"]] = q
        if p.get("required"): req.append(p["name"])
    return {"name": t["name"], "description": t["description"], "parameters": {"type": "object", "properties": props, "required": req}}


def system_prompt(defs, lang):
    t = defs[lang]["system"]
    rules = defs[lang]["rules"] + (("\n\n" + defs[lang]["examples"]) if defs[lang]["examples"] else "")
    v = dict(NOW[lang], time="10:00", timezone="MEZ" if lang == "de" else "CET", city="Köln", emergency_number="112",
             memory_snippets="", user_custom_instructions=rules,
             language_rule="Antworte immer auf Deutsch." if lang == "de" else "Always answer in English only.")
    for k, x in v.items():
        t = t.replace("{" + k + "}", x)
    return re.sub(r"\n{3,}", "\n\n", t).rstrip()


def render(defs, lang, msgs):
    tools = "\n".join(json.dumps(t, ensure_ascii=False) for t in defs["tools"])
    o = BOS + S + "system\n" + system_prompt(defs, lang) + "\n\n<tools>\n" + tools + "\n</tools>" + E + "\n"
    for role, c in msgs:
        if role == "assistant":
            c = "<think></think>" + c
        if role == "tool":
            c = "<tool_response>\n" + c + "\n</tool_response>"
        o += S + role + "\n" + c + E + "\n"
    return o + S + "assistant\n<think></think>"


FOLLOW_STARTS = ("und ", "and ", "what about", "how about", "mach ", "make it", "lieber", "instead", "auch ", "also ", "dann ")
THANKS = re.compile(r"\b(danke|dank|thanks|thank you|thx|super|cool|ok|okay|perfekt|great)\b")


def follow_up(hist, q, lang):
    """Mirrors FollowUp.kt (4d): a short follow-up after a tool answer names the tool again."""
    t = q.strip().lower()
    if len(t.split()) > 7 or not (t.endswith("?") or t.startswith(FOLLOW_STARTS)) or THANKS.search(t):
        return None
    last_user = max((i for i, (r, _) in enumerate(hist) if r == "user"), default=-1)
    tools = [re.search(r"<function=([\w.\-]+)>", c) for r, c in hist[last_user + 1:] if r == "assistant"]
    tools = [m.group(1) for m in tools if m]
    if not tools: return None
    return (f"Das ist eine Folgefrage: rufe {tools[-1]} neu auf, mit den Werten aus dieser Nachricht." if lang == "de"
            else f"This is a follow-up: call {tools[-1]} again with the values from this message.")


def link(q, lang):
    """Mirrors FollowUp.link (4d)."""
    m = re.search(r"https?://[^\s<>\"]+", q)
    if not m: return None
    url = m.group(0).rstrip(".,)!?")
    return f"Die Nachricht enthält einen Link: lies {url} mit read_page." if lang == "de" else f"The message has a link: read {url} with read_page."


def hint(defs, lang, hist=(), q=""):
    h = ("Antworte auf Deutsch. " if lang == "de" else "Answer in English. ") + defs[lang]["nudge"]
    f = (link(q, lang) or follow_up(list(hist), q, lang)) if "enums" in defs else None
    return h + (" " + f if f else "")

# ---------------------------------------------------------------- brain

SERVER = None


def gen(args, prompt, max_tok):
    global SERVER
    if SERVER is None:
        SERVER = subprocess.Popen([args.server, args.model, "0.1", "50", "1.1"], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                  stderr=subprocess.DEVNULL, text=True, errors="replace", env=dict(os.environ, LD_LIBRARY_PATH=args.lib))
        assert SERVER.stdout.readline().strip() == "READY"
    p = os.path.join(args.work, "prompt.txt")
    open(p, "w").write(prompt)
    SERVER.stdin.write(f"{max_tok} {p}\n"); SERVER.stdin.flush()
    head, body = SERVER.stdout.readline(), []
    for line in SERVER.stdout:
        if line.rstrip("\n") == "<<<END>>>": break
        body.append(line)
    m = re.search(r"reused=(\d+)/(\d+) ms=(\d+)", head)
    return "".join(body).rstrip(), int(m.group(3)) / 1000, int(m.group(2))

# ---------------------------------------------------------------- parsing (mirrors XmlToolCallParser)

CALL = re.compile(r"<tool_call>\s*<function=([\w.\-]+)>(.*?)</function>\s*</tool_call>", re.S)
PARAM = re.compile(r"<parameter=([\w.\-]+)>\n?(.*?)\n?</parameter>", re.S)


def parse_strict(out):
    """Before 4d: any broken block -> no calls at all."""
    out = out.split("</think>")[-1]
    calls = [(n, {k: v.strip() for k, v in PARAM.findall(b)}) for n, b in CALL.findall(out)]
    rest = CALL.sub("", out)
    if "<tool_call>" in rest or "</tool_call>" in rest:
        return [], True
    return calls, False


PY_CALL = re.compile(r"([A-Za-z_][\w.\-]*)\((.*?)\)\s*$", re.S)
PY_ARG = re.compile(r"([A-Za-z_]\w*)\s*=\s*(\"(?:[^\"\\]|\\.)*\"|'(?:[^'\\]|\\.)*'|[^,]+)")


def parse_lenient(out):
    """4d (T-183): repair what the small brain breaks; mirrors ToolCallRepair.kt."""
    out = out.split("</think>")[-1]
    out = out.replace("<|tool_call_start|>", "<tool_call>").replace("<|tool_call_end|>", "</tool_call>")
    if out.count("<tool_call>") > out.count("</tool_call>"):
        out = out.rstrip() + "\n</tool_call>"
    calls, broken = [], False
    for block in re.findall(r"<tool_call>(.*?)</tool_call>", out, re.S):
        c = repair_block(block.strip())
        if c: calls.append(c)
        else: broken = True
    return calls, broken and not calls


def repair_block(b):
    m = re.match(r"<function=([\w.\-]+)>", b)
    if m:
        body = b[m.end():]
        body = body.split("</function>")[0]
        args = {}
        for pm in re.finditer(r"<parameter=([\w.\-]+)>(.*?)(?=</parameter>|<parameter=|$)", body, re.S):
            args.setdefault(pm.group(1), pm.group(2).strip())
        return (m.group(1), args)
    if b.startswith("{"):
        try:
            j = json.loads(b)
        except Exception:
            try: j = json.loads(re.sub(r",\s*}", "}", b.replace("'", '"')))
            except Exception: return None
        name = j.get("name") or j.get("function") or j.get("tool")
        a = j.get("arguments", j.get("parameters", j.get("args", {})))
        if isinstance(a, str):
            try: a = json.loads(a)
            except Exception: a = {}
        return (name, {k: str(v) if not isinstance(v, str) else v for k, v in (a or {}).items()}) if name else None
    b = b.strip().strip("[]").strip()
    m = PY_CALL.match(b)
    if m:
        args = {k: v.strip().strip("\"'") for k, v in PY_ARG.findall(m.group(2))}
        return (m.group(1), args)
    return None

# ---------------------------------------------------------------- normalising (mirrors ToolCallFixer.kt)

GLOBAL_NAMES = {}


def normalise(defs, name, args):
    name = defs["names"].get(name.lower(), name)
    al = defs["aliases"].get(name, {})
    out = {}
    for k, v in args.items():
        key = al.get(k.lower(), k.lower())
        out.setdefault(key, v)
    if name == "set_timer" and "enums" in defs:  # mirrors ToolCallFixer.timerSeconds (4d)
        num = lambda *ks: next((float(str(out[k]).replace(",", ".")) for k in ks if k in out and re.fullmatch(r"\s*\d+(?:[.,]\d+)?\s*", str(out[k]))), None)
        m, sec, h = num("minutes"), num("seconds"), num("hours")
        total = int((m or 0) * 60 + (sec or 0) + (h or 0) * 3600) if (m is not None or sec is not None or h is not None) else \
            next((duration(out[k]) for k in ("minutes", "seconds", "duration", "length", "time") if k in out and duration(out[k])), None)
        out = {k: v for k, v in out.items() if k == "label"}
        if total: out["seconds"] = total
    for p, vals in defs.get("enums", {}).get(name, {}).items():
        if p in out and isinstance(out[p], str):
            out[p] = vals.get(out[p].strip().lower(), out[p].strip().lower())
    return name, out


def duration(v):
    s = str(v).strip().lower()
    if re.fullmatch(r"\d+", s): return int(s)
    m = re.fullmatch(r"(\d+(?:[.,]\d+)?)\s*([a-z]+)\.?", s)
    if not m: return None
    n, u = float(m.group(1).replace(",", ".")), m.group(2)
    f = 1 if u in ("s", "sec", "sek", "sekunde", "sekunden", "second", "seconds") else \
        60 if u in ("m", "min", "mins", "minute", "minuten", "minutes") else \
        3600 if u in ("h", "std", "stunde", "stunden", "hour", "hours") else None
    return int(n * f) if f else None


def clock(v):
    s = str(v).strip().lower()
    half = re.search(r"halb\s*(\d{1,2})", s)
    if half: return f"{(int(half.group(1)) - 1) % 24:02d}:30"
    m = re.search(r"(\d{1,2})[:.h](\d{2})", s) or re.fullmatch(r"(\d{1,2})\s*(?:uhr|am|pm|h)?", s)
    if not m: return None
    h, mi = int(m.group(1)), (int(m.group(2)) if m.re.groups >= 2 and m.group(2) else 0)
    if "pm" in s and h < 12: h += 12
    return f"{h:02d}:{mi:02d}" if h < 24 and mi < 60 else None


def start_of(v):
    """What TimeParser.dateTime would read: ISO, or 'tomorrow 15:00' relative to TODAY."""
    s = str(v).lower()
    date = re.search(r"\d{4}-\d{2}-\d{2}", s)
    d = date.group(0) if date else None
    if not d:
        rel = {"heute": 0, "today": 0, "morgen": 1, "tomorrow": 1, "übermorgen": 2, "freitag": 2, "friday": 2}
        for w, n in rel.items():
            if w in s: d = f"2026-11-{4 + n:02d}"
    t = re.search(r"(\d{1,2})[:.](\d{2})", s) or re.search(r"\b(\d{1,2})\s*(uhr|pm|am)\b", s)
    hh = None
    if t:
        hh = int(t.group(1))
        if t.lastindex >= 2 and t.group(2) == "pm" and hh < 12: hh += 12
    return d, hh


def norm(s):
    return str(s).lower().replace("ö", "o").replace("ü", "u").replace("ä", "a").replace("ß", "ss")


def arg_ok(defs, name, args, item):
    schema = next((t for t in defs["tools"] if t["name"] == name), None)
    if schema is None: return False
    for r in schema["parameters"].get("required", []):
        r = "seconds" if (name == "set_timer" and r == "minutes") else r
        if not str(args.get(r, "")).strip(): return False
    for key, want in item["expect"]["args"].items():
        keys = key.split("|")
        k = next((k for k in keys if k in args), None)
        if k is None: return False
        v = args[k]
        if k == "seconds":
            if duration(v) != want: return False
        elif k == "time":
            if clock(v) != want: return False
        elif k == "start":
            d, hh = start_of(v)
            if d != want[0] or hh != int(want[1]): return False
        elif isinstance(want, list):
            if not all(norm(w) in norm(v) for w in want): return False
        elif not any(norm(w) in norm(v) for w in str(want).split("|")):
            return False
    return True

# ---------------------------------------------------------------- one question

DONT_KNOW = re.compile(r"weiß (ich )?(es )?nicht|nicht sicher|kann ich nicht nachsehen|don't know|not sure|can't check|cannot check", re.I)
LOOKED = re.compile(r"nachgeschaut|nachgesehen|recherchiert|abgefragt|i looked it up|i checked|i searched", re.I)
DONE_CLAIM = re.compile(r"\b(ist|sind|wurde|habe|hab)\b[^.!?]{0,80}\b(gestellt|eingestellt|eingetragen|gestartet|gesetzt)\b|\b(is|are|has been|i've|i have)\b[^.!?]{0,60}\b(set|added|started|scheduled)\b(?! up)", re.I)


QUESTION_PREFIXES = ["was ist", "was sind", "wieviel ist", "wie viel ist", "wieviel sind", "wie viel sind", "berechne",
                     "rechne", "what's", "whats", "what is", "how much is", "calculate"]
SPOKEN = [(" prozent von ", "% * "), (" percent of ", "% * "), (" % von ", "% * "), (" % of ", "% * "), (" mal ", " * "),
          (" times ", " * "), (" geteilt durch ", " / "), (" divided by ", " / "), (" plus ", " + "), (" minus ", " - "),
          (" hoch ", " ^ "), (" to the power of ", " ^ ")]


def quick(q):
    """Mirrors CalculatorTool.directAnswer (4d): a spoken sum is answered by code, without the brain."""
    t = " " + q.strip().lower().rstrip("?").strip() + " "
    for p in QUESTION_PREFIXES:
        if t.startswith(f" {p} "):
            t = " " + t[len(p) + 2:]; break
    for w, sign in SPOKEN:
        t = t.replace(w, sign)
    t = t.strip()
    if re.fullmatch(r"[\d\s.,+\-*/^%()]+", t) and re.search(r"\d", t) and re.search(r"[+\-*/^%]", t):
        return t
    return None


CLOCK_Q = {"wie spät ist es", "wie spaet ist es", "wie spät is es", "wieviel uhr ist es", "wie viel uhr ist es", "uhrzeit",
           "wie spät", "what time is it", "whats the time", "what's the time", "what is the time", "time",
           "welcher tag ist heute", "welcher tag ist", "welches datum ist heute", "welches datum haben wir",
           "der wievielte ist heute", "was ist heute für ein tag", "datum", "what day is it", "what day is it today",
           "what's the date", "whats the date", "what is the date", "what's today's date", "what is today's date"}


def clock_q(q):
    """Mirrors ClockAnswer (4d): time/date questions answered by code."""
    return re.sub(r"\s+", " ", re.sub(r"[?!.,]+", "", q.strip().lower())).strip() in CLOCK_Q


def ask(args, defs, item):
    lang = item["lang"]
    if "enums" in defs and not item.get("history") and clock_q(item["q"]):
        ok = not item["expect"]["tools"]
        return dict(id=item["id"], lang=lang, kind=item["kind"], q=item["q"], calls=[], broken=False, tool_ok=ok, args_ok=None,
                    unnecessary=0, halluc=False, why="", cites=False, quick=True, seconds=0.0, model_s=0.0, prompt_tokens=0,
                    raw="(clock answer, no brain)", answer="(code: time/date from the phone clock)")
    if "enums" in defs and not item.get("history") and quick(item["q"]):
        e = quick(item["q"])
        ok = "calculate" in item["expect"]["tools"]
        return dict(id=item["id"], lang=lang, kind=item["kind"], q=item["q"], calls=[["calculate", {"expression": e}]], broken=False,
                    tool_ok=ok, args_ok=ok, unnecessary=0 if ok else 1, halluc=False, why="", cites=False, quick=True,
                    seconds=0.0, model_s=0.0, prompt_tokens=0, raw="(quick answer, no brain)", answer=f"{e} = (code)")
    hist = [(r, c) for r, c in item.get("history", [])]
    msgs = hist + [("user", item["q"]), ("system", hint(defs, lang, hist, item["q"]))]
    t0 = time.time()
    out1, s1, ptok = gen(args, render(defs, lang, msgs), 160)
    parse = parse_strict if args.strict else parse_lenient
    calls, broken = parse(out1)
    calls = [normalise(defs, n, a) for n, a in calls]
    # duplicates (same tool, same arguments) run once
    seen, uniq = set(), []
    for n, a in calls:
        k = (n, json.dumps(a, sort_keys=True))
        if args.strict or k not in seen:
            seen.add(k); uniq.append((n, a))
    calls = uniq
    expect = item["expect"]["tools"]
    names = [n for n, _ in calls]
    if not expect:
        tool_ok = not calls
    else:
        tool_ok = (bool(calls) and names[0] in expect) or (not calls and item["expect"].get("ok_none", False))
    args_ok = None
    if expect and calls and names[0] in expect:
        args_ok = arg_ok(defs, calls[0][0], calls[0][1], item)
    unnecessary = (len(calls) if not expect else max(0, len(calls) - 1)) if calls else 0
    answer, s2 = out1.split("</think>")[-1].strip(), 0.0
    if calls:
        tool_msgs = []
        for n, a in calls:
            tool_msgs.append(("tool", mock(item, n, a, lang, defs)))
        call_txt = "\n\n".join("<tool_call>\n<function=%s>\n%s</function>\n</tool_call>" % (n, "".join(
            "<parameter=%s>\n%s\n</parameter>\n" % (k, v) for k, v in a.items())) for n, a in calls)
        msgs2 = hist + [("user", item["q"]), ("system", hint(defs, lang, hist, item["q"])), ("assistant", call_txt)] + tool_msgs
        answer, s2, _ = gen(args, render(defs, lang, msgs2), 220)
        answer = answer.split("</think>")[-1].strip()
        answer = re.sub(r"<tool_call>.*?(</tool_call>|$)", "", answer, flags=re.S).strip()
    halluc, why = judge(item, calls, answer)
    cites = bool(re.search(r"\[\d\]", answer))
    return dict(id=item["id"], lang=lang, kind=item["kind"], q=item["q"], calls=[[n, a] for n, a in calls], broken=broken,
                tool_ok=tool_ok, args_ok=args_ok, unnecessary=unnecessary, halluc=halluc, why=why, cites=cites,
                seconds=round(time.time() - t0, 1), model_s=round(s1 + s2, 1), prompt_tokens=ptok, raw=out1[:300], answer=answer[:400])


def judge(item, calls, answer):
    kind = item["kind"]
    halluc, why = False, ""
    if kind in ("fact", "current") and not calls and not DONT_KNOW.search(answer):
        halluc, why = True, "answered from memory"
    elif calls and item.get("must") and kind in ("fact", "current", "calc") and not any(m.lower() in answer.lower() for m in item["must"]):
        halluc, why = True, "missed the tool result"
    elif not calls and LOOKED.search(answer):
        halluc, why = True, "claims a lookup"
    elif kind == "action" and calls and DONE_CLAIM.search(answer):
        halluc, why = True, "says done before the tap"
    elif kind == "time" and item.get("must") and not any(m.lower() in answer.lower() for m in item["must"]):
        halluc, why = True, "wrong date/time"
    return halluc, why


def mock(item, name, a, lang, defs=None):
    if item.get("mock") and name in item["expect"]["tools"]:
        return item["mock"]
    if name in ("set_alarm", "set_timer", "add_calendar_event", "navigate"):
        f = {k: str(v) for k, v in a.items()}
        if defs is None or "enums" not in defs:  # before 4d
            return json.dumps(dict({"prepared": name}, **f, status="waiting_for_tap",
                                   hint="Nothing is done yet, so never say it is set or added. A card under your answer shows it. "
                                        "In one short sentence say what you prepared and that a tap on the card's button does it."), ensure_ascii=False)
        return json.dumps(dict({"prepared": name}, **f, status="waiting_for_tap", say=say(name, label(name, a, lang), lang),
                               hint="Nothing is done yet. Answer with the sentence in say; never say it is set or added."), ensure_ascii=False)
    return json.dumps({"error": "not_found", "hint": "Nothing found. Answer from what you know and say you are not sure."})


def label(name, a, lang):
    """The card label the app makes (PhoneTools.kt)."""
    de = lang == "de"
    if name == "set_alarm":
        return clock(a.get("time", "")) or str(a.get("time", ""))
    if name == "set_timer":
        sec = duration(a.get("seconds", 0)) or 0
        h, m, x = sec // 3600, sec % 3600 // 60, sec % 60
        parts = ([f"{h} {'Std.' if de else 'h'}"] if h else []) + ([f"{m} {'Min.' if de else 'min'}"] if m else []) + ([f"{x} {'Sek.' if de else 's'}"] if x else [])
        return " ".join(parts)
    if name == "add_calendar_event":
        d, hh = start_of(a.get("start", ""))
        return f"{a.get('title', '')} · {d or ''}" + (f" {hh:02d}:00" if hh is not None else "")
    return str(a.get("destination", ""))


def say(kind, lbl, lang):
    de = lang == "de"
    what = {"set_alarm": ("Der Wecker für ", "The alarm for "), "set_timer": ("Der Timer über ", "The "),
            "add_calendar_event": ("Der Termin ", "The event "), "navigate": ("Die Route zu ", "The route to ")}[kind]
    w = (what[0] + lbl) if de else (what[1] + lbl + (" timer" if kind == "set_timer" else ""))
    return f"{w} ist vorbereitet – tipp auf der Karte auf den Knopf." if de else f"{w} is ready – tap the button on the card."


# ---------------------------------------------------------------- report


def summary(rows):
    n = len(rows)
    pct = lambda x, d: round(100 * x / d, 1) if d else 0.0
    tool = sum(r["tool_ok"] for r in rows)
    a = [r for r in rows if r["args_ok"] is not None]
    facts = [r for r in rows if r["kind"] in ("fact", "current")]
    return {
        "questions": n,
        "correct_tool_pct": pct(tool, n),
        "valid_args_pct": pct(sum(r["args_ok"] for r in a), len(a)),
        "unnecessary_calls": sum(r["unnecessary"] for r in rows),
        "broken_calls": sum(r["broken"] for r in rows),
        "hallucinations": sum(r["halluc"] for r in rows),
        "halluc_fact_questions": sum(r["halluc"] for r in facts),
        "fact_questions": len(facts),
        "cites_pct_after_tool": pct(sum(r["cites"] for r in rows if r["calls"] and r["kind"] in ("fact", "current")),
                                     sum(1 for r in rows if r["calls"] and r["kind"] in ("fact", "current"))),
        "avg_seconds": round(sum(r["seconds"] for r in rows) / n, 1) if n else 0,
        "max_prompt_tokens_round1": max((r["prompt_tokens"] for r in rows), default=0),
        "quick_answers": sum(1 for r in rows if r.get("quick")),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--defs", choices=["legacy", "repo"], default="repo")
    ap.add_argument("--rev", default="HEAD")
    ap.add_argument("--out", default=None)
    ap.add_argument("--only", default="")
    ap.add_argument("--strict", action="store_true")
    ap.add_argument("--server", default="/workspace/work/hain-lang-host/h2")
    ap.add_argument("--model", default="/workspace/work/models/lfm3.gguf")
    ap.add_argument("--lib", default="/workspace/work/hostllama/b/bin")
    ap.add_argument("--work", default="/tmp")
    ap.add_argument("--rescore", default=None, help="recompute the checks of a saved result file")
    args = ap.parse_args()
    if args.defs == "legacy": args.strict = True
    items_by_id = {i["id"]: i for i in (json.loads(l) for l in open(os.path.join(HERE, "tool_eval.jsonl")) if l.strip())}
    if args.rescore:
        data = json.load(open(args.rescore))
        defs = load_defs(args.defs, args.rev)
        for r in data["rows"]:
            if r.get("quick"):
                continue
            it = items_by_id[r["id"]]
            calls = [(n, a) for n, a in r["calls"]]
            if calls and calls[0][0] in it["expect"]["tools"]:
                r["args_ok"] = arg_ok(defs, calls[0][0], calls[0][1], it)
            r["halluc"], r["why"] = judge(it, calls, r["answer"])
        data["summary"] = summary(data["rows"])
        json.dump(data, open(args.rescore, "w"), ensure_ascii=False, indent=1)
        print(json.dumps(data["summary"], indent=1))
        return
    defs = load_defs(args.defs, args.rev)
    items = [json.loads(l) for l in open(os.path.join(HERE, "tool_eval.jsonl")) if l.strip()]
    if args.only: items = [i for i in items if i["id"] in args.only.split(",")]
    rows = []
    for it in items:
        r = ask(args, defs, it)
        rows.append(r)
        flag = ("OK " if r["tool_ok"] else "XX ") + ("" if r["args_ok"] in (None, True) else "ARGS ") + ("HALL(" + r["why"] + ") " if r["halluc"] else "")
        print(f"{r['id']} {flag}{r['calls']} {r['seconds']}s | {r['answer'][:110]!r}", flush=True)
    s = summary(rows)
    print(json.dumps(s, indent=1))
    if args.out:
        json.dump({"summary": s, "rows": rows}, open(args.out, "w"), ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
