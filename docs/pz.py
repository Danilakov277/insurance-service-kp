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
_sec = {"n": "0", "fig": 0, "tbl": 0, "frm": 0, "h2": None, "pt": 0}
refs = {"fig": {}, "tbl": {}}


def B(x):
    body.append(x)


def h1(num, text):
    _sec.update(n=num, fig=0, tbl=0, frm=0, h2=None, pt=0)
    B({"t": "h1", "num": num, "text": text})


def h1c(text, sec=None):
    if sec:
        _sec.update(n=sec, fig=0, tbl=0, frm=0)
    B({"t": "h1", "text": text})


def h2(num, text):
    _sec.update(h2=num, pt=0)
    B({"t": "h2", "num": num, "text": text})


def pt(text):
    """Пункт подраздела (раздела, если подразделов нет) с порядковым номером (СТП 01-2024, п. 2.2.3–2.2.4)."""
    _sec["pt"] += 1
    base = _sec["h2"] or _sec["n"]
    num = f'{base}.{_sec["pt"]}'
    B({"t": "pt", "num": num, "text": text})
    return num


def src(text):
    """Запись списка использованных источников (ГОСТ 7.1-2003, СТП 01-2024, п. 2.8.6)."""
    B({"t": "src", "text": text})


def ola(*items):
    """Перечисление с буквами (п. 2.3.8) – когда на элементы есть ссылки."""
    letters = "абвгдежиклмнпрстуфхцшщэюя"
    for i, t in enumerate(items):
        t = t.rstrip(";.")
        B({"t": "ola", "letter": letters[i], "text": t + ("." if i == len(items) - 1 else ";")})


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
    block = {"t": "fig", "img": path, "caption": f"Рисунок {n} – {title}", "w": w, "landscape": landscape}
    if landscape:
        block["imgRot"] = _rotated(path)
    B(block)
    return n


def _rotated(path):
    """Копия изображения, повёрнутая на 90° против часовой стрелки (верх рисунка — к корешку)."""
    from PIL import Image
    out = KP / "docs" / "_rot" / Path(path).name
    out.parent.mkdir(exist_ok=True)
    if not out.exists() or out.stat().st_mtime < Path(path).stat().st_mtime:
        Image.open(path).rotate(90, expand=True).save(out)
    return str(out)


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
    B({"t": "where", "text": text.strip()})


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
TEXT_BLOCKS = ("p", "pl", "pc", "li", "ol", "ola", "pt", "where")


def _renumber_sources():
    """Нумерация источников в порядке первого упоминания в тексте (СТП 01-2024): список
    переупорядочивается, ссылки в тексте заменяются новыми номерами."""
    bi = next(i for i, b in enumerate(body) if b["t"] == "h1" and b.get("text") == "СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ")
    j = bi + 1
    entries = {}
    while j < len(body) and body[j]["t"] == "src":
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


NBSP = "\u00a0"
COEF_RE = re.compile(r"(?<!\w)К(бм|в|с|ф|т)(?!\w)")
LATIN1_RE = re.compile(r"(?<![\w\-+/\\@#{}^])(?<!\w\.)([A-Za-z])(?![\w\-/{}]|\.\w)")
REF_RE = re.compile(r"\b(рисун\w*|таблиц\w*|формул\w*|выражени\w*|приложени\w*|подраздел\w*|раздел\w*|"
                    r"пункт\w*|ГОСТ|СТП|глав\w*) (?=\d|[А-ЯA-Z]\b)", re.I)
CAMEL_RE = re.compile(r"\b[A-Za-z][a-z0-9]+(?:[A-Z][a-z0-9]*){1,}\b")
UNIT_RE = re.compile(r"(?<=\d) (?=(?:BYN|%|мс|с\b|ч\b|дн|дней|дня|лет|года|год|месяц|ГБ|ГГц|байт|строк|клиент|запрос|поток|соединен))")


def typo(t, symbols=True):
    """Типографика СТП: индексы коэффициентов, курсив латинских символов (п. 2.4.2), неразрывные
    пробелы в числах, перед единицами, ссылками на источники (п. 2.8.2) и в инициалах."""
    if symbols:
        t = COEF_RE.sub(r"К_{\1}", t)
        t = LATIN1_RE.sub(r"__\1__", t)
    t = re.sub(r" \[(\d)", NBSP + r"[\1", t)
    t = REF_RE.sub(lambda m: m.group(1) + NBSP, t)
    t = re.sub(r"(?<=\d)–(?=\d)", "–\u2060", t)     # диапазон и номер стандарта не разрываются
    t = re.sub(r"(?<=\d) (?=\d{3}(?!\d))", NBSP, t)
    t = UNIT_RE.sub(NBSP, t)
    t = re.sub(r"([А-ЯЁ][а-яё]+) ([А-ЯЁ])\.\s?([А-ЯЁ])\.", r"\1" + NBSP + r"\2." + NBSP + r"\3.", t)
    t = re.sub(r"(?<![\w.])([А-ЯЁ])\.([А-ЯЁ])\. ([А-ЯЁ][а-яё]+)", r"\1." + NBSP + r"\2." + NBSP + r"\3", t)
    return t


def _typography(blocks):
    for b in blocks:
        tt = b["t"]
        if tt in ("p", "pl", "pc", "li", "ol", "ola", "pt", "where", "formula"):
            b["text"] = typo(b["text"])
        elif tt == "src":
            b["text"] = typo(b["text"], symbols=False).replace(" : ", NBSP + ": ").replace(" / ", NBSP + "/ ")
        elif tt == "fig":
            b["caption"] = typo(b["caption"])
        elif tt == "tbl":
            b["caption"] = typo(b["caption"])
            b["header"] = [typo(x) for x in b["header"]]
            b["rows"] = [[x if x == "^" else _soft_breaks(typo(x)) for x in r] for r in b["rows"]]


def _soft_breaks(t):
    """Длинные идентификаторы в узких графах таблиц переносятся по границам слов, а не посередине."""
    return CAMEL_RE.sub(lambda m: re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "\u200b", m.group(0))
                        if len(m.group(0)) > 14 else m.group(0), t)


_FONT = None


def _text_width_twips(text, size_pt=14):
    """Ширина строки шрифтом Times New Roman (метрически совместимый Liberation Serif), твипы."""
    global _FONT
    from PIL import ImageFont
    if _FONT is None:
        _FONT = ImageFont.truetype("/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf", 1000)
    return round(_FONT.getlength(text) / 1000 * size_pt * 20)


def _table_caption_hang(blocks):
    """Вторая строка названия таблицы – по началу текста названия (прил. Л)."""
    for b in blocks:
        if b["t"] == "tbl" and not b.get("cont"):
            prefix = b["caption"].split(" – ")[0] + " – "
            b["hang"] = _text_width_twips(prefix.replace(NBSP, " "))


def save(title, out=KP / "docs" / "content.json"):
    global body, front
    _renumber_sources()
    body[:] = _resolve(body)
    front[:] = _resolve(front)
    _typography(front)
    _typography(body)
    _table_caption_hang(body)
    Path(out).write_text(json.dumps({"title": title, "front": front, "body": body}, ensure_ascii=False, indent=1),
                         encoding="utf8")
