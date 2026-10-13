"""Построитель содержимого ПЗ: блоки для build.js с автоматической нумерацией
рисунков, таблиц и формул в пределах раздела (СТП 01-2024)."""
import json
import re
from pathlib import Path

KP = Path("/home/claude/kp")
D = str(KP / "diagrams") + "/"
S = str(KP / "screenshots") + "/"

body = []
front = []
_sec = {"n": "0", "fig": 0, "tbl": 0, "frm": 0}
refs = {"fig": {}, "tbl": {}}


def B(x):
    body.append(x)


def h1(num, text):
    _sec.update(n=num, fig=0, tbl=0, frm=0)
    B({"t": "h1", "num": num, "text": text})


def h1c(text, sec=None):
    if sec:
        _sec.update(n=sec, fig=0, tbl=0, frm=0)
    B({"t": "h1", "text": text})


def h2(num, text):
    B({"t": "h2", "num": num, "text": text})


def p(text):
    B({"t": "p", "text": text})


def pl(text):
    B({"t": "pl", "text": text})


def li(*items):
    """Перечисление: элементы через тире; последний с точкой, остальные с точкой с запятой."""
    for i, t in enumerate(items):
        t = t.rstrip(";.")
        B({"t": "li", "text": t + ("." if i == len(items) - 1 else ";")})


def ol(*items):
    for i, t in enumerate(items, 1):
        B({"t": "ol", "n": i, "text": t})


def next_fig():
    return f'{_sec["n"]}.{_sec["fig"] + 1}'


def next_fig2():
    return f'{_sec["n"]}.{_sec["fig"] + 2}'


def next_tbl():
    return f'{_sec["n"]}.{_sec["tbl"] + 1}'


def fig(img, title, w=None, landscape=False, key=None):
    """Рисунок с подписью; на альбомной странице по умолчанию — на всю ширину поля (25,7 см)."""
    if w is None:
        w = 25.7 if landscape else 16.5
    _sec["fig"] += 1
    n = f'{_sec["n"]}.{_sec["fig"]}'
    if key:
        refs["fig"][key] = n
    path = img if img.startswith("/") else (S + img if img[:2].isdigit() else D + img)
    B({"t": "fig", "img": path, "caption": f"Рисунок {n} – {title}", "w": w, "landscape": landscape})
    return n


def tbl(title, cols, header, rows, align=None, key=None):
    _sec["tbl"] += 1
    n = f'{_sec["n"]}.{_sec["tbl"]}'
    if key:
        refs["tbl"][key] = n
    B({"t": "tbl", "caption": f"Таблица {n} – {title}", "cols": cols, "header": header,
       "rows": [[str(c) for c in r] for r in rows], "align": align or [], "num": n, "title": title})
    return n


def formula(text, keep_next=True):
    _sec["frm"] += 1
    n = f'({_sec["n"]}.{_sec["frm"]})'
    B({"t": "formula", "text": text, "num": n, "keepNext": keep_next})
    return n


def where(text):
    B({"t": "where", "text": text})


def code(text):
    B({"t": "code", "text": text})


def appendix(letter, title, kind="обязательное"):
    _sec.update(n=letter, fig=0, tbl=0, frm=0)
    B({"t": "appendix", "letter": letter, "title": title, "kind": kind})


def listing(letter, files):
    B({"t": "listing", "letter": letter, "files": files})


def fullpage(imgs):
    B({"t": "fullpage", "imgs": imgs})


def java_source(path, drop_imports=True):
    text = Path(path).read_text(encoding="utf8")
    if drop_imports:
        lines = [l for l in text.split("\n") if not l.startswith("import ")]
        text = re.sub(r"\n{3,}", "\n\n", "\n".join(lines))
    return text.strip("\n")


def _resolve(obj):
    """Подстановка ссылок {fig:ключ} и {tbl:ключ}, определённых в любом месте документа."""
    if isinstance(obj, str):
        def sub(m):
            kind, key = m.group(1), m.group(2)
            if key not in refs[kind]:
                raise KeyError(f"нет ссылки {kind}:{key}")
            return refs[kind][key]
        return re.sub(r"\{(fig|tbl):([\w-]+)\}", sub, obj)
    if isinstance(obj, list):
        return [_resolve(x) for x in obj]
    if isinstance(obj, dict):
        return {k: _resolve(v) for k, v in obj.items()}
    return obj


CITE_RE = re.compile(r"\[(\d+(?:, \d+)*)\]")
TEXT_BLOCKS = ("p", "pl", "pc", "li", "ol", "where")


def _renumber_sources():
    """Нумерация источников в порядке первого упоминания в тексте (СТП 01-2024): список
    переупорядочивается, ссылки в тексте заменяются новыми номерами."""
    bi = next(i for i, b in enumerate(body) if b["t"] == "h1" and b.get("text") == "СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ")
    j = bi + 1
    entries = {}
    while j < len(body) and body[j]["t"] == "where" and re.match(r"\[\d+\] ", body[j]["text"]):
        m = re.match(r"\[(\d+)\] (.*)", body[j]["text"], re.S)
        entries[int(m.group(1))] = m.group(2)
        j += 1
    text_blocks = [b for b in front + body[:bi + 1] + body[j:] if b["t"] in TEXT_BLOCKS]
    order = []
    for b in text_blocks:
        for m in CITE_RE.finditer(b["text"]):
            for n in m.group(1).split(", "):
                n = int(n)
                if n not in entries:
                    raise KeyError(f"ссылка [{n}] на отсутствующий источник")
                if n not in order:
                    order.append(n)
    uncited = [n for n in entries if n not in order]
    if uncited:
        print("источники без ссылок в тексте:", uncited)
    order += uncited
    new_num = {old: i + 1 for i, old in enumerate(order)}
    for b in text_blocks:
        b["text"] = CITE_RE.sub(lambda m: "[" + ", ".join(
            str(x) for x in sorted(new_num[int(n)] for n in m.group(1).split(", "))) + "]", b["text"])
    for k, old in enumerate(order):
        body[bi + 1 + k]["text"] = f"[{k + 1}] {entries[old]}"


def save(title, out=KP / "docs" / "content.json"):
    global body, front
    _renumber_sources()
    body[:] = _resolve(body)
    front[:] = _resolve(front)
    Path(out).write_text(json.dumps({"title": title, "front": front, "body": body}, ensure_ascii=False, indent=1),
                         encoding="utf8")
