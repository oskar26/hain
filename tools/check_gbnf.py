#!/usr/bin/env python3
"""check_gbnf.py (T-131) — validates core-cards/src/main/assets/card.gbnf: llama.cpp-style syntax check, then expands it (non-recursive) into a regex
and checks good and bad sample cards. Stand-in for llama-gbnf-validator (no C++ compiler here)."""
import re, sys
path = sys.argv[1] if len(sys.argv) > 1 else "core-cards/src/main/assets/card.gbnf"
src = open(path, encoding="utf-8").read()
rules = {}
cur = None
for raw in src.splitlines():
    line = re.sub(r'(?<!\\)#.*$', '', raw) if not re.search(r'"[^"]*#', raw) else raw.split(' #')[0]
    if not line.strip(): continue
    m = re.match(r'^([a-zA-Z0-9-]+)\s*::=\s*(.*)$', line)
    if m:
        cur = m.group(1); assert cur not in rules, f"duplicate {cur}"; rules[cur] = m.group(2)
    else:
        assert cur and line.startswith(' '), f"bad line: {raw}"; rules[cur] += ' ' + line.strip()
assert 'root' in rules
def tokenize(body):
    toks=[]; i=0
    while i < len(body):
        c=body[i]
        if c.isspace(): i+=1; continue
        if c=='"':
            j=i+1; s=''
            while body[j]!='"':
                if body[j]=='\\': s+={'n':'\n','"':'"','\\':'\\','t':'\t'}[body[j+1]]; j+=2
                else: s+=body[j]; j+=1
            toks.append(('lit',s)); i=j+1
        elif c=='[':
            j=i+1
            while body[j]!=']':
                j+= 2 if body[j]=='\\' else 1
            toks.append(('cls',body[i:j+1])); i=j+1
        elif c in '()|?*+': toks.append((c,c)); i+=1
        elif c=='{':
            j=body.index('}',i); toks.append(('rep',body[i:j+1])); i=j+1
        else:
            m=re.match(r'[a-zA-Z0-9-]+',body[i:]); assert m, f"bad token at {body[i:i+10]}"
            assert m.group(0) in rules, f"undefined rule {m.group(0)}"
            toks.append(('ref',m.group(0))); i+=len(m.group(0))
    return toks
def cls_to_re(c):
    inner=c[1:-1]; out=''; i=0
    while i<len(inner):
        if inner[i]=='\\': out+={'n':'\\n','"':'"','\\':'\\\\',']':'\\]','[':'\\[','t':'\\t'}.get(inner[i+1],'\\'+inner[i+1]); i+=2
        elif inner[i] in '[|': out+='\\'+inner[i]; i+=1
        else: out+=inner[i]; i+=1
    return '['+out+']'
memo={}
def expand(name, depth=0):
    assert depth<50, "recursion"
    if name in memo: return memo[name]
    toks=tokenize(rules[name]); pos=[0]
    def alt():
        parts=[seq()]
        while pos[0]<len(toks) and toks[pos[0]][0]=='|': pos[0]+=1; parts.append(seq())
        return '(?:'+'|'.join(parts)+')'
    def seq():
        out=''
        while pos[0]<len(toks) and toks[pos[0]][0] not in '|)':
            t,v=toks[pos[0]]; pos[0]+=1
            if t=='lit': a='(?:'+re.escape(v)+')'
            elif t=='cls': a=cls_to_re(v)
            elif t=='ref': a='(?:'+expand(v,depth+1)+')'
            elif t=='(':
                a=alt(); assert toks[pos[0]][0]==')'; pos[0]+=1
            else: raise AssertionError(f"unexpected {v}")
            while pos[0]<len(toks) and toks[pos[0]][0] in ('?','*','+','rep'):
                a='(?:'+a+')'+toks[pos[0]][1]; pos[0]+=1
            out+=a
        return out
    r=alt(); assert pos[0]==len(toks); memo[name]=r; return r
root=re.compile(expand('root'), re.S)
good=[
 'Card title="Einkaufsliste"\nChecklist\n- Tomaten\n- Basilikum [x]\nButton label="In Notizen kopieren" action=copy\n```',
 'List\n- eins\n- zwei\n```',
 'Card title="Wetter"\nTable\n| Stadt | Grad |\n| Köln | 9 |\n| Bonn | 10 |\n```',
 'Progress value=40 label="Vokabeln"\n```',
 'Progress value=100\nButton label="Mehr" action=ask:"Erklär das genauer"\nButton label="Teilen" action=share\n```',
 'Card title="Schlaf"\nChart type=bar unit="h"\n- Mo: 7.5\n- Di: 6\n- Mi: 8\n```',
 'Chart type=line title="Temperatur"\n- 9 Uhr: -2\n- 12 Uhr: 4\nList\n- warm anziehen\n```',
]
bad=[
 'Hallo\n```', 'List\n```', 'Progress value=140\n```', 'Button label="x" action=open\n```',
 'Table\n| nur Kopf |\n```', 'Card title=""\nList\n- a\n```', 'List\n- a\n',
 'Chart type=pie\n- a: 1\n```', 'Chart type=bar\n```', 'Chart type=bar\n- Mo: viel\n```',
]
ok=True
for g in good:
    if not root.fullmatch(g): print("SHOULD MATCH:", repr(g)); ok=False
for b in bad:
    if root.fullmatch(b): print("SHOULD FAIL:", repr(b)); ok=False
print("rules:", len(rules), "ok" if ok else "FAILED"); sys.exit(0 if ok else 1)
