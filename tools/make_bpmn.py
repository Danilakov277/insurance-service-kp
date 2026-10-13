"""Модели бизнес-процессов в нотации BPMN 2.0 (диаграммы взаимодействия с пулами и дорожками).

Пул «Клиент» и пул «Страховая компания» с дорожками «Страховой агент» и «ПС «Страховой сервис»».
Элементы: события (начальное, промежуточное, конечное; с сообщением), задачи, исключающий шлюз,
потоки управления (сплошные) и потоки сообщений (штриховые, между пулами).
Результат: diagrams/bpmn_contract.png, diagrams/bpmn_claim.png (+ .svg).
"""
import sys
from html import escape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from render import render  # noqa: E402

COLW = 124        # ширина колонки сетки
POOL_BAND = 30    # полоса названия пула
LANE_BAND = 30    # полоса названия дорожки
X0 = POOL_BAND + LANE_BAND
LANE_H = 156
GAP = 34          # промежуток между пулами (в нём идут горизонтальные участки потоков сообщений)
TW, TH = 110, 66  # задача
R = 17            # событие
GW = 25           # полудиагональ шлюза
FS = 13
SW = 1.5


class Bpmn:
    def __init__(self, cols, title=""):
        self.cols = cols
        self.W = X0 + cols * COLW + 8
        # дорожки: (пул, дорожка); клиентский пул — одна дорожка без названия
        self.lanes = {"client": 0, "agent": 1, "system": 2}
        self.lane_y = {"client": 6, "agent": 6 + LANE_H + GAP, "system": 6 + 2 * LANE_H + GAP}
        self.H = 6 + 3 * LANE_H + GAP + 6
        self.gap_y = 6 + LANE_H + GAP / 2
        self.nodes = {}
        self.out = []

    # ------------------------------------------------------------ узлы
    def node(self, key, kind, lane, col, label="", dy=0, lpos="below", marker=None):
        x = X0 + COLW * (col + 0.5)
        y = self.lane_y[lane] + LANE_H / 2 + dy
        self.nodes[key] = dict(kind=kind, x=x, y=y, label=label, lpos=lpos, marker=marker, lane=lane)
        return key

    def task(self, key, lane, col, label, dy=0):
        return self.node(key, "task", lane, col, label, dy)

    def start(self, key, lane, col, label, msg=False, lpos="below", dy=0):
        return self.node(key, "start", lane, col, label, dy, lpos, "msg" if msg else None)

    def end(self, key, lane, col, label, msg=False, lpos="below", dy=0):
        return self.node(key, "end", lane, col, label, dy, lpos, "msg" if msg else None)

    def catch(self, key, lane, col, label, lpos="above", dy=0):
        return self.node(key, "inter", lane, col, label, dy, lpos, "msg")

    def gateway(self, key, lane, col, label, lpos="above", dy=0):
        return self.node(key, "gw", lane, col, label, dy, lpos)

    def half(self, key):
        n = self.nodes[key]
        if n["kind"] == "task":
            return TW / 2, TH / 2
        if n["kind"] == "gw":
            return GW, GW
        return R, R

    def port(self, key, side, off=0):
        n = self.nodes[key]
        hw, hh = self.half(key)
        x, y = n["x"], n["y"]
        if side == "l":
            return x - hw, y + off
        if side == "r":
            return x + hw, y + off
        if side == "t":
            if n["kind"] in ("start", "end", "inter") and off:
                return x + off, y - (R * R - off * off) ** 0.5
            return x + off, y - hh
        if n["kind"] in ("start", "end", "inter") and off:
            return x + off, y + (R * R - off * off) ** 0.5
        return x + off, y + hh

    # ------------------------------------------------------------ связи
    def flow(self, a, b, label=None, sa="r", sb="l", xm=None, ym=None, lab_at=None):
        x1, y1 = self.port(a, sa)
        x2, y2 = self.port(b, sb)
        if sa == "r" and sb == "l":
            if abs(y1 - y2) < 1:
                pts = [(x1, y1), (x2, y2)]
            else:
                xm = xm if xm is not None else (x1 + x2) / 2
                pts = [(x1, y1), (xm, y1), (xm, y2), (x2, y2)]
        elif sa in "tb" and sb == "l":
            pts = [(x1, y1), (x1, y2), (x2, y2)]
        elif sa == "r" and sb in "tb":
            pts = [(x1, y1), (x2, y1), (x2, y2)]
        elif sa in "tb" and sb in "tb":
            if abs(x1 - x2) < 1:
                pts = [(x1, y1), (x2, y2)]
            else:
                pts = [(x1, y1), (x1, ym), (x2, ym), (x2, y2)]
        else:
            raise ValueError((sa, sb))
        self.out.append(("seq", pts, label, sa, lab_at))

    def message(self, a, b, oa=0, ob=0, sa=None, sb=None, via_x=None):
        na, nb = self.nodes[a], self.nodes[b]
        if via_x is not None:
            # выход из правой стороны задачи, подъём по свободному промежутку между колонками
            x1, y1 = self.port(a, "r", oa)
            down = self.lanes[na["lane"]] < self.lanes[nb["lane"]]
            x2, y2 = self.port(b, sb or ("t" if down else "b"), ob)
            ym = self.gap_y
            self.out.append(("msg", [(x1, y1), (via_x, y1), (via_x, ym), (x2, ym), (x2, y2)], None, "r", None))
            return
        down = self.lanes[na["lane"]] < self.lanes[nb["lane"]]
        sa = sa or ("b" if down else "t")
        sb = sb or ("t" if down else "b")
        x1, y1 = self.port(a, sa, oa)
        x2, y2 = self.port(b, sb, ob)
        if abs(x1 - x2) < 1:
            pts = [(x1, y1), (x2, y2)]
        else:
            ym = self.gap_y
            pts = [(x1, y1), (x1, ym), (x2, ym), (x2, y2)]
        self.out.append(("msg", pts, None, sa, None))

    # ------------------------------------------------------------ вывод
    @staticmethod
    def _lines(cx, cy, text, size=FS, anchor="middle", lh=None):
        lh = lh or size + 3
        lines = text.split("\n")
        y0 = cy - (len(lines) - 1) * lh / 2 + size * 0.35
        return "".join(f'<text x="{cx:.1f}" y="{y0 + i * lh:.1f}" font-size="{size}" text-anchor="{anchor}">'
                       f"{escape(l)}</text>" for i, l in enumerate(lines))

    def _envelope(self, x, y, filled):
        w, h = 14, 10
        fill = "#000" if filled else "#fff"
        line = "#fff" if filled else "#000"
        return (f'<rect x="{x - w / 2}" y="{y - h / 2}" width="{w}" height="{h}" fill="{fill}" stroke="#000" '
                f'stroke-width="1.1"/><polyline points="{x - w / 2},{y - h / 2} {x},{y + 1} {x + w / 2},{y - h / 2}" '
                f'fill="none" stroke="{line}" stroke-width="1.1"/>')

    def svg(self, pools):
        o = []
        st = f'stroke="#000" stroke-width="{SW}"'
        # пулы и дорожки
        for pool in pools:
            name, lanes = pool
            ys = [self.lane_y[l] for l, _ in lanes]
            top, bottom = min(ys), max(ys) + LANE_H
            o.append(f'<rect x="2" y="{top}" width="{self.W - 4}" height="{bottom - top}" fill="#fff" {st}/>')
            o.append(f'<line x1="{POOL_BAND + 2}" y1="{top}" x2="{POOL_BAND + 2}" y2="{bottom}" {st}/>')
            cx, cy = 2 + POOL_BAND / 2, (top + bottom) / 2
            o.append(f'<text x="{cx}" y="{cy}" font-size="{FS + 1}" font-weight="bold" text-anchor="middle" '
                     f'dominant-baseline="central" transform="rotate(-90 {cx} {cy})">{escape(name)}</text>')
            for i, (lane, lname) in enumerate(lanes):
                y = self.lane_y[lane]
                if i:
                    o.append(f'<line x1="{POOL_BAND + 2}" y1="{y}" x2="{self.W - 2}" y2="{y}" {st}/>')
                if lname:
                    o.append(f'<line x1="{X0 + 2}" y1="{y}" x2="{X0 + 2}" y2="{y + LANE_H}" {st}/>')
                    lx, ly = POOL_BAND + 2 + LANE_BAND / 2, y + LANE_H / 2
                    o.append(f'<text x="{lx}" y="{ly}" font-size="{FS}" text-anchor="middle" '
                             f'dominant-baseline="central" transform="rotate(-90 {lx} {ly})">{escape(lname)}</text>')
        # связи
        for kind, pts, label, sa, lab_at in self.out:
            d = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
            if kind == "seq":
                o.append(f'<polyline points="{d}" fill="none" {st} marker-end="url(#seq)"/>')
                if label:
                    (x1, y1) = pts[0]
                    if lab_at:
                        lx, ly, anc = lab_at
                    elif sa == "r":
                        lx, ly, anc = x1 + 5, y1 - 6, "start"
                    elif sa == "t":
                        lx, ly, anc = x1 + 5, y1 - 8, "start"
                    else:
                        lx, ly, anc = x1 + 5, y1 + 15, "start"
                    o.append(f'<text x="{lx:.1f}" y="{ly:.1f}" font-size="{FS - 1}" text-anchor="{anc}">'
                             f"{escape(label)}</text>")
            else:
                o.append(f'<polyline points="{d}" fill="none" stroke="#000" stroke-width="1.2" '
                         f'stroke-dasharray="6 4" marker-start="url(#mstart)" marker-end="url(#mend)"/>')
        # узлы
        for key, n in self.nodes.items():
            x, y, k = n["x"], n["y"], n["kind"]
            if k == "task":
                o.append(f'<rect x="{x - TW / 2}" y="{y - TH / 2}" width="{TW}" height="{TH}" rx="10" ry="10" '
                         f'fill="#fff" {st}/>')
                o.append(self._lines(x, y, n["label"]))
                continue
            if k == "gw":
                o.append(f'<polygon points="{x},{y - GW} {x + GW},{y} {x},{y + GW} {x - GW},{y}" fill="#fff" {st}/>')
                a = 9
                o.append(f'<path d="M{x - a},{y - a} L{x + a},{y + a} M{x + a},{y - a} L{x - a},{y + a}" '
                         f'stroke="#000" stroke-width="3"/>')
                hh = GW
            else:
                width = {"start": 1.5, "end": 3.6, "inter": 1.3}[k]
                o.append(f'<circle cx="{x}" cy="{y}" r="{R}" fill="#fff" stroke="#000" stroke-width="{width}"/>')
                if k == "inter":
                    o.append(f'<circle cx="{x}" cy="{y}" r="{R - 3.5}" fill="none" stroke="#000" stroke-width="1.3"/>')
                if n["marker"] == "msg":
                    o.append(self._envelope(x, y, filled=(k == "end")))
                hh = R
            if n["label"]:
                lines = n["label"].split("\n")
                lh = FS + 2
                if n["lpos"] == "below":
                    cy = y + hh + 6 + (len(lines) - 1) * lh / 2 + FS * 0.5
                    o.append(self._lines(x, cy, n["label"], FS - 1, lh=lh))
                elif n["lpos"] == "above":
                    cy = y - hh - 8 - (len(lines) - 1) * lh / 2
                    o.append(self._lines(x, cy, n["label"], FS - 1, lh=lh))
                elif n["lpos"] == "right":
                    o.append(self._lines(x + hh + 6, y, n["label"], FS - 1, anchor="start", lh=lh))
        defs = ('<defs><marker id="seq" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="10" markerHeight="10" '
                'orient="auto" markerUnits="userSpaceOnUse"><path d="M0,1 L10,5 L0,9 z" fill="#000"/></marker>'
                '<marker id="mend" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" '
                'orient="auto" markerUnits="userSpaceOnUse"><path d="M1,1.5 L11,6 L1,10.5 z" fill="#fff" '
                'stroke="#000" stroke-width="1.1"/></marker>'
                '<marker id="mstart" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="9" markerHeight="9" '
                'orient="auto" markerUnits="userSpaceOnUse"><circle cx="5" cy="5" r="3.6" fill="#fff" '
                'stroke="#000" stroke-width="1.1"/></marker></defs>')
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.W}" height="{self.H}" '
                f'viewBox="0 0 {self.W} {self.H}">{defs}<rect width="{self.W}" height="{self.H}" fill="#fff"/>'
                + "".join(o) + "</svg>")


POOLS = [("Клиент", [("client", None)]),
         ("Страховая компания", [("agent", "Страховой агент"), ("system", "ПС «Страховой сервис»")])]


def contract():
    """Заключение договора страхования."""
    b = Bpmn(11)
    # клиент
    b.start("s0", "client", 0, "Требуется\nстраховая\nзащита")
    b.task("t1", "client", 1, "Рассчитать\nстоимость\nстрахования")
    b.gateway("g1", "client", 2, "Условия\nустраивают?")
    b.end("e0", "client", 2, "Отказ от\nстрахования", lpos="right", dy=56)
    b.task("t2", "client", 3, "Подать заявку\nна страхование")
    b.catch("m1", "client", 6, "Решение\nполучено")
    b.gateway("g2", "client", 7, "Заявка\nодобрена?")
    b.end("e1", "client", 7, "Заявка\nотклонена", lpos="right", dy=56)
    b.task("t3", "client", 8, "Внести первый\nвзнос")
    b.end("e2", "client", 9, "Договор\nзаключён")
    # программное средство: расчёт по запросу клиента
    b.start("sc", "system", 0, "Запрос\nрасчёта", msg=True)
    b.task("tc", "system", 1, "Рассчитать\nпремию по тарифу\nи коэффициентам")
    b.end("ec", "system", 2, "Расчёт\nотправлен", msg=True)
    # рассмотрение заявки
    b.start("s1", "system", 3, "Заявка\nполучена", msg=True)
    b.task("t5", "agent", 4, "Рассмотреть\nзаявку,\nпроверить объект")
    b.gateway("g3", "agent", 5, "Одобрить\nзаявку?")
    b.task("t7", "agent", 6, "Отклонить\nс указанием\nпричины")
    b.end("e4", "agent", 7, "Заявка\nотклонена")
    b.task("t6", "system", 6, "Оформить\nдоговор и график\nвзносов")
    b.catch("m2", "system", 8, "Взнос\nполучен", lpos="below")
    b.task("t8", "system", 9, "Зачесть взнос,\nдоговор\n«Действует»")
    b.end("e5", "system", 10, "Договор\nдействует")
    # потоки управления
    b.flow("s0", "t1"); b.flow("t1", "g1"); b.flow("g1", "t2", "да")
    b.flow("g1", "e0", "нет", sa="b", sb="t", lab_at=(b.nodes["g1"]["x"] - 6, b.nodes["g1"]["y"] + GW + 11, "end"))
    b.flow("t2", "m1"); b.flow("m1", "g2"); b.flow("g2", "t3", "да")
    b.flow("g2", "e1", "нет", sa="b", sb="t", lab_at=(b.nodes["g2"]["x"] - 6, b.nodes["g2"]["y"] + GW + 11, "end"))
    b.flow("t3", "e2")
    b.flow("sc", "tc"); b.flow("tc", "ec")
    b.flow("s1", "t5"); b.flow("t5", "g3")
    b.flow("g3", "t7", "нет")
    b.flow("g3", "t6", "да", sa="b", sb="l")
    b.flow("t7", "e4")
    b.flow("t6", "m2"); b.flow("m2", "t8"); b.flow("t8", "e5")
    # потоки сообщений
    b.message("t1", "sc", oa=-22)
    b.message("ec", "t1", ob=22)
    b.message("t2", "s1")
    b.message("t7", "m1", oa=-6, ob=-6)
    b.message("t6", "m1", oa=-20, ob=8, via_x=b.nodes["t6"]["x"] + TW / 2 + 7)
    b.message("t3", "m2")
    return b


def claim():
    """Урегулирование страхового случая."""
    b = Bpmn(9)
    b.start("s0", "client", 0, "Наступил\nстраховой\nслучай")
    b.task("t1", "client", 1, "Подать\nзаявление\nо страховом\nслучае")
    b.catch("m1", "client", 6, "Решение\nполучено")
    b.task("t2", "client", 7, "Ознакомиться\nс решением\nи обоснованием")
    b.end("e0", "client", 8, "Случай\nурегулирован")
    b.start("s1", "system", 1, "Заявление\nполучено", msg=True)
    b.task("ts1", "system", 2, "Зарегистрировать\nзаявление")
    b.task("ta1", "agent", 3, "Осмотреть\nобъект,\nоценить ущерб D")
    b.task("ts2", "system", 4, "Проверить\nусловия,\nрассчитать\nвыплату")
    b.gateway("ga", "agent", 5, "Расчёт\nподтверждён?", lpos="right", dy=10)
    b.task("ts3", "system", 6, "Зафиксировать\nрешение,\nуменьшить\nостаток суммы")
    b.end("e1", "system", 7, "Решение\nпринято")
    b.flow("s0", "t1")
    b.flow("t1", "m1")
    b.flow("m1", "t2"); b.flow("t2", "e0")
    b.flow("s1", "ts1")
    b.flow("ts1", "ta1")
    b.flow("ta1", "ts2")
    b.flow("ts2", "ga")
    b.flow("ga", "ts3", "да", sa="b", sb="l")
    b.flow("ga", "ta1", "нет", sa="t", sb="t", ym=b.lane_y["agent"] + 18,
           lab_at=(b.nodes["ga"]["x"] - 8, b.lane_y["agent"] + 14, "end"))
    b.flow("ts3", "e1")
    b.message("t1", "s1")
    b.message("ts3", "m1")
    return b


if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "/home/claude/kp/diagrams")
    for name, d in [("bpmn_contract", contract()), ("bpmn_claim", claim())]:
        svg = d.svg(POOLS)
        (out / f"{name}.svg").write_text(svg, encoding="utf8")
        render(svg, d.W, d.H, png=str(out / f"{name}.png"), scale=2.5)
        print(name, d.W, d.H)
