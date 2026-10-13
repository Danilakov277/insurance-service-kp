"""Схемы алгоритмов по ГОСТ 19.701-90 (SVG -> PNG / встраивание в чертёж).

Символы: пуск-останов (terminator), процесс, решение, данные (ввод-вывод),
предопределённый процесс, соединитель. Линии потока без стрелок сверху вниз и
слева направо; стрелки — при обратном направлении и на слияниях.
"""
import sys
from html import escape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from render import render

FS = 15          # шрифт, px
LH = 18          # межстрочный интервал
SW = 1.6         # толщина линий


class Chart:
    def __init__(self, w, h):
        self.W, self.H = w, h
        self.items = []
        self.blocks = {}

    # ------------------------------------------------------------- геометрия
    def _add(self, key, kind, cx, cy, w, h, text):
        self.blocks[key] = (kind, cx, cy, w, h, text)
        return key

    def term(self, key, cx, cy, text, w=150, h=40):
        return self._add(key, "term", cx, cy, w, h, text)

    def proc(self, key, cx, cy, text, w=230, h=60):
        return self._add(key, "proc", cx, cy, w, h, text)

    def pred(self, key, cx, cy, text, w=230, h=60):
        return self._add(key, "pred", cx, cy, w, h, text)

    def io(self, key, cx, cy, text, w=250, h=60):
        return self._add(key, "io", cx, cy, w, h, text)

    def dec(self, key, cx, cy, text, w=250, h=84):
        return self._add(key, "dec", cx, cy, w, h, text)

    def conn(self, key, cx, cy, text, r=14):
        return self._add(key, "conn", cx, cy, 2 * r, 2 * r, text)

    def top(self, k):
        _, cx, cy, w, h, _ = self.blocks[k]
        return cx, cy - h / 2

    def bottom(self, k):
        _, cx, cy, w, h, _ = self.blocks[k]
        return cx, cy + h / 2

    def left(self, k):
        kind, cx, cy, w, h, _ = self.blocks[k]
        inset = 14 if kind == "io" else 0
        return cx - w / 2 + inset / 2, cy

    def right(self, k):
        kind, cx, cy, w, h, _ = self.blocks[k]
        inset = 14 if kind == "io" else 0
        return cx + w / 2 - inset / 2, cy

    # ------------------------------------------------------------- линии
    def line(self, pts, arrow=False, label=None, lpos=None, anchor="start"):
        self.items.append(("line", pts, arrow, label, lpos, anchor))

    def down(self, a, b, label=None):
        (x1, y1), (x2, y2) = self.bottom(a), self.top(b)
        if abs(x1 - x2) < 0.5:
            self.line([(x1, y1), (x2, y2)], label=label, lpos=(x1 + 6, y1 + 15) if label else None)
        else:
            my = (y1 + y2) / 2
            self.line([(x1, y1), (x1, my), (x2, my), (x2, y2)], label=label, lpos=(x1 + 6, y1 + 15) if label else None)

    def side(self, a, b, label=None):
        """Из правой вершины решения a вправо к блоку b (вход сверху или слева)."""
        x1, y1 = self.right(a)
        kind, bx, by, bw, bh, _ = self.blocks[b]
        if abs(by - y1) < 1:
            x2, y2 = self.left(b)
            self.line([(x1, y1), (x2, y2)], label=label, lpos=(x1 + 8, y1 - 7))
        else:
            x2, y2 = self.top(b)
            self.line([(x1, y1), (x2, y1), (x2, y2)], label=label, lpos=(x1 + 8, y1 - 7))

    def dot(self, x, y):
        self.items.append(("dot", x, y))

    def text(self, x, y, s, anchor="start", size=FS, weight="normal"):
        self.items.append(("text", x, y, s, anchor, size, weight))

    # ------------------------------------------------------------- вывод
    def _text_block(self, cx, cy, text, size=FS):
        lines = text.split("\n")
        y0 = cy - (len(lines) - 1) * LH / 2 + size * 0.35
        return "".join(f'<text x="{cx:.1f}" y="{y0 + i * LH:.1f}" font-size="{size}" text-anchor="middle">'
                       f'{escape(l)}</text>' for i, l in enumerate(lines))

    def svg(self, inner_only=False):
        st = f'fill="#fff" stroke="#000" stroke-width="{SW}"'
        out = []
        for it in self.items:
            if it[0] == "line":
                _, pts, arrow, label, lpos, anchor = it
                d = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
                mk = ' marker-end="url(#fa)"' if arrow else ""
                out.append(f'<polyline points="{d}" fill="none" stroke="#000" stroke-width="{SW}"{mk}/>')
                if label:
                    x, y = lpos
                    out.append(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{FS - 1}" text-anchor="{anchor}">{escape(label)}</text>')
            elif it[0] == "dot":
                out.append(f'<circle cx="{it[1]}" cy="{it[2]}" r="3" fill="#000"/>')
            elif it[0] == "text":
                _, x, y, s, anchor, size, weight = it
                out.append(f'<text x="{x}" y="{y}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}">{escape(s)}</text>')
        for key, (kind, cx, cy, w, h, text) in self.blocks.items():
            x0, y0 = cx - w / 2, cy - h / 2
            if kind == "term":
                out.append(f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="{h / 2}" ry="{h / 2}" {st}/>')
            elif kind == "proc":
                out.append(f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" {st}/>')
            elif kind == "pred":
                out.append(f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" {st}/>')
                out.append(f'<line x1="{x0 + 14}" y1="{y0}" x2="{x0 + 14}" y2="{y0 + h}" stroke="#000" stroke-width="{SW}"/>')
                out.append(f'<line x1="{x0 + w - 14}" y1="{y0}" x2="{x0 + w - 14}" y2="{y0 + h}" stroke="#000" stroke-width="{SW}"/>')
            elif kind == "io":
                k = 14
                out.append(f'<polygon points="{x0 + k},{y0} {x0 + w},{y0} {x0 + w - k},{y0 + h} {x0},{y0 + h}" {st}/>')
            elif kind == "dec":
                out.append(f'<polygon points="{cx},{y0} {x0 + w},{cy} {cx},{y0 + h} {x0},{cy}" {st}/>')
            elif kind == "conn":
                out.append(f'<circle cx="{cx}" cy="{cy}" r="{w / 2}" {st}/>')
            out.append(self._text_block(cx, cy, text, FS - 1 if kind == "dec" else FS))
        body = "\n".join(out)
        if inner_only:
            return body
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.W}" height="{self.H}" viewBox="0 0 {self.W} {self.H}">'
                '<defs><marker id="fa" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="10" markerHeight="10" '
                'orient="auto" markerUnits="userSpaceOnUse"><path d="M0,1.5 L10,5 L0,8.5 z" fill="#000"/></marker></defs>'
                f'<rect width="{self.W}" height="{self.H}" fill="#fff"/>{body}</svg>')


# ======================================================================= алгоритм 1
def premium_chart():
    c = Chart(780, 1330)
    X, R = 240, 630          # основная и правая колонки
    y = 30
    c.term("b", X, y, "Начало")
    y += 70
    c.io("in", X, y, "Ввод: вид страхования, С, m,\nфраншиза f, число взносов n,\nстрахователь, дата начала", w=290, h=70)
    y += 92
    c.pred("val", X, y, "Проверить вид, С, m, f, n\n(validate)", w=250, h=56)
    y += 88
    c.dec("ok", X, y, "Параметры\nкорректны?")
    c.io("err", R, y + 88, "Вывод сообщения\nоб ошибке", w=230, h=56)
    y += 108
    c.proc("t", X, y, "Т = базовый тариф вида;\nвозраст = полных лет\nна дату начала", h=66)
    y += 92
    c.proc("kv", X, y, "Кв = 1,20 (до 25 лет),\n1,35 (старше 60 лет),\nиначе 1,00", h=66)
    y += 84
    c.proc("ks", X, y, "Кс = m / 12", h=44)
    y += 78
    c.dec("short", X, y, "m < 12 ?")
    c.proc("ks2", X + 245, y, "Кс = Кс × 1,1", w=150, h=44)
    y += 104
    c.pred("k", X, y, "k = число выплат клиенту\nза 2 года (запрос к БД)", w=250, h=56)
    y += 84
    c.proc("kbm", X, y, "Кбм = 0,90 (k = 0), 1,00 (k = 1),\n1,25 (k ≥ 2)", w=270, h=52)
    y += 80
    c.proc("kf", X, y, "Кф = 1,00 / 0,95 / 0,90 / 0,80\nпри f = 0 / 1 / 2 / 5 %", w=270, h=52)
    y += 84
    c.pred("kt", X, y, "Кт = стратегия вида страхования\n(PremiumStrategyFactory)", w=270, h=56)
    y += 86
    c.proc("p", X, y, "П = С × Т × Кв × Кс × Кбм × Кф × Кт;\nокруглить П до 0,01 BYN", w=300, h=56)
    y += 88
    c.io("out", X, y, "Вывод: П, франшиза,\nзначения коэффициентов", w=280, h=56)
    y += 78
    c.term("e", X, y, "Конец")

    for a, b in [("b", "in"), ("in", "val"), ("val", "ok"), ("t", "kv"), ("kv", "ks"), ("ks", "short"),
                 ("k", "kbm"), ("kbm", "kf"), ("kf", "kt"), ("kt", "p"), ("p", "out")]:
        c.down(a, b)
    c.down("ok", "t", label="да")
    c.side("ok", "err", label="нет")
    # m < 12: «да» вправо, «нет» вниз
    c.side("short", "ks2", label="да")
    xb, yb = c.bottom("short")
    yk = c.top("k")[1]
    c.line([(xb, yb), (xb, yk)], label="нет", lpos=(xb - 6, yb + 15), anchor="end")
    x2, y2 = c.bottom("ks2")
    ym = yk - 14
    c.line([(x2, y2), (x2, ym), (xb, ym)], arrow=True)
    c.dot(xb, ym)
    # ошибка -> конец
    ex, ey = c.bottom("err")
    tx, ty = c.top("e")
    yj = ty - 14
    c.line([(ex, ey), (ex, yj), (tx, yj)], arrow=True)
    xo, yo = c.bottom("out")
    c.line([(xo, yo), (xo, ty)])
    c.dot(tx, yj)
    return c


# ======================================================================= алгоритм 2
def payout_chart(compact=False):
    """compact=True — плотная компоновка для чертежа формата А4 (меньше вертикальных промежутков)."""
    k = 0.86 if compact else 1.0
    dy = lambda v: round(v * k)
    c = Chart(780, 1330)
    X, R = 230, 580
    y = 30
    c.term("b", X, y, "Начало")
    y += dy(72)
    c.io("in", X, y, "Ввод: договор, график взносов,\nзаявление, оценённый ущерб D,\nвыплаты по договору W", w=300, h=70)
    decs = [
        ("d1", "Дата события\nв периоде действия\nдоговора?", "нет", "r1", "Причина: событие вне\nпериода действия"),
        ("d2", "Договор расторгнут\nдо даты события?", "да", "r2", "Причина: договор\nрасторгнут"),
        ("d3", "Есть просроченные\nвзносы на дату\nсобытия?", "да", "r3", "Причина: просрочены\nстраховые взносы"),
        ("d4", "Заявление подано\nне позднее\n30 дней?", "нет", "r4", "Причина: пропущен\nсрок подачи"),
    ]
    dw, dh = (290, 86) if compact else (260, 96)
    y += dy(108)
    prev = "in"
    for key, text, fail, rkey, rtext in decs:
        c.dec(key, X, y, text, w=dw, h=dh)
        c.proc(rkey, R, y, rtext, w=210, h=52)
        c.down(prev, key, label=None if prev == "in" else ("да" if decs[[d[0] for d in decs].index(prev)][2] == "нет" else "нет"))
        c.side(key, rkey, label=fail)
        prev = key
        y += dy(118)
    y -= dy(14)
    c.proc("f", X, y, "Ф = франшиза договора", h=44)
    c.down("d4", "f", label="да")
    y += dy(86)
    c.dec("d5", X, y, "D > Ф ?", w=220, h=80)
    c.proc("r5", R, y, "Причина: ущерб не\nпревышает франшизу", w=210, h=52)
    c.down("f", "d5")
    c.side("d5", "r5", label="нет")
    y += dy(100)
    c.proc("bw", X, y, "B = D − Ф;\nR = С − W", h=52)
    c.down("d5", "bw", label="да")
    y += dy(92)
    c.dec("d6", X, y, "R > 0 ?", w=220, h=80)
    c.proc("r6", R, y, "Причина: страховая\nсумма исчерпана", w=210, h=52)
    c.down("bw", "d6")
    c.side("d6", "r6", label="нет")
    y += dy(112)
    c.dec("d7", X, y, "B ≥ R ?", w=220, h=80)
    c.down("d6", "d7", label="да")
    c.proc("lim", X + 250, y + dy(70), "B = R; договор\nисполнен", w=170, h=50)
    c.side("d7", "lim", label="да")
    y += dy(130)
    c.proc("rnd", X, y, "Округлить B\nдо 0,01 BYN", h=52)
    xb, yb = c.bottom("d7")
    yr = c.top("rnd")[1]
    c.line([(xb, yb), (xb, yr)], label="нет", lpos=(xb + 6, yb + 15))
    lx, ly = c.bottom("lim")
    ym = yr - 16 * k
    c.line([(lx, ly), (lx, ym), (xb, ym)], arrow=True)
    c.dot(xb, ym)
    y += dy(84)
    c.io("ok", X, y, "Вывод: решение\n«выплата B»", w=240, h=52)
    c.down("rnd", "ok")
    # шина отказов
    BUS = R + 105 + 30
    for kk in ["r1", "r2", "r3", "r4", "r5", "r6"]:
        rx, ry = c.right(kk)
        c.line([(rx, ry), (BUS, ry)])
        if kk != "r1":
            c.dot(BUS, ry)
    ry1 = c.right("r1")[1]
    y_rej = c.blocks["ok"][2]
    c.io("rej", R + 10, y_rej, "Вывод: решение\n«отказ», причина", w=230, h=52)
    rjx, rjy = c.top("rej")
    c.line([(BUS, ry1), (BUS, rjy - 24 * k), (rjx, rjy - 24 * k), (rjx, rjy)])
    y += dy(84)
    c.term("e", X, y, "Конец")
    tx, ty = c.top("e")
    c.line([c.bottom("ok"), (tx, ty)])
    jx, jy = c.bottom("rej")
    yj = ty - 14 * k
    c.line([(jx, jy), (jx, yj), (tx, yj)], arrow=True)
    c.dot(tx, yj)
    c.H = int(y + 40)
    return c


if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "/home/claude/kp/diagrams")
    for name, ch in [("flow_premium", premium_chart()), ("flow_payout", payout_chart())]:
        svg = ch.svg()
        (out / f"{name}.svg").write_text(svg, encoding="utf8")
        render(svg, ch.W, ch.H, png=str(out / f"{name}.png"), scale=2)
        print(name, ch.W, ch.H)
