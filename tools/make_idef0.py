"""IDEF0 model of the insurance service domain: A-0, A0, A4, A44."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from idef0 import Diagram
from render import render

OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "/home/claude/kp/diagrams")
OUT.mkdir(parents=True, exist_ok=True)


def vbus(d, edge_y, trunk_x, trunk_to_y, bus_y, branches, label, lpos, anchor="start"):
    """Vertical trunk from page edge (top or bottom) with a horizontal bus and vertical branches.
    branches: list of (x, target_y)."""
    d.arrow([(trunk_x, edge_y), (trunk_x, trunk_to_y)], label, lpos, anchor)
    xs = [x for x, _ in branches] + [trunk_x]
    lo, hi = min(xs), max(xs)
    d.arrow([(lo, bus_y), (hi, bus_y)], head=False)
    for x, ty in branches:
        d.arrow([(x, bus_y), (x, ty)])
        if x != lo and x != hi:
            d.dots.append((x, bus_y))
    d.dots.append((trunk_x, bus_y))


def to_files(d, name):
    svg = d.svg()
    (OUT / f"{name}.svg").write_text(svg, encoding="utf8")
    render(svg, d.W, d.H, png=str(OUT / f"{name}.png"), scale=2)


# ---------------------------------------------------------------- A-0
def a_minus_0():
    d = Diagram(1500, 960, "A-0", "Оказывать страховые услуги населению", "1")
    b = d.add_box("A0", 520, 300, 460, 230, "Оказывать страховые\nуслуги населению", "A0")
    L = 2
    ins = [(0.22, "Обращение клиента\n(данные страхователя\nи объекта страхования)"),
           (0.55, "Страховые взносы"),
           (0.82, "Заявление о страховом\nслучае")]
    for f, t in ins:
        x, y = b.side("L", f)
        d.arrow([(L, y), (x, y)], t, (20, y - 10 - 23 * t.count("\n")))
    ctr = [(0.25, "Законодательство Республики\nБеларусь о страховании"),
           (0.75, "Правила страхования\nи тарифы компании")]
    for f, t in ctr:
        x, y = b.side("T", f)
        d.arrow([(x, L), (x, y)], t, (x + 8, 60))
    outs = [(0.22, "Договор страхования\n(полис)"),
            (0.55, "Страховая выплата или\nмотивированный отказ"),
            (0.82, "Отчётность и анализ\nубыточности")]
    for f, t in outs:
        x, y = b.side("R", f)
        d.arrow([(x, y), (1498, y)], t, (1010, y - 10 - 23 * t.count("\n")))
    mech = [(0.18, "Страховые агенты"), (0.5, "Администратор"), (0.82, "ПС «Страховой сервис»")]
    for f, t in mech:
        x, y = b.side("B", f)
        d.arrow([(x, 898), (x, y)], t, (x + 8, 860))
    d.boxes[0].num = "A0"
    note = ("Цель моделирования: описать процесс оказания страховых\nуслуг физическим лицам для определения функций,\n"
            "автоматизируемых программным средством.\nТочка зрения: руководитель страховой компании.")
    svg_extra = d._text(20, 30, note, size=15)
    svg = d.svg().replace("</svg>", svg_extra + "</svg>")
    (OUT / "idef0_A-0.svg").write_text(svg, encoding="utf8")
    render(svg, d.W, d.H, png=str(OUT / "idef0_A-0.png"), scale=2)
    return d


# ---------------------------------------------------------------- A0
def a0():
    W, H = 1760, 1120
    d = Diagram(W, H, "A0", "Оказывать страховые услуги населению", "2")
    bw, bh = 220, 124
    A1 = d.add_box("A1", 130, 130, bw, bh, "Консультировать\nи рассчитать\nстоимость\nстрахования", "A1")
    A2 = d.add_box("A2", 450, 300, bw, bh, "Заключить\nдоговор\nстрахования", "A2")
    A3 = d.add_box("A3", 770, 470, bw, bh, "Сопровождать\nдоговор\nстрахования", "A3")
    A4 = d.add_box("A4", 1090, 640, bw, bh, "Урегулировать\nстраховой\nслучай", "A4")
    A5 = d.add_box("A5", 1410, 810, bw, bh, "Формировать\nотчётность и\nанализировать\nубыточность", "A5")
    R = W - 2
    # inputs from left
    x, y = A1.side("L", 0.5); d.arrow([(2, y), (x, y)], "Обращение\nклиента", (14, y - 33))
    x, y = A3.side("L", 0.72); d.arrow([(2, y), (x, y)], "Страховые взносы", (14, y - 10))
    x, y = A4.side("L", 0.62); d.arrow([(2, y), (x, y)], "Заявление о страховом случае", (14, y - 10))
    # controls: rules & tariffs bus (y=100), legislation bus (y=58)
    vbus(d, 2, A1.side("T", 0.5)[0], A1.y, 100,
         [(A2.side("T", 0.3)[0], A2.y), (A3.side("T", 0.3)[0], A3.y), (A4.side("T", 0.22)[0], A4.y)],
         "Правила страхования и тарифы компании", (A1.side("T", 0.5)[0] + 8, 26))
    vbus(d, 2, A5.side("T", 0.5)[0], A5.y, 58, [(A4.side("T", 0.65)[0], A4.y)],
         "Законодательство Республики Беларусь\nо страховании", (A4.side("T", 0.65)[0] - 260, 22))
    # internal flows
    x0, y0 = A1.side("R", 0.5); x1, y1 = A2.side("L", 0.4); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Расчёт\nстоимости", (x0 + 8, y0 - 33))
    x0, y0 = A2.side("R", 0.28); d.arrow([(x0, y0), (R, y0)], "Договор страхования (полис)", (x0 + 10, y0 - 10))
    x0, y0 = A2.side("R", 0.72); x1, y1 = A3.side("L", 0.32); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "График\nвзносов", (cx + 8, (y0 + y1) / 2 - 4))
    x0, y0 = A3.side("R", 0.28); x1, y1 = A4.side("L", 0.28); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Статус\nдоговора\nи оплаты", (cx + 6, 590))
    x0, y0 = A3.side("R", 0.75); x1, y1 = A5.side("L", 0.3); cx = A4.x + bw + 42
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Данные о договорах\nи взносах", (cx + 8, A5.y - 52))
    x0, y0 = A4.side("R", 0.3); d.arrow([(x0, y0), (R, y0)], "Страховая выплата\nили отказ", (R - 170, y0 - 33))
    x0, y0 = A4.side("R", 0.75); x1, y1 = A5.side("L", 0.65); cx = A4.x + bw + 20
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Сведения\nо выплатах", (cx - 140, y1 + 26))
    x0, y0 = A5.side("R", 0.5); d.arrow([(x0, y0), (R, y0)], "Отчётность\nи аналитика", (x0 + 10, y0 - 33))
    # mechanisms
    yb = H - 60
    vbus(d, yb, A1.side("B", 0.2)[0], A1.y + bh, 990,
         [(A2.side("B", 0.2)[0], A2.y + bh), (A3.side("B", 0.2)[0], A3.y + bh), (A4.side("B", 0.2)[0], A4.y + bh)],
         "Страховые агенты", (A1.side("B", 0.2)[0] + 8, yb - 12))
    vbus(d, yb, A5.side("B", 0.5)[0], A5.y + bh, 1018,
         [(A1.side("B", 0.75)[0], A1.y + bh), (A2.side("B", 0.75)[0], A2.y + bh),
          (A3.side("B", 0.75)[0], A3.y + bh), (A4.side("B", 0.75)[0], A4.y + bh)],
         "ПС «Страховой сервис»", (A5.side("B", 0.5)[0] - 8, yb - 12), anchor="end")
    x, y = A5.side("B", 0.85); d.arrow([(x, yb), (x, A5.y + bh)], "Администратор", (x + 8, yb - 40))
    to_files(d, "idef0_A0")


# ---------------------------------------------------------------- A4
def a4():
    W, H = 1760, 1120
    d = Diagram(W, H, "A4", "Урегулировать страховой случай", "3")
    bw, bh = 220, 124
    B1 = d.add_box("A41", 200, 130, bw, bh, "Принять\nзаявление\nо страховом\nслучае", "A41")
    B2 = d.add_box("A42", 500, 300, bw, bh, "Проверить\nдействие договора\nи уплату\nвзносов", "A42")
    B3 = d.add_box("A43", 800, 470, bw, bh, "Оценить\nразмер\nущерба", "A43")
    B4 = d.add_box("A44", 1100, 640, bw, bh, "Рассчитать\nстраховую\nвыплату", "A44")
    B5 = d.add_box("A45", 1400, 810, bw, bh, "Оформить\nрешение по\nстраховому\nслучаю", "A45")
    R = W - 2
    x, y = B1.side("L", 0.5); d.arrow([(2, y), (x, y)], "I1 Заявление о\nстраховом случае", (14, y - 33))
    x, y = B2.side("L", 0.7); d.arrow([(2, y), (x, y)], "I2 Статус договора\nи оплаты", (14, y - 33))
    vbus(d, 2, B2.side("T", 0.5)[0], B2.y, 100,
         [(B4.side("T", 0.3)[0], B4.y), (B5.side("T", 0.3)[0], B5.y)],
         "C2 Правила страхования и тарифы компании", (B2.side("T", 0.5)[0] + 8, 26))
    vbus(d, 2, B5.side("T", 0.75)[0], B5.y, 58, [(B1.side("T", 0.5)[0], B1.y)],
         "C1 Законодательство Республики Беларусь о страховании", (B5.side("T", 0.75)[0] - 8, 26), anchor="end")
    x0, y0 = B1.side("R", 0.5); x1, y1 = B2.side("L", 0.35); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Зарегистри-\nрованное\nзаявление", (x0 + 8, y0 - 56))
    x0, y0 = B2.side("R", 0.3); x1, y1 = B3.side("L", 0.3); cx = x0 + 20
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Подтверждённое\nпокрытие", (x0 + 8, y0 - 33))
    x0, y0 = B2.side("R", 0.8); x1, y1 = B5.side("L", 0.8); cx = B3.x - 20
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Основание для отказа", (cx + 10, y1 - 10))
    x0, y0 = B3.side("R", 0.3); x1, y1 = B4.side("L", 0.3); cx = x0 + 50
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Размер ущерба", (x0 + 8, y0 - 10))
    x0, y0 = B3.side("R", 0.75); x1, y1 = B4.side("L", 0.72); cx = x0 + 20
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Данные\nдоговора", (cx - 6, B3.y + bh + 26), "end")
    x0, y0 = B4.side("R", 0.4); x1, y1 = B5.side("L", 0.35); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Сумма\nстраховой\nвыплаты", (x0 + 8, y0 - 56))
    x0, y0 = B5.side("R", 0.35); d.arrow([(x0, y0), (R, y0)], "O1 Страховая\nвыплата\nили отказ", (x0 + 8, y0 - 56))
    x0, y0 = B5.side("R", 0.75); d.arrow([(x0, y0), (R, y0)], "O2 Сведения\nо выплатах", (x0 + 8, y0 + 22))
    yb = H - 60
    vbus(d, yb, B1.side("B", 0.25)[0], B1.y + bh, 990,
         [(B2.side("B", 0.25)[0], B2.y + bh), (B3.side("B", 0.25)[0], B3.y + bh),
          (B4.side("B", 0.25)[0], B4.y + bh), (B5.side("B", 0.25)[0], B5.y + bh)],
         "M1 Страховой агент", (B1.side("B", 0.25)[0] + 8, yb - 12))
    vbus(d, yb, B5.side("B", 0.75)[0], B5.y + bh, 1018,
         [(B1.side("B", 0.75)[0], B1.y + bh), (B2.side("B", 0.75)[0], B2.y + bh),
          (B3.side("B", 0.75)[0], B3.y + bh), (B4.side("B", 0.75)[0], B4.y + bh)],
         "M2 ПС «Страховой сервис»", (B5.side("B", 0.75)[0] - 8, yb - 12), anchor="end")
    to_files(d, "idef0_A4")


# ---------------------------------------------------------------- A44
def a44():
    W, H = 1700, 1060
    d = Diagram(W, H, "A44", "Рассчитать страховую выплату", "4")
    bw, bh = 250, 130
    C1 = d.add_box("A441", 170, 140, bw, bh, "Определить\nостаток страховой\nсуммы", "A441")
    C2 = d.add_box("A442", 520, 330, bw, bh, "Учесть\nфраншизу", "A442")
    C3 = d.add_box("A443", 870, 520, bw, bh, "Ограничить\nвыплату остатком\nстраховой суммы", "A443")
    C4 = d.add_box("A444", 1220, 710, bw, bh, "Зафиксировать\nвыплату и обновить\nстатус договора", "A444")
    R = W - 2
    x, y = C1.side("L", 0.5); d.arrow([(2, y), (x, y)], "I2 Данные договора", (14, y - 10))
    x, y = C2.side("L", 0.5); d.arrow([(2, y), (x, y)], "I1 Размер ущерба", (14, y - 10))
    vbus(d, 2, C2.side("T", 0.5)[0], C2.y, 90, [(C3.side("T", 0.5)[0], C3.y)],
         "C1 Правила страхования (франшиза, лимиты)", (C2.side("T", 0.5)[0] + 8, 26))
    x0, y0 = C1.side("R", 0.5); x1, y1 = C3.side("L", 0.25); cx = C2.x + bw + 40
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Остаток страховой суммы", (x0 + 8, y0 - 10))
    x0, y0 = C2.side("R", 0.5); x1, y1 = C3.side("L", 0.65); cx = C2.x + bw + 20
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Ущерб за вычетом\nфраншизы", (cx - 6, C2.y + bh + 70), "end")
    x0, y0 = C3.side("R", 0.5); x1, y1 = C4.side("L", 0.4); cx = (x0 + x1) / 2
    d.arrow([(x0, y0), (cx, y0), (cx, y1), (x1, y1)], "Расчётная сумма\nвыплаты", (x0 + 8, y0 - 33))
    x0, y0 = C4.side("R", 0.3); d.arrow([(x0, y0), (R, y0)], "O1 Сумма страховой\nвыплаты", (R - 175, y0 - 33))
    x0, y0 = C4.side("R", 0.75); d.arrow([(x0, y0), (R, y0)], "O2 Статус договора", (R - 175, y0 + 24))
    yb = H - 60
    vbus(d, yb, C4.side("B", 0.4)[0], C4.y + bh, 950,
         [(C1.side("B", 0.5)[0], C1.y + bh), (C2.side("B", 0.5)[0], C2.y + bh), (C3.side("B", 0.5)[0], C3.y + bh)],
         "M2 ПС «Страховой сервис»", (C4.side("B", 0.4)[0] - 8, yb - 12), anchor="end")
    x, y = C4.side("B", 0.85); d.arrow([(x, yb), (x, C4.y + bh)], "M1 Страховой агент", (x + 8, yb - 12))
    to_files(d, "idef0_A44")


if __name__ == "__main__":
    a_minus_0(); a0(); a4(); a44()
    print("ok")
