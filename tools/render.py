"""Render SVG strings to PNG / PDF with headless Chromium (Playwright)."""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

FONT_CSS = "svg text{font-family:'Times New Roman','Liberation Serif',serif;}"


def _html(svg: str, w: int, h: int) -> str:
    return (f"<!doctype html><html><head><meta charset='utf-8'><style>html,body{{margin:0;padding:0;background:#fff}}"
            f"{FONT_CSS}</style></head><body>{svg}</body></html>")


def render(svg: str, w: int, h: int, png: str | None = None, pdf: str | None = None,
           scale: float = 2.0, pdf_mm: tuple[float, float] | None = None):
    with sync_playwright() as p:
        b = p.chromium.launch()
        if png:
            pg = b.new_page(viewport={"width": w, "height": h}, device_scale_factor=scale)
            pg.set_content(_html(svg, w, h))
            pg.screenshot(path=png, clip={"x": 0, "y": 0, "width": w, "height": h})
            pg.close()
        if pdf:
            pg = b.new_page(viewport={"width": w, "height": h})
            pg.set_content(_html(svg, w, h))
            wmm, hmm = pdf_mm if pdf_mm else (w * 25.4 / 96, h * 25.4 / 96)
            pg.pdf(path=pdf, width=f"{wmm}mm", height=f"{hmm}mm", print_background=True,
                   margin={"top": "0", "bottom": "0", "left": "0", "right": "0"}, prefer_css_page_size=False)
            pg.close()
        b.close()


if __name__ == "__main__":
    src = Path(sys.argv[1]).read_text(encoding="utf8")
    render(src, int(sys.argv[2]), int(sys.argv[3]), png=sys.argv[4])
