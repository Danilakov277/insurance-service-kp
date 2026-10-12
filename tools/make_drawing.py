"""A4 landscape drawing sheet (ГОСТ 2.104 form 1 title block) with an embedded diagram SVG -> PDF + PNG."""
import base64, re, sys
from html import escape
from pathlib import Path
from playwright.sync_api import sync_playwright

TOOLS = Path(__file__).parent
FONT_B64 = base64.b64encode((TOOLS / "GOSTTypeB-italic.ttf").read_bytes()).decode()
CSS = (f"@font-face{{font-family:GOST;src:url(data:font/ttf;base64,{FONT_B64})}}"
       ".g{font-family:GOST,'Liberation Sans',sans-serif;font-style:italic}")


def stamp(x0, y0, info):
    L = 0.5; T = 0.25
    s = []
    line = lambda x1, y1, x2, y2, w=L: s.append(
        f'<line x1="{x0+x1}" y1="{y0+y1}" x2="{x0+x2}" y2="{y0+y2}" stroke="#000" stroke-width="{w}"/>')
    def txt(x, y, t, size=3.0, anchor="start"):
        s.append(f'<text class="g" x="{x0+x}" y="{y0+y}" font-size="{size}" text-anchor="{anchor}">{escape(t)}</text>')
    s.append(f'<rect x="{x0}" y="{y0}" width="185" height="55" fill="#fff" stroke="#000" stroke-width="{L}"/>')
    cols = [7, 17, 40, 55, 65]
    for c in cols:
        line(c, 0, c, 25 if c == 7 else 55, L)
    for r in range(1, 11):
        y = r * 5
        w = L if r in (4, 5, 11) else T
        line(0, y, 65, y, w)
    # cols 7 vs 17 only in top 5 rows (changes block)
    s_rm = []
    # right part
    line(65, 15, 185, 15)           # under designation
    line(65, 40, 185, 40)           # under name / lit block
    line(135, 15, 135, 55)          # name | lit
    line(135, 20, 185, 20, T)       # under Лит/Масса/Масштаб header
    line(135, 35, 185, 35)          # above Лист/Листов
    line(150, 15, 150, 35); line(167, 15, 167, 35)
    line(140, 20, 140, 35, T); line(145, 20, 145, 35, T)
    line(155, 35, 155, 40, T)
    # labels
    txt(100, 9.8, info["code"], 4.3, "middle")
    for i, t in enumerate(["Изм.", "Лист", "№ докум.", "Подп.", "Дата"]):
        x = [3.5, 12, 28.5, 47.5, 60][i]
        txt(x, 24, t, 2.6, "middle")
    rows = ["Разраб.", "Пров.", "Т.контр.", "Рец.", "Н.контр.", "Утв."]
    names = info.get("names", {})
    for i, t in enumerate(rows):
        y = 29 + i * 5
        txt(0.8, y, t, 2.6)
        if t in names:
            txt(17.8, y, names[t], 2.6)
    name_lines = info["title"].split("\n")
    for i, l in enumerate(name_lines):
        txt(100, 27.5 - (len(name_lines) - 1) * 2.4 + i * 4.8, l, 3.6, "middle")
    txt(142.5, 18.8, "Лит.", 2.6, "middle"); txt(158.5, 18.8, "Масса", 2.6, "middle"); txt(176, 18.8, "Масштаб", 2.6, "middle")
    txt(142.5, 29, "Т", 3.0, "middle")
    txt(145, 38.8, "Лист 1", 2.6, "middle"); txt(170, 38.8, "Листов 1", 2.6, "middle")
    org = info["org"].split("\n")
    for i, l in enumerate(org):
        txt(160, 46 + i * 4.5, l, 3.4, "middle")
    return "".join(s)


def sheet(diagram_svg, dw, dh, info, out_pdf, out_png):
    W, H = 297, 210
    inner = re.sub(r'^<svg[^>]*>', '', diagram_svg.strip()).rsplit("</svg>", 1)[0]
    # diagram placement area: x 25..287, y 10..148
    ax, ay, aw, ah = 24, 21, 264, 126
    sc = min(aw / dw, ah / dh)
    w, h = dw * sc, dh * sc
    x, y = ax + (aw - w) / 2, ay + (ah - h) / 2
    code = info["code"]
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}mm" height="{H - 0.3}mm" viewBox="0 0 {W} {H}">'
           f'<rect width="{W}" height="{H}" fill="#fff"/>'
           f'<rect x="20" y="5" width="272" height="200" fill="none" stroke="#000" stroke-width="0.7"/>'
           f'<svg x="{x}" y="{y}" width="{w}" height="{h}" viewBox="0 0 {dw} {dh}">{inner}</svg>'
           f'{stamp(107, 150, info)}'
           f'<g transform="rotate(180 {20+35} {5+7})"><rect x="20" y="5" width="70" height="14" fill="#fff" stroke="#000" stroke-width="0.5"/>'
           f'<text class="g" x="55" y="13.8" font-size="4.2" text-anchor="middle">{escape(code)}</text></g>'
           f'</svg>')
    html = (f"<!doctype html><html><head><meta charset='utf-8'><style>@page{{size:297mm 210mm;margin:0}}"
            f"html,body{{margin:0;padding:0;overflow:hidden}}svg{{display:block}}svg text{{font-family:'Times New Roman','Liberation Serif',serif}}{CSS}</style>"
            f"</head><body>{svg}</body></html>")
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1123, "height": 794})
        pg.set_content(html)
        pg.wait_for_timeout(300)
        pg.pdf(path=out_pdf, width="297mm", height="210mm", print_background=True,
               margin={"top": "0", "bottom": "0", "left": "0", "right": "0"})
        pg2 = b.new_page(viewport={"width": 1123, "height": 794}, device_scale_factor=2.5)
        pg2.set_content(html.replace(f'width="{W}mm" height="{H - 0.3}mm"', 'width="1123" height="794"'))
        pg2.wait_for_timeout(300)
        pg2.screenshot(path=out_png)
        b.close()


if __name__ == "__main__":
    D = Path("/home/claude/kp/diagrams")
    svg = (D / "idef0_A0.svg").read_text(encoding="utf8")
    m = re.search(r'width="(\d+)" height="(\d+)"', svg)
    out = Path("/home/claude/kp/graphics"); out.mkdir(exist_ok=True)
    sheet(svg, int(m.group(1)), int(m.group(2)),
          {"code": "БГУИР КП 1-40 01 02-08 015 Д1", "title": "IDEF0-модель процессов\nпредметной области",
           "org": "Кафедра ЭИ,\nгруппа 414351", "names": {"Разраб.": "Балащенко", "Пров.": "Сторожев"}},
          str(out / "Чертеж_1_IDEF0.pdf"), str(out / "Чертеж_1_IDEF0.png"))
    print("ok")
