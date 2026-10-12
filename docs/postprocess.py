"""Two-pass finishing of the ПЗ: static-but-updatable TOC with real page numbers, abstract counters."""
import json, re, subprocess, sys, zipfile, shutil, os
from pathlib import Path
from xml.sax.saxutils import escape

HERE = Path(__file__).parent
content = json.loads((HERE / "content.json").read_text(encoding="utf8"))
docx_in, docx_out = sys.argv[1], sys.argv[2]

def heads():
    out = []
    for b in content["body"]:
        if b["t"] == "h1" and b.get("text") in ("СОДЕРЖАНИЕ",):
            continue
        if b["t"] == "h1":
            out.append((1, b.get("num"), b["text"]))
        elif b["t"] == "h2":
            out.append((2, b["num"], b["text"]))
    return out

def sentence(t):
    if t.isupper():
        t = t[0] + t[1:].lower()
    return t

def toc_xml(entries):
    TW = 9354  # 16.5 cm in twips
    paras = []
    for i, (lvl, num, text, page) in enumerate(entries):
        label = (f"{num} " if num else "") + sentence(text)
        left = 0 if lvl == 1 else 284
        hang = 0
        if num and lvl == 1: left, hang = 284, 284
        if num and lvl == 2: left, hang = 851, 567
        r = '<w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/><w:sz w:val="28"/></w:rPr>'
        begin = ""
        if i == 0:
            begin = (f'<w:r>{r}<w:fldChar w:fldCharType="begin"/></w:r><w:r>{r}<w:instrText xml:space="preserve"> TOC \\o "1-2" \\h \\z \\u </w:instrText></w:r>'
                     f'<w:r>{r}<w:fldChar w:fldCharType="separate"/></w:r>')
        end = f'<w:r>{r}<w:fldChar w:fldCharType="end"/></w:r>' if i == len(entries) - 1 else ""
        paras.append(
            f'<w:p><w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="{TW}"/></w:tabs>'
            f'<w:spacing w:after="0" w:line="360" w:lineRule="exact"/><w:ind w:left="{left}" w:hanging="{hang}"/><w:jc w:val="left"/></w:pPr>'
            f'{begin}<w:r>{r}<w:t xml:space="preserve">{escape(label)}</w:t></w:r><w:r>{r}<w:tab/></w:r>'
            f'<w:r>{r}<w:t>{page}</w:t></w:r>{end}</w:p>')
    return "".join(paras)

def patch(src, dst, entries, counters):
    tmp = Path(dst + ".dir")
    shutil.rmtree(tmp, ignore_errors=True)
    with zipfile.ZipFile(src) as z:
        z.extractall(tmp)
    doc = (tmp / "word/document.xml").read_text(encoding="utf8")
    m = re.search(r"<w:p>(?:(?!<w:p>).)*?%%TOC%%.*?</w:p>", doc, re.S)
    if not m:
        m = re.search(r"<w:p [^>]*>(?:(?!<w:p[ >]).)*?%%TOC%%.*?</w:p>", doc, re.S)
    doc = doc[:m.start()] + toc_xml(entries) + doc[m.end():]
    for k, v in counters.items():
        doc = doc.replace("{" + k + "}", str(v))
    doc = doc.replace(", 0 прил.", "")
    (tmp / "word/document.xml").write_text(doc, encoding="utf8")
    if os.path.exists(dst): os.remove(dst)
    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(tmp.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(tmp).as_posix())
    shutil.rmtree(tmp)

def to_pdf(path):
    outdir = str(Path(path).parent)
    subprocess.run(["soffice", "--headless", "--convert-to", "pdf", "--outdir", outdir, path],
                   check=True, capture_output=True, timeout=300)
    return str(Path(path).with_suffix(".pdf"))

def page_texts(pdf):
    n = int(re.search(r"Pages:\s+(\d+)", subprocess.run(["pdfinfo", pdf], capture_output=True, text=True).stdout).group(1))
    pages = []
    for i in range(1, n + 1):
        t = subprocess.run(["pdftotext", "-f", str(i), "-l", str(i), "-layout", pdf, "-"], capture_output=True, text=True).stdout
        pages.append(re.sub(r"\s+", " ", t))
    return pages

H = heads()
nfig = sum(1 for b in content["body"] if b["t"] == "fig")
ntab = sum(1 for b in content["body"] if b["t"] == "tbl")
nref = sum(1 for b in content["body"] if b["t"] == "where" and b["text"].startswith("["))
napp = sum(1 for b in content["body"] if b["t"] == "h1" and b.get("text", "").startswith("ПРИЛОЖЕНИЕ"))

# pass 1: dummy numbers of the same width
pass1 = docx_out + ".p1.docx"
patch(docx_in, pass1, [(l, n, t, "99") for l, n, t in H], {"PAGES": 99, "FIGS": nfig, "TABLES": ntab, "REFS": nref, "APPS": napp})
pages = page_texts(to_pdf(pass1))
toc_page = next(i for i, t in enumerate(pages) if "СОДЕРЖАНИЕ" in t)
res = []
for lvl, num, text in H:
    key = ((num + " ") if num else "") + text
    key = re.sub(r"\s+", " ", key)[:45]
    found = None
    for i in range(toc_page + 1, len(pages)):
        if key in pages[i]:
            found = i + 1
            break
    if found is None:
        print("NOT FOUND:", key)
        found = "?"
    res.append((lvl, num, text, found))
patch(docx_in, docx_out, res, {"PAGES": len(pages), "FIGS": nfig, "TABLES": ntab, "REFS": nref, "APPS": napp})
os.remove(pass1); os.remove(pass1[:-5] + ".pdf")
print("pages", len(pages), "figs", nfig, "tables", ntab)
for r in res: print(r[3], r[1] or "", r[2][:60])
