"""Hand-laid UML use case diagram (SVG -> PNG)."""
import math, sys
from html import escape
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from render import render

W, H = 1640, 1330
p = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
     '<defs><marker id="open" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="13" markerHeight="13" orient="auto" markerUnits="userSpaceOnUse">'
     '<path d="M0,1 L11,6 L0,11" fill="none" stroke="#000" stroke-width="1.4"/></marker>'
     '<marker id="tri" viewBox="0 0 16 16" refX="15" refY="8" markerWidth="18" markerHeight="18" orient="auto" markerUnits="userSpaceOnUse">'
     '<path d="M1,1 L15,8 L1,15 z" fill="#fff" stroke="#000" stroke-width="1.4"/></marker></defs>',
     f'<rect width="{W}" height="{H}" fill="#fff"/>']


def text(x, y, s, size=17, anchor="middle", weight="normal", italic=False):
    st = ' font-style="italic"' if italic else ''
    lines = s.split("\n")
    y0 = y - (len(lines) - 1) * (size + 2) / 2 + size * 0.35
    for i, l in enumerate(lines):
        p.append(f'<text x="{x}" y="{y0 + i * (size + 2):.1f}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}"{st}>{escape(l)}</text>')


RX, RY = 135, 38
UC = {}


def uc(key, x, y, label):
    UC[key] = (x, y)
    p.append(f'<ellipse cx="{x}" cy="{y}" rx="{RX}" ry="{RY}" fill="#fff" stroke="#000" stroke-width="1.6"/>')
    text(x, y, label)


ACT = {}


def actor(key, x, y, label):
    ACT[key] = (x, y)
    s = 'stroke="#000" stroke-width="1.8" fill="none"'
    p.append(f'<circle cx="{x}" cy="{y - 38}" r="11" {s} fill="#fff"/>')
    p.append(f'<line x1="{x}" y1="{y - 27}" x2="{x}" y2="{y + 6}" {s}/>')
    p.append(f'<line x1="{x - 18}" y1="{y - 16}" x2="{x + 18}" y2="{y - 16}" {s}/>')
    p.append(f'<line x1="{x}" y1="{y + 6}" x2="{x - 14}" y2="{y + 30}" {s}/>')
    p.append(f'<line x1="{x}" y1="{y + 6}" x2="{x + 14}" y2="{y + 30}" {s}/>')
    text(x, y + 50, label)


def edge_pt(cx, cy, tx, ty):
    """point on use-case ellipse towards (tx,ty)"""
    a = math.atan2(ty - cy, tx - cx)
    k = 1 / math.sqrt((math.cos(a) / RX) ** 2 + (math.sin(a) / RY) ** 2)
    return cx + k * math.cos(a), cy + k * math.sin(a)


def assoc(akey, ukey):
    ax, ay = ACT[akey]; ux, uy = UC[ukey]
    sx = ax + (24 if ux > ax else -24)
    ex, ey = edge_pt(ux, uy, sx, ay)
    p.append(f'<line x1="{sx}" y1="{ay - 10}" x2="{ex:.1f}" y2="{ey:.1f}" stroke="#000" stroke-width="1.5"/>')


def dep(a, b, stereo, lab_dx=0, lab_dy=-8):
    (x1, y1), (x2, y2) = UC[a], UC[b]
    sx, sy = edge_pt(x1, y1, x2, y2); ex, ey = edge_pt(x2, y2, x1, y1)
    p.append(f'<line x1="{sx:.1f}" y1="{sy:.1f}" x2="{ex:.1f}" y2="{ey:.1f}" stroke="#000" stroke-width="1.4" '
             f'stroke-dasharray="7,5" marker-end="url(#open)"/>')
    text((sx + ex) / 2 + lab_dx, (sy + ey) / 2 + lab_dy, f"«{stereo}»", size=15)


# system boundary
p.append('<rect x="270" y="30" width="1110" height="1140" fill="none" stroke="#000" stroke-width="1.8"/>')
text(825, 52, "ПС «Страховой сервис»", size=19, weight="bold")

L, M, R = 490, 825, 1155
uc("UC1", L, 120, "Регистрация\nклиента")
uc("UC3", L, 240, "Расчёт стоимости\nстрахования")
uc("UC4", L, 350, "Подача заявки\nна страхование")
uc("UC6", L, 460, "Внесение\nстрахового взноса")
uc("UC7", L, 570, "Подача заявления\nо страховом случае")
uc("UC12", L, 680, "Просмотр договоров,\nвзносов и выплат")
uc("UC2", L, 1100, "Авторизация")
uc("UCg", M, 125, "Формирование\nграфика взносов")
uc("UCp", M, 420, "Расчёт страховой\nвыплаты")
uc("UC17", M, 1010, "Выгрузка отчёта\nв файл")
uc("UC5", R, 210, "Рассмотрение заявки\nи оформление полиса")
uc("UC8", R, 330, "Урегулирование\nстрахового случая")
uc("UC9", R, 440, "Продление\nдоговора")
uc("UC10", R, 550, "Досрочное расторжение\nс расчётом возврата")
uc("UC11", R, 660, "Контроль просрочек и\nистекающих договоров")
uc("UC13", R, 780, "Управление видами\nстрахования и тарифами")
uc("UC14", R, 880, "Управление учётными\nзаписями агентов")
uc("UC15", R, 980, "Отчёт о продажах\nагентов с диаграммой")
uc("UC16", R, 1090, "Анализ убыточности\nвидов страхования")

actor("G", 120, 110, "Гость")
actor("C", 120, 455, "Клиент")
actor("A", 1520, 430, "Агент")
actor("AD", 1520, 930, "Администратор")
actor("U", 825, 1245, "Пользователь")

assoc("G", "UC1")
for k in ("UC3", "UC4", "UC6", "UC7", "UC12"):
    assoc("C", k)
for k in ("UC5", "UC8", "UC9", "UC10", "UC11"):
    assoc("A", k)
for k in ("UC13", "UC14", "UC15", "UC16"):
    assoc("AD", k)
# Пользователь — Авторизация
ux, uy = ACT["U"]; ex, ey = edge_pt(*UC["UC2"], ux - 24, uy - 10)
p.append(f'<line x1="{ux - 24}" y1="{uy - 10}" x2="{ex:.1f}" y2="{ey:.1f}" stroke="#000" stroke-width="1.5"/>')

dep("UC5", "UC3", "include", 0, -10)
dep("UC5", "UCg", "include", 40, -6)
dep("UC8", "UCp", "include", 10, -10)
dep("UC17", "UC15", "extend", 0, -12)
dep("UC17", "UC16", "extend", 10, 22)

# generalisations to Пользователь (hollow triangle)
g = 'fill="none" stroke="#000" stroke-width="1.5"'
p.append(f'<polyline points="120,540 120,1235 790,1235" {g} marker-end="url(#tri)"/>')
p.append(f'<polyline points="1520,515 1520,560 1600,560 1600,1235 860,1235" {g} marker-end="url(#tri)"/>')
p.append(f'<polyline points="1520,1015 1520,1060 1600,1060" {g}/>')
p.append('<circle cx="1600" cy="1060" r="3" fill="#000"/>')
p.append("</svg>")

svg = "\n".join(p)
out = Path("/home/claude/kp/diagrams")
(out / "usecase.svg").write_text(svg, encoding="utf8")
render(svg, W, H, png=str(out / "usecase.png"), scale=2)
print("ok")
