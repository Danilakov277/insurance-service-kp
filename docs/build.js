// Builds the explanatory note (ПЗ) per СТП 01-2024 / кафедра ЭИ template.
// usage: node build.js content.json out.docx
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell, WidthType, AlignmentType,
  PageNumber, Footer, Header, TabStopType, BorderStyle, VerticalAlign, LineRuleType, PageBreak, SectionType,
  TableLayoutType,
} = require("docx");

const [, , contentPath, outPath] = process.argv;
const C = JSON.parse(fs.readFileSync(contentPath, "utf8"));

const FONT = "Times New Roman";
const PT = (x) => x * 2;               // half-points
const CM = (x) => Math.round(x * 567); // twips
const LINE = { line: 360, lineRule: LineRuleType.EXACT }; // exactly 18 pt
const IND = CM(1.25);

function runs(text, extra = {}) {
  // supports **bold** and _italic_ markup
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

function image(block) {
  const buf = fs.readFileSync(block.img);
  // png size
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const widthCm = block.w || 16.5;
  const pxW = Math.round(widthCm / 2.54 * 96), pxH = Math.round(pxW * h / w);
  return new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 240, after: 120, line: 240, lineRule: LineRuleType.AUTO }, keepNext: true,
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
  const mk = (cells, isHeader) => new TableRow({
    tableHeader: isHeader, cantSplit: true,
    children: cells.map((t, i) => new TableCell({
      width: { size: widths[i], type: WidthType.DXA }, borders, verticalAlign: VerticalAlign.TOP,
      margins: { left: 80, right: 80, top: 20, bottom: 20 },
      children: String(t).split("\n").map((line) => cellPara(line, isHeader, isHeader ? AlignmentType.CENTER : undefined)),
    })),
  });
  return new Table({
    width: { size: total, type: WidthType.DXA }, columnWidths: widths, layout: TableLayoutType.FIXED,
    rows: [mk(block.header, true), ...block.rows.map((r) => mk(r, false))],
  });
}

// ---------------- title page ----------------
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

// ---------------- body ----------------
function body(blocks, noFirstBreak) {
  const out = [];
  let first = !!noFirstBreak;
  for (const b of blocks) {
    const isFirst = first; first = false;
    switch (b.t) {
      case "h1": { const hp = heading(1, b.num, b.text, !b.num, isFirst); out.push(hp); break; }
      case "h2": out.push(heading(2, b.num, b.text)); break;
      case "p": out.push(P(b.text, { keepNext: b.keepNext })); break;
      case "pc": out.push(P(b.text, { align: AlignmentType.CENTER, indent: { firstLine: 0 } })); break;
      case "li": out.push(P(`– ${b.text}`)); break;
      case "ol": out.push(P(`${b.n}) ${b.text}`)); break;
      case "fig":
        out.push(image(b));
        out.push(P(b.caption, { align: AlignmentType.CENTER, indent: { firstLine: 0 }, after: 240, keepLines: true }));
        break;
      case "tbl":
        out.push(P(b.caption, { align: AlignmentType.LEFT, indent: { firstLine: 0 }, before: 240, keepNext: true }));
        out.push(table(b));
        out.push(new Paragraph({ children: [], spacing: { line: 240, after: 120 } }));
        break;
      case "tblcont":
        out.push(P(b.caption, { align: AlignmentType.LEFT, indent: { firstLine: 0 }, keepNext: true }));
        break;
      case "formula":
        out.push(new Paragraph({ alignment: AlignmentType.LEFT, spacing: { ...LINE, before: 120, after: 120 },
          tabStops: [{ type: TabStopType.CENTER, position: CM(8.25) }, { type: TabStopType.RIGHT, position: CM(16.5) }],
          children: [new TextRun({ text: `\t${b.text}\t${b.num || ""}`, font: FONT, size: PT(14) })] }));
        break;
      case "where": out.push(P(b.text, { indent: { firstLine: 0 } })); break;
      case "code":
        for (const line of b.text.split("\n")) out.push(new Paragraph({ spacing: { line: 240 },
          children: [new TextRun({ text: line || " ", font: "Courier New", size: PT(10) })] }));
        break;
      case "toc": out.push(new Paragraph({ children: [new TextRun({ text: "%%TOC%%", font: FONT, size: PT(14) })] })); break;
      case "pb": out.push(new Paragraph({ children: [new PageBreak()] })); break;
      case "centerpage":
        out.push(heading(1, null, b.title, true));
        for (const l of b.lines || []) out.push(P(l, { align: AlignmentType.CENTER, indent: { firstLine: 0 } }));
        break;
      default: throw new Error("unknown block " + b.t);
    }
  }
  return out;
}

const pageProps = { page: { size: { width: CM(21), height: CM(29.7) },
  margin: { left: CM(3), right: CM(1.5), top: CM(2), bottom: CM(2), footer: CM(1) } } };
const footer = new Footer({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
  children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: PT(14) })] })] });

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
  sections: [
    { properties: { ...pageProps }, children: [...titlePage(C.title), ...body(C.front)] },
    { properties: { ...pageProps, type: SectionType.NEXT_PAGE }, footers: { default: footer }, children: body(C.body, true) },
  ],
});

Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(outPath, buf); console.log("written", outPath); });
