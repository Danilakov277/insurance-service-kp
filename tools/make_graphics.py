"""Графическая часть курсового проекта и ведомость документов (формат А4, PDF + PNG).

Рамки и основные надписи строятся по форматкам ЕСКД из шаблонов Altium (tools/templates,
архив Altium_Templates): GOST_A4_1 — первый лист (основная надпись по форме 1, 185×55 мм),
GOST_A4_2 — последующие листы (форма 2а, 185×15 мм). Из шаблона берутся все линии рамки,
основной надписи, дополнительных граф на левом поле и графы 26 (обозначение, повёрнутое на 180°),
а также подписи граф; значения граф (обозначение, наименование, фамилии, листы) подставляются.
Формат А4 по ГОСТ 2.301 и 2.104 располагается только вертикально (основная надпись вдоль короткой
стороны), поэтому все листы — книжные. Шрифт — GOST Common из того же архива.

  Чертёж 1  ГУИР.425707.015 Д2  IDEF0-модель процессов предметной области;
  Чертёж 2  ГУИР.425707.015 Д3  схема алгоритма расчёта страховой выплаты;
  Плакаты   ГУИР.425707.015 Д5  листы 1–3, материал вписан в рамку листа;
  Ведомость БГУИР КП 1-40 01 02-08 015 Д1 (форма 2, последний лист ПЗ).

Запуск: python3 make_graphics.py [число_страниц_ПЗ] [ved d1 d2 pl]
"""
import base64
import re
import sys
from html import escape
from pathlib import Path

from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).parent))
import flowchart  # noqa: E402
from cfb import CFB, altium_records  # noqa: E402

TOOLS = Path(__file__).parent
KP = TOOLS.parent
DIAG = KP / "diagrams"
SHOTS = KP / "screenshots"
OUT = KP / "graphics"
TPL = TOOLS / "templates"

FONT_B64 = base64.b64encode((TPL / "GOST_Common.ttf").read_bytes()).decode()
CSS_FONT = (f"@font-face{{font-family:GOSTC;src:url(data:font/ttf;base64,{FONT_B64})}}"
            ".g{font-family:GOSTC,'Liberation Sans',sans-serif;font-style:italic}"
            "svg text{font-family:'Times New Roman','Liberation Serif',serif}")

# ----------------------------------------------------------------- общие данные
CODE_PZ = "БГУИР КП 1-40 01 02-08 015 ПЗ"
CODE_VED = "БГУИР КП 1-40 01 02-08 015 Д1"
CODE_IDEF0 = "ГУИР.425707.015 Д2"
CODE_ALG = "ГУИР.425707.015 Д3"
CODE_POSTERS = "ГУИР.425707.015 Д5"
TOPIC = "Программное средство для\nподдержки процесса оказания\nстраховых услуг населению"
ORG = ["Кафедра ЭИ,", "группа 414351"]
NAMES = {"Author": "Балащенко", "CheckedBy": "Сторожев"}
LIT = "Т"
POSTERS = [
    ("Диаграмма классов программного средства", "Лист 1 – Диаграмма классов ПС"),
    ("Модели представления программного средства", "Лист 2 – Модели представления ПС"),
    ("Интерфейс программного средства", "Лист 3 – Интерфейс ПС"),
]
# наименование в основной надписи: строки наименования и (ниже) вид документа
IDEF0_TITLE = ["Программное средство", "поддержки страховых услуг", "IDEF0-модель процессов", "предметной области"]
ALG_TITLE = ["Программное средство", "поддержки страховых услуг", "Схема алгоритма расчёта", "страховой выплаты"]
POSTERS_TITLE = ["Плакаты, отражающие", "результаты курсового", "проектирования"]

W, H = 210, 297                 # А4, книжная
THICK, THIN = 0.6, 0.25         # основная и тонкая линии, мм
U = 0.254                       # единица координат Altium (10 mil), мм


def b64(path):
    return "data:image/png;base64," + base64.b64encode(Path(path).read_bytes()).decode()


# ----------------------------------------------------------------- форматки из шаблонов Altium
def load_template(name):
    """Линии и надписи форматки в мм, ось y — сверху вниз."""
    recs = altium_records(CFB(str(TPL / name)).stream("FileHeader"))

    def v(r, k):
        return (int(r.get(k, b"0")) + int(r.get(k + "_FRAC", b"0")) / 100000) * U

    fonts = {}
    head = recs[1]
    for i in range(1, 20):
        if f"SIZE{i}" in head:
            fonts[str(i)] = int(head[f"SIZE{i}"])
    lines, texts = [], []
    for r in recs[2:]:
        t = r.get("RECORD", b"").decode()
        if t == "13":
            lines.append((v(r, "LOCATION.X"), H - v(r, "LOCATION.Y"), v(r, "CORNER.X"), H - v(r, "CORNER.Y"),
                          THICK if int(r.get("LINEWIDTH", b"0")) >= 2 else THIN))
        elif t == "4":
            text = r.get("TEXT", b"").decode("cp1251")
            if text:
                texts.append(dict(x=v(r, "LOCATION.X"), y=H - v(r, "LOCATION.Y"), text=text,
                                  size=fonts.get(r.get("FONTID", b"2").decode(), 14),
                                  orient=int(r.get("ORIENTATION", b"0")), just=int(r.get("JUSTIFICATION", b"0"))))
    return lines, texts


FORM1 = load_template("GOST_A4_1.SchDot")
FORM2A = load_template("GOST_A4_2.SchDot")


class Svg:
    def __init__(self):
        self.s = []

    def line(self, x1, y1, x2, y2, w=THICK, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.s.append(f'<line x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}" stroke="#000" '
                      f'stroke-width="{w}" stroke-linecap="square"{d}/>')

    def rect(self, x, y, w, h, sw=THICK, fill="none"):
        self.s.append(f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" fill="{fill}" '
                      f'stroke="{"#000" if sw else "none"}" stroke-width="{sw}"/>')

    def text(self, x, y, t, size=3.5, anchor="start", rotate=None, underline=False, baseline=None):
        t = t.replace("«", '"').replace("»", '"')      # в шрифте GOST Common нет знаков «»
        tr = f' transform="rotate({rotate} {x:.2f} {y:.2f})"' if rotate else ""
        deco = ' text-decoration="underline"' if underline else ""
        bl = f' dominant-baseline="{baseline}"' if baseline else ""
        self.s.append(f'<text class="g" x="{x:.2f}" y="{y:.2f}" font-size="{size:.2f}" '
                      f'text-anchor="{anchor}"{tr}{deco}{bl}>{escape(t)}</text>')

    def raw(self, x):
        self.s.append(x)

    def __str__(self):
        return "".join(self.s)


# Altium: JUSTIFICATION 0..8 = (низ, середина, верх) × (лево, центр, право); ORIENTATION 1 = 90° против ч. с.
ANCHOR = {0: "start", 1: "middle", 2: "end"}
BASE = {0: "auto", 1: "central", 2: "hanging"}


def _put_text(g, t, text, size_mm):
    j = t["just"]
    anchor = ANCHOR[j % 3]
    base = BASE[j // 3]
    rot = {0: None, 1: -90, 2: 180, 3: 90}[t["orient"]]
    g.text(t["x"], t["y"], text, size_mm, anchor, rotate=rot, baseline=base)


def frame(info, form=1):
    """Рамка листа А4 по шаблону: info = code, title (строки), sheet, sheets."""
    lines, texts = FORM1 if form == 1 else FORM2A
    g = Svg()
    g.rect(0, 0, W, H, 0, "#fff")
    for x1, y1, x2, y2, w in lines:
        g.line(x1, y1, x2, y2, w)
    for t in texts:
        s = t["text"]
        size = t["size"] * 0.2          # пт шаблона -> высота шрифта, мм (14 -> 2,8; 36 -> 7,2)
        if s.startswith("="):
            key = s[1:].split()[0].strip("'")
            if key == "DecimalNumber":
                _put_text(g, t, info["code"], 7.0 if t["orient"] == 0 else 5.0)
            elif key in NAMES:
                _put_text(g, t, NAMES[key], 2.8)
            elif key in ("SheetNumber", "SheetTotal"):
                # по высоте — на одной линии с подписью графы («Лист», «Листов»)
                label = [u for u in texts if u["text"] in ("Лист", "Листов") and abs(u["y"] - t["y"]) < 3]
                y = label[0]["y"] if label else t["y"]
                val = info.get("sheet", 1) if key == "SheetNumber" else info.get("sheets", 1)
                g.text(t["x"] + 2, y, str(val), 2.8, "middle", baseline="central")
            elif key == "lit_A":
                _put_text(g, t, LIT, 2.8)
            elif key == "CompanyName":
                g.text(t["x"], t["y"] - 2.6, ORG[0], 3.6, "middle", baseline="central")
                g.text(t["x"], t["y"] + 2.6, ORG[1], 3.6, "middle", baseline="central")
            # Name1, Name2 и прочие параметры заполняются ниже или остаются пустыми
            continue
        if s in ("СХЕМА   ЭЛЕКТРИЧЕСКАЯ", "ПРИНЦИПИАЛЬНАЯ"):
            continue                    # вид документа из шаблона схемы заменяется своим наименованием
        s = {"Нач.сект.": "Рец.", "Масштаб": "Масса", "Масса": "Масштаб"}.get(s, s)  # графы 5 и 6 в порядке ГОСТ
        _put_text(g, t, s, min(size, 2.8))
    if form == 1:
        # графа 1 (наименование): поле x 85..155, y 252..277 (сверху вниз)
        title = info["title"]
        n = len(title)
        for i, l in enumerate(title):
            g.text(120, 264.5 + (i - (n - 1) / 2) * 5.2, l, 3.5, "middle", baseline="central")
    return g


# область для изображения внутри рамки (над основной надписью и ниже графы 26)
AREA1 = (23, 22, 179, 204)      # x, y, ширина, высота — для формы 1 (выше полосы над основной надписью)
AREA2A = (23, 22, 179, 253)     # для формы 2а


# ----------------------------------------------------------------- вывод страниц
POSTER_CSS = """
.poster{position:absolute;box-sizing:border-box;font-family:'Liberation Sans',Arial,sans-serif;display:flex;flex-direction:column}
.poster h1{margin:0;text-align:center;font-size:5.6mm;line-height:7mm;font-weight:bold;color:#000}
.poster .rule{height:0.7mm;background:#8b1a1a;margin:1.2mm 0 2.5mm 0}
.poster .grid{flex:1;display:grid;gap:2.5mm;min-height:0}
.panel{border:0.4mm solid #c08080;display:flex;flex-direction:column;min-height:0;min-width:0;padding:1mm 1.5mm 1.5mm 1.5mm}
.panel.bare{border:none;padding:0}
.panel h2{margin:0 0 1mm 0;text-align:center;font-size:3.4mm;line-height:4.4mm;font-weight:normal}
.panel .img{flex:1;min-height:0;display:flex;align-items:center;justify-content:center}
.panel img{max-width:100%;max-height:100%;object-fit:contain;display:block}
.panel.shot img{border:0.25mm solid #9a9a9a}
"""


def page_html(pages):
    divs = []
    for svg, overlay in pages:
        inner = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}mm" height="{H}mm" viewBox="0 0 {W} {H}" '
                 f'style="position:absolute;left:0;top:0">{svg}</svg>{overlay or ""}')
        divs.append(f'<div class="pg" style="width:{W}mm;height:{H}mm">{inner}</div>')
    return ("<!doctype html><html><head><meta charset='utf-8'><style>"
            f"@page{{size:{W}mm {H}mm;margin:0}}html,body{{margin:0;padding:0}}"
            ".pg{position:relative;overflow:hidden;page-break-after:always;break-after:page;background:#fff}"
            ".pg:last-child{page-break-after:auto;break-after:auto}svg{display:block}"
            f"{CSS_FONT}{POSTER_CSS}</style></head><body>{''.join(divs)}</body></html>")


def render(pages, pdf, png_prefix=None, dpi=200):
    html = page_html(pages)
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        pg.set_content(html, wait_until="load")
        pg.wait_for_timeout(500)
        pg.pdf(path=str(pdf), width=f"{W}mm", height=f"{H}mm", print_background=True,
               margin={"top": "0", "bottom": "0", "left": "0", "right": "0"})
        if png_prefix:
            px = lambda mm: round(mm / 25.4 * 96)
            pg2 = b.new_page(viewport={"width": px(W), "height": px(H)}, device_scale_factor=dpi / 96)
            pg2.set_content(html, wait_until="load")
            pg2.wait_for_timeout(500)
            els = pg2.query_selector_all(".pg")
            for i, el in enumerate(els):
                name = f"{png_prefix}.png" if len(els) == 1 else f"{png_prefix}_{i + 1}.png"
                el.screenshot(path=str(name))
        b.close()


def place_svg(g, src_svg, area, top_align=False):
    m = re.search(r'width="([\d.]+)" height="([\d.]+)"', src_svg)
    dw, dh = float(m.group(1)), float(m.group(2))
    inner = re.sub(r"^<svg[^>]*>", "", src_svg.strip()).rsplit("</svg>", 1)[0]
    ax, ay, aw, ah = area
    sc = min(aw / dw, ah / dh)
    w, h = dw * sc, dh * sc
    x, y = ax + (aw - w) / 2, ay + (0 if top_align else (ah - h) / 2)
    g.raw(f'<svg x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" viewBox="0 0 {dw} {dh}" '
          f'overflow="visible">{inner}</svg>')
    return x, y, w, h, sc


# ----------------------------------------------------------------- чертёж 1: IDEF0
def drawing_idef0():
    g = frame({"code": CODE_IDEF0, "title": IDEF0_TITLE})
    place_svg(g, (DIAG / "idef0_A0.svg").read_text(encoding="utf8"), AREA1)
    return [(str(g), None)]


# ----------------------------------------------------------------- чертёж 2: схема алгоритма
ZONE_ROWS = "ABCDEFGHJKLMNOPQRSTUVWXYZ"


def drawing_algorithm():
    ch = flowchart.payout_chart(compact=True)
    ncol, nrow = 6, 18
    cw, rh = ch.W / ncol, ch.H / nrow
    labels = []
    for key, (kind, cx, cy, w, h, _t) in ch.blocks.items():
        x0, y0 = cx - w / 2, cy - h / 2
        zone = f"{ZONE_ROWS[min(int(y0 // rh), nrow - 1)]}{int(cx // cw) + 1:02d}"
        if kind == "dec":
            labels.append(f'<text x="{cx - w / 4 - 6:.1f}" y="{y0 + h / 4 - 4:.1f}" font-size="11" '
                          f'text-anchor="end">{zone}</text>')
        elif kind == "conn":
            labels.append(f'<text x="{x0 - 3:.1f}" y="{y0 + 2:.1f}" font-size="11" text-anchor="end">{zone}</text>')
        else:
            labels.append(f'<text x="{x0 + 3:.1f}" y="{y0 - 4:.1f}" font-size="11">{zone}</text>')
    svg = ch.svg()
    svg = svg.rsplit("</svg>", 1)[0] + "".join(labels) + "</svg>"
    g = frame({"code": CODE_ALG, "title": ALG_TITLE})
    ax, ay, aw, ah = AREA1
    # поле схемы с местом под линейки зон (сверху и слева)
    x, y, w, h, sc = place_svg(g, svg, (ax + 9, ay + 8, aw - 11, ah - 9), top_align=True)
    ry = y - 5
    g.line(x, ry, x + w, ry, THIN)
    for i in range(ncol + 1):
        g.line(x + i * w / ncol, ry, x + i * w / ncol, ry + 2.5, THIN)
    for i in range(ncol):
        g.text(x + (i + 0.5) * w / ncol, ry - 1.2, f"{i + 1:02d}", 2.8, "middle")
    rx = x - 6
    g.line(rx, y, rx, y + h, THIN)
    for i in range(nrow + 1):
        g.line(rx, y + i * h / nrow, rx + 2.5, y + i * h / nrow, THIN)
    for i in range(nrow):
        g.text(rx - 1.5, y + (i + 0.5) * h / nrow, ZONE_ROWS[i], 2.8, "end", baseline="central")
    return [(str(g), None)], sc


# ----------------------------------------------------------------- плакаты
def panel(title, img, cls="", style=""):
    return (f'<div class="panel {cls}" style="{style}"><h2>{escape(title)}</h2>'
            f'<div class="img"><img src="{b64(img)}"></div></div>')


def poster(n, title, grid_style, panels):
    g = frame({"code": CODE_POSTERS, "title": POSTERS_TITLE, "sheet": n, "sheets": 3})
    ax, ay, aw, ah = AREA1
    overlay = (f'<div class="poster" style="left:{ax}mm;top:{ay - 1}mm;width:{aw}mm;height:{ah + 1}mm">'
               f'<h1>{escape(title)}</h1><div class="rule"></div>'
               f'<div class="grid" style="{grid_style}">{"".join(panels)}</div></div>')
    return [(str(g), overlay)]


def posters():
    p1 = poster(1, POSTERS[0][0], "grid-template-columns:1fr 1fr;grid-template-rows:36fr 28fr 36fr", [
        panel("Классы модели данных (модуль common)", DIAG / "classes_model.png", style="grid-column:1/3"),
        panel("Классы бизнес-логики сервера", DIAG / "classes_logic.png", style="grid-column:1/3"),
        panel("Классы сетевого и сервисного уровней, доступа к данным", DIAG / "classes_server.png",
              style="grid-column:1/3"),
    ])
    p2 = poster(2, POSTERS[1][0], "grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr 1fr", [
        panel("Диаграмма вариантов использования", DIAG / "usecase.png"),
        panel("Диаграмма развёртывания", DIAG / "deployment.png"),
        panel("Логическая модель базы данных (IDEF1X)", DIAG / "idef1x.png", style="grid-column:1/3"),
        panel("Диаграмма последовательности «Рассмотрение заявки»", DIAG / "sequence.png", style="grid-column:1/3"),
    ])
    shots = [
        ("Расчёт стоимости страхования", "05_calc.png"),
        ("Договор и график взносов", "11_my_policy.png"),
        ("Рассмотрение заявок агентом", "08_agent_applications.png"),
        ("Расчёт страховой выплаты", "17_payout_calc.png"),
        ("Отчёт о продажах агентов", "26_sales.png"),
        ("Анализ убыточности", "28_loss_ratio.png"),
    ]
    p3 = poster(3, POSTERS[2][0], "grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr 1fr;gap:2mm 3mm",
                [panel(t, SHOTS / f, "bare shot") for t, f in shots])
    return [p1, p2, p3]


# ----------------------------------------------------------------- ведомость документов
def stamp_form2(g, x0, y0, info):
    """Основная надпись по форме 2 (текстовые документы, первый лист), 185×40 мм.
    Ширины граф левой части — как в форме 1 шаблона (8, 9, 23, 15, 10 мм)."""
    ln = lambda x1, y1, x2, y2, w=THICK: g.line(x0 + x1, y0 + y1, x0 + x2, y0 + y2, w)
    tx = lambda x, y, t, s=2.8, a="start": g.text(x0 + x, y0 + y, t, s, a, baseline="central")
    g.rect(x0, y0, 185, 40, THICK, "#fff")
    for c in [7, 17, 40, 55, 65]:          # графы 7, 10, 23, 15, 10 мм (ГОСТ 2.104, форма 2)
        ln(c, 0, c, 15 if c == 7 else 40)
    for r in range(1, 8):
        ln(0, r * 5, 65, r * 5, THICK if r in (2, 3) else THIN)
    ln(65, 15, 185, 15)
    ln(135, 15, 135, 40)
    ln(135, 20, 185, 20)
    ln(135, 25, 185, 25)
    ln(150, 15, 150, 25)
    ln(165, 15, 165, 25)
    ln(140, 20, 140, 25, THIN)
    ln(145, 20, 145, 25, THIN)
    tx(125, 7.5, info["code"], 7.0, "middle")
    for t, x in zip(["Изм.", "Лист", "№ докум.", "Подп.", "Дата"], [3.5, 12, 28.5, 47.5, 60]):
        tx(x, 12.5, t, 2.6, "middle")
    for i, t in enumerate(["Разраб.", "Пров.", "Т.контр.", "Н.контр.", "Утв."]):
        tx(1, 17.5 + i * 5, t, 2.8)
        val = {"Разраб.": NAMES["Author"], "Пров.": NAMES["CheckedBy"]}.get(t)
        if val:
            tx(19, 17.5 + i * 5, val, 2.8)
    for i, l in enumerate(info["title"].split("\n")):
        tx(100, 19.5 + i * 4.4, l, 3.4, "middle")
    tx(100, 36.5, info["doc"], 3.4, "middle")
    tx(142.5, 17.5, "Лит.", 2.8, "middle")
    tx(157.5, 17.5, "Лист", 2.8, "middle")
    tx(175, 17.5, "Листов", 2.8, "middle")
    tx(137.5, 22.5, LIT, 2.8, "middle")
    tx(157.5, 22.5, str(info["sheet"]), 2.8, "middle")
    tx(175, 22.5, str(info["sheets"]), 2.8, "middle")
    tx(160, 29.8, ORG[0], 3.6, "middle")
    tx(160, 35.0, ORG[1], 3.6, "middle")


def vedomost(n_pages):
    g = Svg()
    g.rect(0, 0, W, H, 0, "#fff")
    # рамка и дополнительные графы на левом поле — по линиям шаблона формы 1
    lines, texts = FORM1
    for x1, y1, x2, y2, w in lines:
        if max(x1, x2) <= 20.01:            # дополнительные графы 19–25 на левом поле
            g.line(x1, y1, x2, y2, w)
    g.rect(20, 5, 185, 287, THICK)
    for t in texts:
        if t["x"] < 20 and not t["text"].startswith("="):
            _put_text(g, t, t["text"], 2.8)
    # таблица: Обозначение | Наименование | Дополнительные сведения
    X = [20, 85, 170, 205]
    top, head, rowh = 5, 15, 8
    stamp_y = 292 - 40
    for x in X[1:3]:
        g.line(x, top, x, stamp_y)
    g.line(20, top + head, 205, top + head)
    g.text(52.5, top + 7.5, "Обозначение", 3.5, "middle", baseline="central")
    g.text(127.5, top + 7.5, "Наименование", 3.5, "middle", baseline="central")
    g.text(187.5, top + 5.2, "Дополнитель-", 3.5, "middle", baseline="central")
    g.text(187.5, top + 10.2, "ные сведения", 3.5, "middle", baseline="central")
    nrows = int((stamp_y - top - head) // rowh)
    for i in range(1, nrows + 1):
        g.line(20, top + head + i * rowh, 205, top + head + i * rowh, THIN)
    rows = [
        ("", "", ""),
        ("", "#Текстовые документы", ""),
        ("", "", ""),
        (CODE_PZ, "Пояснительная записка", f"{n_pages} с."),
        ("", "Отчет о проверке на заимство-", "Приложение А"),
        ("", "вания в системе «Антиплагиат»", "к ПЗ"),
        ("", "", ""),
        ("", "#Графические документы", ""),
        ("", "", ""),
        (CODE_IDEF0, "IDEF0-модель процессов", "Формат А4"),
        ("", "предметной области", ""),
        ("", "", ""),
        (CODE_ALG, "Схема алгоритма расчёта", "Формат А4"),
        ("", "страховой выплаты", ""),
        ("", "", ""),
        (CODE_POSTERS, "Плакаты, отражающие", "Формат А4"),
        ("", "результаты курсового", "3 листа"),
        ("", "проектирования:", ""),
    ] + [("", name, "") for _t, name in POSTERS] + [
        ("", "", ""),
        ("", "#Прочие документы", ""),
        ("", "", ""),
        ("", "Исходный код программного", "Репозиторий"),
        ("", "средства", "GitHub"),
    ]
    for i, (code, name, extra) in enumerate(rows):
        yc = top + head + (i + 0.5) * rowh
        if code:
            g.text(21.5, yc, code, 3.4, baseline="central")
        if name.startswith("#"):
            g.text(127.5, yc, name[1:], 3.5, "middle", underline=True, baseline="central")
        elif name:
            g.text(86.5, yc, name, 3.5, baseline="central")
        if extra:
            g.text(187.5, yc, extra, 3.4, "middle", baseline="central")
    stamp_form2(g, 20, stamp_y, {"code": CODE_VED, "title": TOPIC, "doc": "Ведомость документов",
                                 "sheet": n_pages, "sheets": n_pages})
    g.text(105, 295.3, "Копировал", 2.8, "middle", baseline="central")
    g.text(180, 295.3, "Формат А4", 2.8, "middle", baseline="central")
    return [(str(g), None)]


# ----------------------------------------------------------------- main
def main():
    n_pages = int(sys.argv[1]) if len(sys.argv) > 1 else 99
    OUT.mkdir(exist_ok=True)
    only = set(sys.argv[2:])
    if not only or "ved" in only:
        render(vedomost(n_pages), OUT / "Ведомость_документов.pdf", KP / "docs" / "vedomost", dpi=200)
        print("ведомость:", n_pages, "с.")
    if not only or "d1" in only:
        render(drawing_idef0(), OUT / "Чертеж_1_IDEF0.pdf", OUT / "Чертеж_1_IDEF0")
        print("чертёж 1")
    if not only or "d2" in only:
        pages, sc = drawing_algorithm()
        render(pages, OUT / "Чертеж_2_Схема_алгоритма.pdf", OUT / "Чертеж_2_Схема_алгоритма")
        print(f"чертёж 2, шрифт схемы {flowchart.FS * sc:.2f} мм")
    if not only or "pl" in only:
        for i, pages in enumerate(posters(), 1):
            render(pages, OUT / f"Плакат_{i}.pdf", OUT / f"Плакат_{i}")
            print("плакат", i)


if __name__ == "__main__":
    main()
