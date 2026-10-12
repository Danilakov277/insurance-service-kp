"""Minimal IDEF0 diagram renderer producing SVG.

Arrows are orthogonal polylines given by explicit points (helpers below build them).
Coordinates are in px; diagram font is Times New Roman.
"""
from dataclasses import dataclass, field
from html import escape

FS = 17          # label font size
BFS = 19         # box title font size
LH = 20          # line height for labels


@dataclass
class Box:
    id: str
    x: float
    y: float
    w: float
    h: float
    title: str
    num: str

    def side(self, s: str, f: float):
        """Point on side s ('L','R','T','B') at fraction f (0..1, top->bottom or left->right)."""
        if s == "L":
            return (self.x, self.y + self.h * f)
        if s == "R":
            return (self.x + self.w, self.y + self.h * f)
        if s == "T":
            return (self.x + self.w * f, self.y)
        if s == "B":
            return (self.x + self.w * f, self.y + self.h)
        raise ValueError(s)


@dataclass
class Arrow:
    pts: list
    label: str = ""
    lpos: tuple | None = None     # (x, y) of label's first line baseline
    anchor: str = "start"
    head: bool = True
    squiggle: bool = False


@dataclass
class Diagram:
    W: int
    H: int
    node: str
    title: str
    number: str
    boxes: list = field(default_factory=list)
    arrows: list = field(default_factory=list)
    dots: list = field(default_factory=list)   # branch junction points
    footer: bool = True

    def add_box(self, *a, **k):
        b = Box(*a, **k)
        self.boxes.append(b)
        return b

    def arrow(self, pts, label="", lpos=None, anchor="start", head=True):
        self.arrows.append(Arrow([tuple(p) for p in pts], label, lpos, anchor, head))

    # ---------- svg ----------
    def _text(self, x, y, s, size=FS, anchor="start", weight="normal"):
        out = []
        for i, line in enumerate(s.split("\n")):
            out.append(f'<text x="{x:.1f}" y="{y + i * (size + 3):.1f}" font-size="{size}" '
                       f'text-anchor="{anchor}" font-weight="{weight}">{escape(line)}</text>')
        return "".join(out)

    def svg(self) -> str:
        W, H = self.W, self.H
        body_h = H - (60 if self.footer else 0)
        p = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
             '<defs><marker id="ah" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="11" markerHeight="11" '
             'orient="auto-start-reverse" markerUnits="userSpaceOnUse"><path d="M0,1.5 L10,5 L0,8.5 z" fill="#000"/></marker></defs>',
             f'<rect x="1" y="1" width="{W - 2}" height="{body_h - 2}" fill="#fff" stroke="#000" stroke-width="1.5"/>']
        for a in self.arrows:
            d = " ".join(f"{x:.1f},{y:.1f}" for x, y in a.pts)
            mk = ' marker-end="url(#ah)"' if a.head else ""
            p.append(f'<polyline points="{d}" fill="none" stroke="#000" stroke-width="1.6"{mk}/>')
            if a.label:
                if a.lpos:
                    lx, ly = a.lpos
                else:
                    # longest segment midpoint
                    best = max(range(len(a.pts) - 1),
                               key=lambda i: abs(a.pts[i + 1][0] - a.pts[i][0]) + abs(a.pts[i + 1][1] - a.pts[i][1]))
                    (x1, y1), (x2, y2) = a.pts[best], a.pts[best + 1]
                    lx, ly = (x1 + x2) / 2 + (6 if x1 == x2 else 0), (y1 + y2) / 2 - (8 if y1 == y2 else 0)
                p.append(self._text(lx, ly, a.label, anchor=a.anchor))
        for x, y in self.dots:
            p.append(f'<circle cx="{x}" cy="{y}" r="3" fill="#000"/>')
        for b in self.boxes:
            p.append(f'<rect x="{b.x}" y="{b.y}" width="{b.w}" height="{b.h}" fill="#fff" stroke="#000" stroke-width="2"/>')
            lines = b.title.split("\n")
            ty = b.y + b.h / 2 - (len(lines) - 1) * (BFS + 3) / 2 + BFS * 0.35
            p.append(self._text(b.x + b.w / 2, ty, b.title, size=BFS, anchor="middle"))
            p.append(self._text(b.x + b.w - 6, b.y + b.h - 6, b.num, size=FS, anchor="end", weight="bold"))
        if self.footer:
            y0 = body_h
            p.append(f'<rect x="1" y="{y0}" width="{W - 2}" height="58" fill="#fff" stroke="#000" stroke-width="1.5"/>')
            c1, c2 = 230, W - 230
            p.append(f'<line x1="{c1}" y1="{y0}" x2="{c1}" y2="{H - 1}" stroke="#000" stroke-width="1.5"/>')
            p.append(f'<line x1="{c2}" y1="{y0}" x2="{c2}" y2="{H - 1}" stroke="#000" stroke-width="1.5"/>')
            p.append(self._text(10, y0 + 18, "УЗЕЛ:", size=13))
            p.append(self._text(c1 / 2, y0 + 44, self.node, size=22, anchor="middle", weight="bold"))
            p.append(self._text(c1 + 10, y0 + 18, "НАЗВАНИЕ:", size=13))
            p.append(self._text((c1 + c2) / 2, y0 + 44, self.title, size=22, anchor="middle", weight="bold"))
            p.append(self._text(c2 + 10, y0 + 18, "НОМЕР:", size=13))
            p.append(self._text((c2 + W) / 2, y0 + 44, self.number, size=22, anchor="middle", weight="bold"))
        p.append("</svg>")
        return "\n".join(p)
