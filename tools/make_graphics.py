"""Графическая часть курсового проекта и ведомость документов (формат А4, PDF + PNG).

Оформление по шаблонам кафедры ЭИ (папка «Шаблоны граф материалов Visio»):
  Чертёж 1  ГУИР.425707.015 Д2  IDEF0-модель процессов предметной области (А4, альбомная);
  Чертёж 2  ГУИР.425707.015 Д3  схема алгоритма расчёта страховой выплаты (А4, книжная);
  Плакаты   ГУИР.425707.015 Д5  листы 1–3: лицевая сторона с материалами, оборотная — рамка и штамп;
  Ведомость БГУИР КП 1-40 01 02-08 015 Д1 (основная надпись по форме 2, последний лист ПЗ).
Рамка: поля 20 мм слева, 5 мм с остальных сторон; основная надпись по ГОСТ 2.104 (форма 1 — 185×55,
форма 2 — 185×40); шрифт — чертёжный ГОСТ тип Б, курсив.

Запуск: python3 make_graphics.py [число_страниц_ПЗ]
"""
import base64
import re
import sys
from html import escape
from pathlib import Path

from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).parent))
import flowchart  # noqa: E402

TOOLS = Path(__file__).parent
KP = TOOLS.parent
DIAG = KP / "diagrams"
SHOTS = KP / "screenshots"
OUT = KP / "graphics"

FONT_B64 = base64.b64encode((TOOLS / "GOSTTypeB-italic.ttf").read_bytes()).decode()
CSS_FONT = (f"@font-face{{font-family:GOST;src:url(data:font/ttf;base64,{FONT_B64})}}"
            ".g{font-family:GOST,'Liberation Sans',sans-serif;font-style:italic}"
            "svg text{font-family:'Times New Roman','Liberation Serif',serif}")

# ----------------------------------------------------------------- общие данные
CODE_PZ = "БГУИР КП 1-40 01 02-08 015 ПЗ"
CODE_VED = "БГУИР КП 1-40 01 02-08 015 Д1"
CODE_IDEF0 = "ГУИР.425707.015 Д2"
CODE_ALG = "ГУИР.425707.015 Д3"
CODE_POSTERS = "ГУИР.425707.015 Д5"
TOPIC = "Программное средство для\nподдержки процесса оказания\nстраховых услуг населению"
ORG = "Кафедра ЭИ,\nгруппа 414351"
NAMES = {"Разраб.": "Балащенко", "Пров.": "Сторожев"}
POSTERS = [
    ("Диаграмма классов программного средства", "Лист 1 – Диаграмма классов ПС"),
    ("Модели представления программного средства", "Лист 2 – Модели представления ПС"),
    ("Интерфейс программного средства", "Лист 3 – Интерфейс ПС"),
]
ALG_TITLE = "Схема алгоритма расчёта\nстраховой выплаты"
IDEF0_TITLE = "IDEF0-модель процессов\nпредметной области"
POSTERS_TITLE = "Плакаты, отражающие\nрезультаты курсового\nпроектирования"

L, T = 0.6, 0.25        # толщина основных и тонких линий, мм


def b64(path):
    return "data:image/png;base64," + base64.b64encode(Path(path).read_bytes()).decode()


class Svg:
    """Накопитель SVG-примитивов в миллиметрах."""

    def __init__(self):
        self.s = []

    def line(self, x1, y1, x2, y2, w=L, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.s.append(f'<line x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}" stroke="#000" '
                      f'stroke-width="{w}"{d}/>')

    def rect(self, x, y, w, h, sw=L, fill="none"):
        self.s.append(f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" fill="{fill}" '
                      f'stroke="#000" stroke-width="{sw}"/>')

    def text(self, x, y, t, size=3.5, anchor="start", rotate=None, underline=False, cls="g"):
        tr = f' transform="rotate({rotate} {x:.2f} {y:.2f})"' if rotate is not None else ""
        deco = ' text-decoration="underline"' if underline else ""
        self.s.append(f'<text class="{cls}" x="{x:.2f}" y="{y:.2f}" font-size="{size}" '
                      f'text-anchor="{anchor}"{tr}{deco}>{escape(t)}</text>')

    def raw(self, x):
        self.s.append(x)

    def __str__(self):
        return "".join(self.s)


# ----------------------------------------------------------------- основные надписи
def stamp_form1(g, x0, y0, info):
    """Основная надпись по форме 1 ГОСТ 2.104 (чертежи и схемы), 185×55 мм."""
    ln = lambda x1, y1, x2, y2, w=L: g.line(x0 + x1, y0 + y1, x0 + x2, y0 + y2, w)
    tx = lambda x, y, t, s=2.6, a="start": g.text(x0 + x, y0 + y, t, s, a)
    g.rect(x0, y0, 185, 55, L, "#fff")
    for c in [7, 17, 40, 55, 65]:
        ln(c, 0, c, 25 if c == 7 else 55)
    for r in range(1, 11):
        ln(0, r * 5, 65, r * 5, L if r in (4, 5) else T)
    ln(65, 15, 185, 15)
    ln(65, 40, 185, 40)
    ln(135, 15, 135, 55)
    ln(135, 20, 185, 20)
    ln(135, 35, 185, 35)
    ln(150, 15, 150, 35)
    ln(167, 15, 167, 35)
    ln(140, 20, 140, 35, T)
    ln(145, 20, 145, 35, T)
    ln(155, 35, 155, 40)
    tx(125, 10, info["code"], 5.0, "middle")
    for t, x in zip(["Изм.", "Лист", "№ докум.", "Подп.", "Дата"], [3.5, 12, 28.5, 47.5, 60]):
        tx(x, 24, t, 2.5, "middle")
    for i, t in enumerate(["Разраб.", "Пров.", "Т.контр.", "Рец.", "Н.контр.", "Утв."]):
        tx(0.8, 29 + i * 5, t, 2.5)
        if t in NAMES:
            tx(17.8, 29 + i * 5, NAMES[t], 2.6)
    lines = info["title"].split("\n")
    for i, l in enumerate(lines):
        tx(100, 28.6 - (len(lines) - 1) * 2.2 + i * 4.4, l, 3.4, "middle")
    tx(142.5, 18.9, "Лит.", 2.5, "middle")
    tx(158.5, 18.9, "Масса", 2.5, "middle")
    tx(176, 18.9, "Масштаб", 2.5, "middle")
    tx(137.5, 29, "Т", 3.0, "middle")
    tx(145, 38.9, f"Лист {info.get('sheet', 1)}", 2.6, "middle")
    tx(170, 38.9, f"Листов {info.get('sheets', 1)}", 2.6, "middle")
    for i, l in enumerate(ORG.split("\n")):
        tx(160, 46.5 + i * 4.5, l, 3.4, "middle")


def stamp_form2(g, x0, y0, info):
    """Основная надпись по форме 2 ГОСТ 2.104 (текстовые документы, первый лист), 185×40 мм."""
    ln = lambda x1, y1, x2, y2, w=L: g.line(x0 + x1, y0 + y1, x0 + x2, y0 + y2, w)
    tx = lambda x, y, t, s=2.6, a="start": g.text(x0 + x, y0 + y, t, s, a)
    g.rect(x0, y0, 185, 40, L, "#fff")
    for c in [7, 17, 40, 55, 65]:
        ln(c, 0, c, 15 if c == 7 else 40)
    for r in range(1, 8):
        ln(0, r * 5, 65, r * 5, L if r in (2, 3) else T)
    ln(65, 15, 185, 15)
    ln(135, 15, 135, 40)
    ln(135, 20, 185, 20, T)
    ln(135, 25, 185, 25)
    ln(150, 15, 150, 25)
    ln(165, 15, 165, 25)
    ln(140, 20, 140, 25, T)
    ln(145, 20, 145, 25, T)
    tx(125, 10, info["code"], 5.0, "middle")
    for t, x in zip(["Изм.", "Лист", "№ докум.", "Подп.", "Дата"], [3.5, 12, 28.5, 47.5, 60]):
        tx(x, 14, t, 2.5, "middle")
    for i, t in enumerate(["Разраб.", "Пров.", "Т.контр.", "Н.контр.", "Утв."]):
        tx(0.8, 19 + i * 5, t, 2.5)
        if t in NAMES:
            tx(17.8, 19 + i * 5, NAMES[t], 2.6)
    lines = info["title"].split("\n")
    for i, l in enumerate(lines):
        tx(100, 20.2 + i * 4.3, l, 3.3, "middle")
    tx(100, 38.6, info["doc"], 3.3, "middle")
    tx(142.5, 18.9, "Лит.", 2.5, "middle")
    tx(157.5, 18.9, "Лист", 2.5, "middle")
    tx(175, 18.9, "Листов", 2.5, "middle")
    tx(137.5, 24, "Т", 3.0, "middle")
    tx(157.5, 24, str(info["sheet"]), 3.0, "middle")
    tx(175, 24, str(info["sheets"]), 3.0, "middle")
    for i, l in enumerate(ORG.split("\n")):
        tx(160, 31.5 + i * 4.8, l, 3.4, "middle")


def code_box(g, code):
    """Дополнительная графа с обозначением документа, повёрнутая на 180° (левый верхний угол рамки)."""
    g.raw(f'<g transform="rotate(180 55 12)">')
    g.rect(20, 5, 70, 14, L, "#fff")
    g.text(55, 13.8, code, 4.6, "middle")
    g.raw("</g>")


def framed(W, H, info, form=1):
    g = Svg()
    g.rect(0, 0, W, H, 0, "#fff")
    g.rect(20, 5, W - 25, H - 10, 0.8)
    if form == 1:
        stamp_form1(g, W - 5 - 185, H - 5 - 55, info)
        code_box(g, info["code"])
    return g


# ----------------------------------------------------------------- вывод страниц
def page_html(pages):
    """pages: [(svg_or_html, W, H, is_svg)] -> многостраничный HTML для печати."""
    divs = []
    for content, W, H, is_svg in pages:
        inner = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}mm" height="{H}mm" viewBox="0 0 {W} {H}">'
                 f'{content}</svg>') if is_svg else content
        divs.append(f'<div class="pg" style="width:{W}mm;height:{H}mm">{inner}</div>')
    W, H = pages[0][1], pages[0][2]
    return ("<!doctype html><html><head><meta charset='utf-8'><style>"
            f"@page{{size:{W}mm {H}mm;margin:0}}html,body{{margin:0;padding:0}}"
            ".pg{position:relative;overflow:hidden;page-break-after:always;break-after:page;background:#fff}"
            ".pg:last-child{page-break-after:auto;break-after:auto}svg{display:block}"
            f"{CSS_FONT}{POSTER_CSS}</style></head><body>{''.join(divs)}</body></html>")


def render(pages, pdf, png_prefix=None, dpi=200):
    html = page_html(pages)
    W, H = pages[0][1], pages[0][2]
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        pg.set_content(html, wait_until="load")
        pg.wait_for_timeout(400)
        pg.pdf(path=str(pdf), width=f"{W}mm", height=f"{H}mm", print_background=True,
               margin={"top": "0", "bottom": "0", "left": "0", "right": "0"})
        if png_prefix:
            px = lambda mm: round(mm / 25.4 * 96)
            pg2 = b.new_page(viewport={"width": px(W), "height": px(H)}, device_scale_factor=dpi / 96)
            pg2.set_content(html, wait_until="load")
            pg2.wait_for_timeout(400)
            els = pg2.query_selector_all(".pg")
            for i, el in enumerate(els):
                name = f"{png_prefix}.png" if len(els) == 1 else f"{png_prefix}_{i + 1}.png"
                el.screenshot(path=str(name))
        b.close()


# ----------------------------------------------------------------- чертёж 1: IDEF0
def drawing_idef0():
    W, H = 297, 210
    src = (DIAG / "idef0_A0.svg").read_text(encoding="utf8")
    m = re.search(r'width="(\d+)" height="(\d+)"', src)
    dw, dh = int(m.group(1)), int(m.group(2))
    inner = re.sub(r"^<svg[^>]*>", "", src.strip()).rsplit("</svg>", 1)[0]
    g = framed(W, H, {"code": CODE_IDEF0, "title": IDEF0_TITLE})
    ax, ay, aw, ah = 24, 21, 264, 126
    sc = min(aw / dw, ah / dh)
    w, h = dw * sc, dh * sc
    g.raw(f'<svg x="{ax + (aw - w) / 2:.2f}" y="{ay + (ah - h) / 2:.2f}" width="{w:.2f}" height="{h:.2f}" '
          f'viewBox="0 0 {dw} {dh}">{inner}</svg>')
    return [(str(g), W, H, True)]


# ----------------------------------------------------------------- чертёж 2: схема алгоритма
ZONE_ROWS = "ABCDEFGHJKLMNOPQRSTUVWXYZ"


def drawing_algorithm():
    W, H = 210, 297
    ch = flowchart.payout_chart(compact=True)
    ncol, nrow = 6, 18
    cw, rh = ch.W / ncol, ch.H / nrow
    # обозначения зон у символов (буква строки + номер столбца), как в шаблоне кафедры
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
    inner = re.sub(r"^<svg[^>]*>", "", svg.strip()).rsplit("</svg>", 1)[0] + "".join(labels)

    g = framed(W, H, {"code": CODE_ALG, "title": ALG_TITLE})
    # поле схемы: между линейками зон и основной надписью
    ax, ay, aw, ah = 34, 30, 166, H - 5 - 55 - 32
    sc = min(aw / ch.W, ah / ch.H)
    w, h = ch.W * sc, ch.H * sc
    x, y = ax + (aw - w) / 2, ay + 1
    g.raw(f'<svg x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" viewBox="0 0 {ch.W} {ch.H}" overflow="visible">{inner}</svg>')
    # линейки зон: сверху номера столбцов, слева буквы строк
    ry = y - 6
    g.line(x, ry, x + w, ry, T * 1.6)
    for i in range(ncol + 1):
        g.line(x + i * w / ncol, ry, x + i * w / ncol, ry + 2.5, T * 1.6)
    for i in range(ncol):
        g.text(x + (i + 0.5) * w / ncol, ry - 1.2, f"{i + 1:02d}", 2.8, "middle")
    rx = x - 6
    g.line(rx, y, rx, y + h, T * 1.6)
    for i in range(nrow + 1):
        g.line(rx, y + i * h / nrow, rx + 2.5, y + i * h / nrow, T * 1.6)
    for i in range(nrow):
        g.text(rx - 1.5, y + (i + 0.5) * h / nrow + 1, ZONE_ROWS[i], 2.8, "end")
    return [(str(g), W, H, True)], sc


# ----------------------------------------------------------------- плакаты
POSTER_CSS = """
.poster{position:absolute;inset:0;padding:5mm 6mm 5mm 6mm;box-sizing:border-box;font-family:'Liberation Sans',Arial,sans-serif;
  display:flex;flex-direction:column}
.poster h1{margin:0;text-align:center;font-size:6.2mm;line-height:8mm;font-weight:bold;color:#000}
.poster .rule{height:0.8mm;background:#8b1a1a;margin:1.5mm 0 3mm 0}
.poster .grid{flex:1;display:grid;gap:3mm;min-height:0}
.panel{border:0.45mm solid #c08080;display:flex;flex-direction:column;min-height:0;min-width:0;padding:1mm 2mm 2mm 2mm}
.panel.bare{border:none;padding:0}
.panel h2{margin:0 0 1.2mm 0;text-align:center;font-size:3.9mm;line-height:5mm;font-weight:normal}
.panel .img{flex:1;min-height:0;display:flex;align-items:center;justify-content:center}
.panel img{max-width:100%;max-height:100%;object-fit:contain;display:block}
.panel.shot img{border:0.25mm solid #9a9a9a}
"""


def panel(title, img, cls="", style=""):
    return (f'<div class="panel {cls}" style="{style}"><h2>{escape(title)}</h2>'
            f'<div class="img"><img src="{b64(img)}"></div></div>')


def poster_front(title, grid_style, panels):
    return (f'<div class="poster"><h1>{escape(title)}</h1><div class="rule"></div>'
            f'<div class="grid" style="{grid_style}">{"".join(panels)}</div></div>')


def poster_back(n):
    g = framed(297, 210, {"code": CODE_POSTERS, "title": POSTERS_TITLE, "sheet": n, "sheets": 3})
    return str(g)


def posters():
    p1 = poster_front(POSTERS[0][0], "grid-template-columns:1fr 1fr;grid-template-rows:47fr 53fr", [
        panel("Классы модели данных (модуль common)", DIAG / "classes_model.png", style="grid-column:1/3"),
        panel("Классы бизнес-логики сервера", DIAG / "classes_logic.png"),
        panel("Классы сетевого, сервисного уровней и доступа к данным", DIAG / "classes_server.png"),
    ])
    p2 = poster_front(POSTERS[1][0], "grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr", [
        panel("Диаграмма вариантов использования", DIAG / "usecase.png"),
        panel("Логическая модель базы данных (IDEF1X)", DIAG / "idef1x.png"),
        panel("Диаграмма последовательности «Рассмотрение заявки»", DIAG / "sequence.png"),
        panel("Диаграмма развёртывания", DIAG / "deployment.png"),
    ])
    shots = [
        ("Окно входа в систему", "01_login.png"),
        ("Расчёт стоимости страхования (клиент)", "05_calc.png"),
        ("Договор и график взносов (клиент)", "11_my_policy.png"),
        ("Рассмотрение заявок (агент)", "08_agent_applications.png"),
        ("Расчёт страховой выплаты (агент)", "17_payout_calc.png"),
        ("Расторжение договора (агент)", "21_terminate.png"),
        ("Виды страхования и тарифы (администратор)", "23_types.png"),
        ("Отчёт о продажах (администратор)", "26_sales.png"),
        ("Анализ убыточности (администратор)", "28_loss_ratio.png"),
    ]
    p3 = poster_front(POSTERS[2][0], "grid-template-columns:1fr 1fr 1fr;grid-template-rows:1fr 1fr 1fr;gap:2mm 4mm",
                      [panel(t, SHOTS / f, "bare shot") for t, f in shots])
    return [p1, p2, p3]


# ----------------------------------------------------------------- ведомость документов
def vedomost(n_pages):
    W, H = 210, 297
    g = Svg()
    g.rect(0, 0, W, H, 0, "#fff")
    g.rect(20, 5, W - 25, H - 10, 0.8)
    # дополнительные графы на левом поле
    def strip(y1, y2, label, dashed=False):
        dash = "1.6 1.2" if dashed else None
        w = T * 2 if dashed else L
        for x in (8, 13):
            g.line(x, y1, x, y2, w, dash)
        g.line(8, y1, 20, y1, w, dash)
        g.line(8, y2, 20, y2, w, dash)
        g.text(11.6, (y1 + y2) / 2, label, 2.5, "middle", rotate=-90)
    y = 5
    for label in ["Перв. примен.", "Справ. №"]:
        strip(y, y + 60, label, dashed=True)
        y += 60
    y = 292
    for label, hgt in [("Инв. № подл.", 25), ("Подп. и дата", 35), ("Взам. инв. №", 25),
                       ("Инв. № дубл.", 25), ("Подп. и дата", 35)]:
        strip(y - hgt, y, label)
        y -= hgt
    # таблица: Обозначение | Наименование | Дополнительные сведения
    X = [20, 85, 170, 205]
    top, head, rowh = 5, 15, 8
    stamp_y = 292 - 40
    for x in X[1:3]:
        g.line(x, top, x, stamp_y)
    g.line(20, top + head, 205, top + head)
    g.text(52.5, top + 9.3, "Обозначение", 3.5, "middle")
    g.text(127.5, top + 9.3, "Наименование", 3.5, "middle")
    g.text(187.5, top + 6.8, "Дополнитель-", 3.5, "middle")
    g.text(187.5, top + 12.2, "ные сведения", 3.5, "middle")
    nrows = int((stamp_y - top - head) // rowh)
    for i in range(1, nrows + 1):
        g.line(20, top + head + i * rowh, 205, top + head + i * rowh, T)
    rows = [
        ("", "", ""),
        ("", "#Текстовые документы", ""),
        ("", "", ""),
        (CODE_PZ, "Пояснительная записка", f"{n_pages} с."),
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
        yb = top + head + (i + 1) * rowh - 2.3
        if code:
            g.text(21.5, yb, code, 3.4)
        if name.startswith("#"):
            g.text(127.5, yb, name[1:], 3.5, "middle", underline=True)
        elif name:
            g.text(86.5, yb, name, 3.5)
        if extra:
            g.text(187.5, yb, extra, 3.4, "middle")
    stamp_form2(g, 20, stamp_y, {"code": CODE_VED, "title": TOPIC, "doc": "Ведомость документов",
                                 "sheet": n_pages, "sheets": n_pages})
    g.text(105, 295.6, "Копировал", 2.6, "middle")
    g.text(178, 295.6, "Формат А4", 2.6, "middle")
    return [(str(g), W, H, True)]


# ----------------------------------------------------------------- main
def main():
    n_pages = int(sys.argv[1]) if len(sys.argv) > 1 else 99
    OUT.mkdir(exist_ok=True)
    only = set(sys.argv[2:])

    if not only or "ved" in only:
        ved = vedomost(n_pages)
        render(ved, OUT / "Ведомость_документов.pdf", KP / "docs" / "vedomost", dpi=200)
        print("ведомость:", n_pages, "с.")
    if not only or "d1" in only:
        render(drawing_idef0(), OUT / "Чертеж_1_IDEF0.pdf", OUT / "Чертеж_1_IDEF0")
        print("чертёж 1")
    if not only or "d2" in only:
        pages, sc = drawing_algorithm()
        render(pages, OUT / "Чертеж_2_Схема_алгоритма.pdf", OUT / "Чертеж_2_Схема_алгоритма")
        print(f"чертёж 2, масштаб {sc:.3f} мм/px, шрифт {flowchart.FS * sc:.2f} мм")
    if not only or "pl" in only:
        fronts = posters()
        for i, front in enumerate(fronts, 1):
            render([(front, 297, 210, False), (poster_back(i), 297, 210, True)],
                   OUT / f"Плакат_{i}.pdf", OUT / f"Плакат_{i}")
            print("плакат", i)


if __name__ == "__main__":
    main()
