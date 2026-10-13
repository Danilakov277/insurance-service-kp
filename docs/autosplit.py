"""Перенос таблиц по СТП 01-2024: таблица, не поместившаяся на странице, делится на части,
и над каждой следующей частью пишется «Продолжение таблицы N».

Положение строк определяется по реальной вёрстке: документ собирается build.js, открывается
в LibreOffice (UNO, без окна) и для каждой строки таблицы запрашивается номер страницы.
За один проход делится только первая разорвавшаяся таблица (деление сдвигает всё, что ниже),
поэтому проходы повторяются до тех пор, пока разрывов не останется.

usage: python3 autosplit.py content.json raw.docx
"""
import copy
import json
import os
import subprocess
import sys
import time
from pathlib import Path

import uno
from com.sun.star.beans import PropertyValue

HERE = Path(__file__).parent
content_path = Path(sys.argv[1] if len(sys.argv) > 1 else HERE / "content.json")
docx_path = Path(sys.argv[2] if len(sys.argv) > 2 else HERE / "raw.docx").resolve()
PORT = 2083


def pv(name, value):
    p = PropertyValue()
    p.Name, p.Value = name, value
    return p


def start_office():
    profile = Path("/tmp/claude-0/lo_autosplit_profile")
    proc = subprocess.Popen(["soffice", "--headless", "--invisible", "--norestore", "--nologo",
                             f"-env:UserInstallation=file://{profile}",
                             f"--accept=socket,host=127.0.0.1,port={PORT};urp;"],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", local)
    for _ in range(120):
        try:
            ctx = resolver.resolve(f"uno:socket,host=127.0.0.1,port={PORT};urp;StarOffice.ComponentContext")
            break
        except Exception:
            time.sleep(0.5)
    else:
        raise RuntimeError("LibreOffice не запустился")
    desktop = ctx.ServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    return proc, desktop


def probe(desktop, path):
    """Для каждой таблицы документа (в порядке следования): текст первой ячейки заголовка
    и номера страниц всех строк."""
    doc = desktop.loadComponentFromURL(uno.systemPathToFileUrl(str(path)), "_blank", 0,
                                       (pv("Hidden", True), pv("ReadOnly", True)))
    try:
        vc = doc.getCurrentController().getViewCursor()
        tables = doc.getTextTables()
        res = []
        for i in range(tables.getCount()):
            t = tables.getByIndex(i)
            rows = t.getRows().getCount()
            pages = []
            for r in range(rows):
                cell = t.getCellByPosition(0, r)
                vc.gotoRange(cell.getStart(), False)
                pages.append(vc.getPage())
            first = t.getCellByPosition(0, 0).getString()
            res.append({"name": t.getName(), "head": first, "pages": pages})
        # порядок следования: по странице первой строки, затем по номеру в имени (Table12)
        def order(x):
            digits = "".join(ch for ch in x["name"] if ch.isdigit())
            return (x["pages"][0], int(digits or 0))
        res.sort(key=order)
        return res
    finally:
        doc.close(True)


def pages_of(desktop, path, strings):
    """Номера страниц, на которых найдены строки (поиск с учётом регистра)."""
    doc = desktop.loadComponentFromURL(uno.systemPathToFileUrl(str(path)), "_blank", 0,
                                       (pv("Hidden", True), pv("ReadOnly", True)))
    try:
        vc = doc.getCurrentController().getViewCursor()
        res = {}
        for st in strings:
            d = doc.createSearchDescriptor()
            d.SearchString = st
            d.SearchCaseSensitive = True
            found = doc.findFirst(d)
            if found is None:
                raise RuntimeError(f"не найдено: {st}")
            vc.gotoRange(found.getStart(), False)
            res[st] = vc.getPage()
        return res
    finally:
        doc.close(True)


MOVABLE = {"p", "pl", "li", "ol", "where", "formula"}


def float_landscape(content, desktop):
    """Рисунок на альбомной странице выделяется в отдельный раздел, и текст перед ним обрывается,
    оставляя пустую часть книжной страницы. Абзацы, следующие за таким рисунком, по одному переносятся
    перед ним, пока они помещаются на ту же страницу (рисунок при этом остаётся после ссылки на него,
    как требует СТП). Через заголовки, другие рисунки и таблицы абзацы не переносятся."""
    body = content["body"]
    captions = [b["caption"].split(" – ")[0] + " –" for b in body if b["t"] == "fig" and b.get("landscape")]
    moved = 0
    for cap in captions:
        find = lambda: next(i for i, b in enumerate(body) if b["t"] == "fig" and b["caption"].startswith(cap))
        j = find()
        if j + 1 >= len(body) or body[j + 1]["t"] not in MOVABLE:
            continue
        build(content)
        base = pages_of(desktop, docx_path, [cap])[cap]
        while True:
            j = find()
            if j + 1 >= len(body) or body[j + 1]["t"] not in MOVABLE:
                break
            body[j], body[j + 1] = body[j + 1], body[j]
            build(content)
            if pages_of(desktop, docx_path, [cap])[cap] == base:
                moved += 1
                continue
            body[j], body[j + 1] = body[j + 1], body[j]
            break
    print(f"float: перенесено абзацев перед альбомными рисунками: {moved}")


def build(content):
    content_path.write_text(json.dumps(content, ensure_ascii=False, indent=1), encoding="utf8")
    subprocess.run(["node", str(HERE / "build.js"), str(content_path), str(docx_path)], check=True,
                   stdout=subprocess.DEVNULL)


def main():
    content = json.loads(content_path.read_text(encoding="utf8"))
    proc, desktop = start_office()
    try:
        float_landscape(content, desktop)
        for it in range(60):
            build(content)
            tables = probe(desktop, docx_path)
            blocks = [(i, b) for i, b in enumerate(content["body"]) if b["t"] == "tbl"]
            # таблицы с пустой первой ячейкой — служебные (титульный лист, повёрнутые рисунки)
            body_tables = [t for t in tables if t["head"].strip()]
            assert len(body_tables) == len(blocks), (len(tables), len(blocks))
            split = None
            for (idx, b), t in zip(blocks, body_tables):
                head = str(b["header"][0]).split("\n")[0]
                if not t["head"].startswith(head[:10]):
                    raise RuntimeError(f"таблица {b['num']}: ожидался заголовок «{head}», в документе «{t['head']}»")
                data_pages = t["pages"][1:]
                for k, pg in enumerate(data_pages):
                    if pg != data_pages[0]:
                        split = (idx, k)
                        break
                if split:
                    break
            if not split:
                print(f"autosplit: проходов {it + 1}, разрывов нет")
                return
            idx, k = split
            b = content["body"][idx]
            if k == 0:
                raise RuntimeError(f"таблица {b['num']}: первая строка не помещается на страницу")
            if k == 1 and not b.get("cont") and not b.get("newPage"):
                # на странице поместилась бы одна строка — таблица целиком переносится на следующую страницу
                b["newPage"] = True
                print(f"autosplit: таблица {b['num']} перенесена на новую страницу")
                continue
            rest = copy.deepcopy(b)
            b["rows"], rest["rows"] = b["rows"][:k], b["rows"][k:]
            rest["cont"] = True
            rest["caption"] = f"Продолжение таблицы {b['num']}"
            content["body"].insert(idx + 1, rest)
            print(f"autosplit: таблица {b['num']} разделена после строки {k}")
        raise RuntimeError("autosplit: слишком много проходов")
    finally:
        try:
            desktop.terminate()
        except Exception:
            pass
        proc.wait(timeout=30)


if __name__ == "__main__":
    main()
