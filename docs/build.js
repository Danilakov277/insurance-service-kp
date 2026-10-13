// Сборка пояснительной записки (ПЗ) по СТП 01-2024 и шаблону кафедры ЭИ.
// usage: node build.js content.json out.docx
const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell, WidthType, AlignmentType,
  PageNumber, Footer, TabStopType, BorderStyle, VerticalAlign, LineRuleType, PageBreak, SectionType,
  TableLayoutType, PageOrientation,
} = require("docx");

const [, , contentPath, outPath] = process.argv;
const C = JSON.parse(fs.readFileSync(contentPath, "utf8"));

const FONT = "Times New Roman";
const MONO = "Courier New";
const PT = (x) => x * 2;               // полупункты
const CM = (x) => Math.round(x * 567); // twips
const LINE = { line: 360, lineRule: LineRuleType.EXACT }; // ровно 18 пт
const IND = CM(1.25);

function runs(text, extra = {}) {
  // разметка: **жирный**, __курсив__
  const out = [];
  const re = /(\*\*[^*]+\*\*|__[^_]+__)/g;
  let last = 0, m;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(new TextRun({ text: text.slice(last, m.index), font: FONT, size: PT(14), ...extra }));
    const t = m[0];
    if (t.startsWith("**")) out.push(new TextRun({ text: t.slice(2, -2), font: FONT, size: PT(14), bold: true, ...extra }));
    else out.push(new TextRun({ text: t.slice(2, -2), font: FONT, size: PT(14), italics: true, ...extra }));
    last = m.index + t.length;
  }
  if (last < text.length) out.push(new TextRun({ text: text.slice(last), font: FONT, size: PT(14), ...extra }));
  return out;
}

const P = (text, o = {}) => new Paragraph({
  children: runs(text, o.run || {}),
  alignment: o.align ?? AlignmentType.JUSTIFIED,
  indent: o.indent ?? { firstLine: IND },
  spacing: { ...LINE, before: o.before || 0, after: o.after || 0 },
  keepNext: o.keepNext, keepLines: o.keepLines, pageBreakBefore: o.pageBreakBefore,
  tabStops: o.tabStops,
});
const empty = () => new Paragraph({ children: [], spacing: { ...LINE } });

function heading(level, num, text, centered, noBreak) {
  const opts = {
    heading: level === 1 ? "Heading1" : "Heading2",
    spacing: { ...LINE, before: level === 2 ? 360 : 0, after: 360 },
    keepNext: true, keepLines: true,
    pageBreakBefore: level === 1 && !noBreak,
  };
  if (centered) {
    return new Paragraph({ ...opts, alignment: AlignmentType.CENTER, indent: { firstLine: 0 },
      children: [new TextRun({ text, font: FONT, size: PT(14), bold: true })] });
  }
  const textStart = level === 1 ? IND + CM(0.6) : IND + CM(1.0);
  return new Paragraph({
    ...opts, alignment: AlignmentType.LEFT,
    indent: { left: textStart, hanging: textStart - IND },
    tabStops: [{ type: TabStopType.LEFT, position: textStart }],
    children: [new TextRun({ text: `${num}\t${text}`, font: FONT, size: PT(14), bold: true })],
  });
}

function pngSize(buf) {
  return [buf.readUInt32BE(16), buf.readUInt32BE(20)];
}

function image(block, maxW, maxH) {
  const buf = fs.readFileSync(block.img);
  const [w, h] = pngSize(buf);
  let widthCm = Math.min(block.w || maxW, maxW);
  if (widthCm * h / w > maxH) widthCm = maxH * w / h;
  const pxW = Math.round(widthCm / 2.54 * 96), pxH = Math.round(pxW * h / w);
  return new Paragraph({
    alignment: AlignmentType.CENTER, keepNext: true,
    spacing: { before: 240, after: 120, line: 240, lineRule: LineRuleType.AUTO },
    children: [new ImageRun({ data: buf, type: "png", transformation: { width: pxW, height: pxH } })],
  });
}

function cellPara(text, bold, align) {
  return new Paragraph({
    alignment: align || AlignmentType.LEFT, spacing: { line: 240, lineRule: LineRuleType.AUTO },
    children: [new TextRun({ text, font: FONT, size: PT(12), bold })],
  });
}

function table(block) {
  const total = CM(16.5);
  const sum = block.cols.reduce((a, b) => a + b, 0);
  const widths = block.cols.map((c) => Math.round(c / sum * total));
  widths[widths.length - 1] += total - widths.reduce((a, b) => a + b, 0);
  const border = { style: BorderStyle.SINGLE, size: 6, color: "000000" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const aligns = block.align || [];
  const mk = (cells, isHeader) => new TableRow({
    tableHeader: isHeader, cantSplit: true,
    children: cells.map((t, i) => new TableCell({
      width: { size: widths[i], type: WidthType.DXA }, borders, verticalAlign: VerticalAlign.TOP,
      margins: { left: 80, right: 80, top: 20, bottom: 20 },
      children: String(t).split("\n").map((line) => cellPara(line, isHeader,
        isHeader ? AlignmentType.CENTER : (aligns[i] === "c" ? AlignmentType.CENTER
          : aligns[i] === "r" ? AlignmentType.RIGHT : undefined))),
    })),
  });
  return new Table({
    width: { size: total, type: WidthType.DXA }, columnWidths: widths, layout: TableLayoutType.FIXED,
    rows: [mk(block.header, true), ...block.rows.map((r) => mk(r, false))],
  });
}

// ---------------- титульный лист ----------------
function titlePage(t) {
  const c = (text, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { ...LINE },
    children: [new TextRun({ text, font: FONT, size: PT(14), bold: o.bold, allCaps: o.caps })] });
  const noB = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
  const nob = { top: noB, bottom: noB, left: noB, right: noB, insideHorizontal: noB, insideVertical: noB };
  const rightBlock = (lines) => new Table({
    width: { size: CM(16.5), type: WidthType.DXA }, columnWidths: [CM(7.0), CM(9.5)], borders: nob,
    layout: TableLayoutType.FIXED,
    rows: [new TableRow({ children: [
      new TableCell({ width: { size: CM(7.0), type: WidthType.DXA }, borders: nob, children: [new Paragraph("")] }),
      new TableCell({ width: { size: CM(9.5), type: WidthType.DXA }, borders: nob,
        children: lines.map(([text, small]) => new Paragraph({ spacing: { ...LINE },
          alignment: small ? AlignmentType.CENTER : AlignmentType.LEFT,
          children: [new TextRun({ text, font: FONT, size: PT(small ? 10 : 14) })] })) }),
    ] })],
  });
  return [
    c("Министерство образования Республики Беларусь"),
    c("Учреждение образования"),
    c("«Белорусский государственный университет"),
    c("информатики и радиоэлектроники»"),
    empty(),
    c(`Факультет ${t.faculty}`),
    c(`Кафедра ${t.department}`),
    c(`Дисциплина «${t.discipline}»`),
    empty(),
    rightBlock([["«К ЗАЩИТЕ ДОПУСТИТЬ»"], ["Руководитель курсового проекта"], [t.supervisorPosition],
      [`________________ ${t.supervisor}`], [`___.____.${t.year}`]]),
    empty(), empty(),
    c("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА", { bold: true }),
    c("к курсовому проекту"),
    c("на тему:"),
    ...t.topicLines.map((l) => c(l, { bold: true })),
    empty(),
    c(t.code),
    empty(), empty(),
    rightBlock([[`Выполнила студентка группы ${t.group}`], [t.student], ["____________________________"],
      ["(подпись студента)", true], [`Курсовой проект представлен на проверку ___.____.${t.year}`],
      ["____________________________"], ["(подпись студента)", true]]),
    empty(), empty(), empty(), empty(), empty(), empty(),
    c(`Минск ${t.year}`),
  ];
}

// ---------------- листинги приложений ----------------
// Курсив 10 пт с точным интервалом 12 пт; страницы режутся заранее, каждая следующая
// начинается строкой «Продолжение приложения X» (СТП 01-2024).
const LISTING_WIDTH = 76;
const FIRST_PAGE_LINES = 50;
const PAGE_LINES = 56;

function wrapCode(line) {
  const out = [];
  let s = line.replace(/\t/g, "    ");
  const indent = (s.match(/^ */) || [""])[0].length;
  while (s.length > LISTING_WIDTH) {
    let cut = s.lastIndexOf(" ", LISTING_WIDTH);
    if (cut <= indent + 10) cut = LISTING_WIDTH;
    out.push(s.slice(0, cut));
    s = " ".repeat(Math.min(indent + 8, 40)) + s.slice(cut).replace(/^ +/, "");
  }
  out.push(s);
  return out;
}

function listing(block) {
  const lines = [];
  for (const f of block.files) {
    if (lines.length) lines.push({ text: "", kind: "code" });
    lines.push({ text: f.title, kind: "title" });
    for (const l of f.text.replace(/\s+$/, "").split("\n")) for (const w of wrapCode(l)) lines.push({ text: w, kind: "code" });
  }
  const pages = [];
  let i = 0, cap = FIRST_PAGE_LINES;
  while (i < lines.length) {
    pages.push(lines.slice(i, i + cap));
    i += cap; cap = PAGE_LINES;
  }
  const out = [];
  pages.forEach((page, k) => {
    if (k > 0) {
      out.push(new Paragraph({ alignment: AlignmentType.RIGHT, pageBreakBefore: true, spacing: { ...LINE, after: 120 },
        children: [new TextRun({ text: `Продолжение приложения ${block.letter}`, font: FONT, size: PT(14), italics: true })] }));
    }
    for (const l of page) {
      out.push(new Paragraph({ spacing: { line: 240, lineRule: LineRuleType.EXACT }, keepLines: true,
        children: [new TextRun({ text: l.text || " ", font: MONO, size: PT(10), bold: l.kind === "title" })] }));
    }
  });
  return out;
}

function appendixHeading(b, isFirst) {
  return [
    new Paragraph({ heading: "Heading1", alignment: AlignmentType.CENTER, pageBreakBefore: !isFirst,
      keepNext: true, spacing: { ...LINE },
      children: [new TextRun({ text: `ПРИЛОЖЕНИЕ ${b.letter}`, font: FONT, size: PT(14), bold: true })] }),
    new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { ...LINE },
      children: [new TextRun({ text: `(${b.kind || "обязательное"})`, font: FONT, size: PT(14) })] }),
    ...b.title.split("\n").map((t, i, arr) => new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true,
      spacing: { ...LINE, after: i === arr.length - 1 ? 360 : 0 },
      children: [new TextRun({ text: t, font: FONT, size: PT(14), bold: true })] })),
  ];
}

// ---------------- тело ----------------
function body(blocks, geom, noFirstBreak) {
  const out = [];
  let first = !!noFirstBreak;
  for (let bi = 0; bi < blocks.length; bi++) {
    const b = blocks[bi], next = blocks[bi + 1];
    const isFirst = first; first = false;
    switch (b.t) {
      case "h1": out.push(heading(1, b.num, b.text, !b.num, isFirst)); break;
      case "h2": out.push(heading(2, b.num, b.text)); break;
      case "p": out.push(P(b.text, { keepNext: b.keepNext })); break;
      case "pc": out.push(P(b.text, { align: AlignmentType.CENTER, indent: { firstLine: 0 } })); break;
      case "pl": out.push(P(b.text, { align: AlignmentType.LEFT, indent: { firstLine: 0 } })); break;
      case "li": out.push(P(`– ${b.text}`)); break;
      case "ol": out.push(P(`${b.n}) ${b.text}`)); break;
      case "fig":
        out.push(image(b, geom.maxW, geom.maxH));
        out.push(P(b.caption, { align: AlignmentType.CENTER, indent: { firstLine: 0 }, after: 240, keepLines: true }));
        break;
      case "tbl":
        out.push(P(b.caption, { align: AlignmentType.LEFT, indent: { firstLine: 0 }, before: b.cont ? 0 : 240,
          keepNext: true, pageBreakBefore: !!b.cont || !!b.newPage }));
        out.push(table(b));
        // пустой абзац после таблицы; перед продолжением таблицы не нужен (иначе он уходит
        // на следующую страницу и даёт пустой лист перед «Продолжение таблицы»)
        // То же перед заголовком раздела (он и так начинается с новой страницы) и перед
        // следующей таблицей (её отделяет собственное название).
        if (!(next && ["tbl", "h1", "appendix"].includes(next.t))) {
          out.push(new Paragraph({ children: [], spacing: { line: 240, after: 120 } }));
        }
        break;
      case "formula":
        out.push(new Paragraph({ alignment: AlignmentType.LEFT, spacing: { ...LINE, before: 120, after: 120 },
          keepNext: !!b.keepNext,
          tabStops: [{ type: TabStopType.CENTER, position: CM(8.25) }, { type: TabStopType.RIGHT, position: CM(16.5) }],
          children: [new TextRun({ text: `\t${b.text}\t${b.num || ""}`, font: FONT, size: PT(14), italics: false })] }));
        break;
      case "where": out.push(P(b.text, { indent: { firstLine: 0 } })); break;
      case "code":
        for (const line of b.text.split("\n")) for (const w of wrapCode(line)) out.push(new Paragraph({
          spacing: { line: 240, lineRule: LineRuleType.EXACT }, keepLines: true,
          children: [new TextRun({ text: w || " ", font: MONO, size: PT(10) })] }));
        out.push(new Paragraph({ children: [], spacing: { line: 240, after: 120 } }));
        break;
      case "toc": out.push(new Paragraph({ children: [new TextRun({ text: "%%TOC%%", font: FONT, size: PT(14) })] })); break;
      case "pb": out.push(new Paragraph({ children: [new PageBreak()] })); break;
      case "appendix": out.push(...appendixHeading(b, isFirst)); break;
      case "listing": out.push(...listing(b)); break;
      default: throw new Error("unknown block " + b.t);
    }
  }
  return out;
}

// ---------------- разделы документа ----------------
const portrait = { page: { size: { width: CM(21), height: CM(29.7) },
  margin: { left: CM(3), right: CM(1.5), top: CM(2), bottom: CM(2), footer: CM(1) } } };
const landscape = { page: { size: { width: CM(21), height: CM(29.7), orientation: PageOrientation.LANDSCAPE },
  margin: { top: CM(3), bottom: CM(1.5), left: CM(2), right: CM(2), footer: CM(0.7) } } };
const fullPage = { page: { size: { width: CM(21), height: CM(29.7) },
  margin: { left: 0, right: 0, top: 0, bottom: 0, header: 0, footer: 0 } } };
const PORTRAIT_GEOM = { maxW: 16.5, maxH: 22.5 };
const LANDSCAPE_GEOM = { maxW: 25.7, maxH: 14.5 };

const mkFooter = () => new Footer({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
  children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: PT(14) })] })] });
const emptyFooter = () => new Footer({ children: [new Paragraph({ children: [] })] });

function fullPageImage(b) {
  const buf = fs.readFileSync(b.img);
  const pxW = Math.round(21 / 2.54 * 96) - 2, pxH = Math.round(29.7 / 2.54 * 96) - 4;
  return new Paragraph({ spacing: { before: 0, after: 0, line: 240, lineRule: LineRuleType.AUTO },
    children: [new ImageRun({ data: buf, type: "png", transformation: { width: pxW, height: pxH } })] });
}

function sections(blocks, withFooter, firstNoBreak) {
  const out = [];
  let cur = [];
  const flush = (noBreak) => {
    if (cur.length) {
      out.push({ properties: { ...portrait, type: SectionType.NEXT_PAGE },
        footers: { default: withFooter ? mkFooter() : emptyFooter() }, children: body(cur, PORTRAIT_GEOM, noBreak) });
    }
    cur = [];
  };
  let nextNoBreak = firstNoBreak;
  for (const b of blocks) {
    if (b.t === "fullpage") {
      flush(nextNoBreak);
      // каждое изображение — отдельный раздел без полей (разрыв страницы после рисунка во всю страницу
      // дал бы лишнюю пустую страницу)
      for (const img of (b.imgs || [b.img])) {
        out.push({ properties: { ...fullPage, type: SectionType.NEXT_PAGE }, footers: { default: emptyFooter() },
          children: [fullPageImage({ img })] });
      }
      nextNoBreak = true;
    } else if (b.t === "fig" && b.landscape) {
      flush(nextNoBreak);
      out.push({ properties: { ...landscape, type: SectionType.NEXT_PAGE },
        footers: { default: withFooter ? mkFooter() : emptyFooter() }, children: body([b], LANDSCAPE_GEOM, true) });
      nextNoBreak = true;
    } else {
      if (!cur.length && nextNoBreak === undefined) nextNoBreak = false;
      cur.push(b);
      continue;
    }
  }
  flush(nextNoBreak);
  return out;
}

const front = sections(C.front, false, false);
front[0] = { ...front[0], children: [...titlePage(C.title), ...front[0].children] };
// реферат начинается со второй страницы
const doc = new Document({
  features: { updateFields: false },
  styles: {
    default: { document: { run: { font: FONT, size: PT(14) }, paragraph: { spacing: { ...LINE } } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: PT(14), bold: true, color: "000000" }, paragraph: { outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: PT(14), bold: true, color: "000000" }, paragraph: { outlineLevel: 1 } },
    ],
  },
  sections: [...front, ...sections(C.body, true, true)],
});

Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(outPath, buf); console.log("written", outPath); });
