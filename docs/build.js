// Сборка пояснительной записки (ПЗ) по СТП 01-2024 и шаблону кафедры ЭИ.
// usage: node build.js content.json out.docx
const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell, WidthType, AlignmentType,
  PageNumber, Footer, TabStopType, BorderStyle, VerticalAlign, LineRuleType, PageBreak, SectionType,
  TableLayoutType, PageOrientation, TextDirection, HeightRule, VerticalMergeType,
} = require("docx");

const [, , contentPath, outPath] = process.argv;
const C = JSON.parse(fs.readFileSync(contentPath, "utf8"));

const FONT = "Times New Roman";
const MONO = "Courier New";
const PT = (x) => x * 2;               // полупункты
const CM = (x) => Math.round(x * 567); // twips
const LINE = { line: 360, lineRule: LineRuleType.EXACT }; // ровно 18 пт
const IND = CM(1.25);

// Разметка текста: **жирный**, __курсив__, _{нижний индекс}, ^{верхний индекс}; допускается вложение.
function parseMarkup(text, style, out, size) {
  let i = 0, buf = "";
  const flush = () => {
    if (!buf) return;
    out.push(new TextRun({ text: buf, font: style.font || FONT, size: PT(size), bold: style.bold,
      italics: style.italics, subScript: style.sub, superScript: style.sup }));
    buf = "";
  };
  while (i < text.length) {
    const two = text.substr(i, 2);
    if (two === "**" || two === "__") {
      const j = text.indexOf(two, i + 2);
      if (j > i + 1) {
        flush();
        parseMarkup(text.slice(i + 2, j), { ...style, [two === "**" ? "bold" : "italics"]: true }, out, size);
        i = j + 2; continue;
      }
    }
    if (two === "_{" || two === "^{") {
      let depth = 1, j = i + 2;
      while (j < text.length && depth) { if (text[j] === "{") depth++; else if (text[j] === "}") depth--; j++; }
      flush();
      parseMarkup(text.slice(i + 2, j - 1), { ...style, sub: two === "_{", sup: two === "^{" }, out, size);
      i = j; continue;
    }
    buf += text[i]; i++;
  }
  flush();
}
function runs(text, extra = {}, size = 14) {
  const out = [];
  parseMarkup(String(text), extra, out, size);
  return out;
}

const BL = 360;        // пробельная строка (одна строка 18 пт)
const P = (text, o = {}) => new Paragraph({
  children: runs(text, o.run || {}),
  alignment: o.align ?? AlignmentType.JUSTIFIED,
  indent: o.indent ?? { firstLine: IND },
  spacing: { ...LINE, before: o.before || 0, after: 0 },
  keepNext: o.keepNext, keepLines: o.keepLines, pageBreakBefore: o.pageBreakBefore,
  tabStops: o.tabStops,
});
const empty = () => new Paragraph({ children: [], spacing: { ...LINE } });

// Заголовки (СТП 01-2024, п. 2.1.1, 2.2.5, прил. Л): разделы и подразделы – с абзацного отступа,
// полужирные, вторая строка – по началу текста первой; ненумерованные – по центру.
// Пробельная строка после заголовка задаётся отступом «перед» следующего элемента.
function heading(level, num, text, centered, noBreak, before) {
  const opts = {
    heading: level === 1 ? "Heading1" : "Heading2",
    spacing: { ...LINE, before: before || 0, after: 0 },
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

function image(block, maxW, maxH, before) {
  const buf = fs.readFileSync(block.img);
  const [w, h] = pngSize(buf);
  let widthCm = Math.min(block.w || maxW, maxW);
  if (widthCm * h / w > maxH) widthCm = maxH * w / h;
  const pxW = Math.round(widthCm / 2.54 * 96), pxH = Math.round(pxW * h / w);
  // СТП 01-2024, п. 2.5.3 и прил. Н: рисунок отделяется от текста и от подписи пробельной строкой
  return new Paragraph({
    alignment: AlignmentType.CENTER, keepNext: true,
    spacing: { before: before || 0, after: 0, line: 240, lineRule: LineRuleType.AUTO },
    children: [new ImageRun({ data: buf, type: "png", transformation: { width: pxW, height: pxH } })],
  });
}

// Широкий рисунок на книжной странице, повёрнутый так, чтобы его читали с поворотом записки
// на 90° по часовой стрелке (СТП 01-2024, п. 2.5.4). Изображение заранее повёрнуто (pz.py), подпись
// стоит справа от него в ячейке с направлением текста снизу вверх. Высота строки почти равна
// высоте поля страницы, поэтому рисунок всегда занимает отдельную страницу.
const ROT_ROW_H = 25.0, ROT_CAP_W = 1.6, ROT_GAP = 0.7;
function rotatedFigure(b) {
  const buf = fs.readFileSync(b.imgRot);
  const [w, h] = pngSize(buf);                       // размеры повёрнутого изображения
  const maxW = 16.5 - ROT_CAP_W - ROT_GAP, maxH = ROT_ROW_H - 0.6;
  let wCm = maxW, hCm = maxW * h / w;
  if (hCm > maxH) { hCm = maxH; wCm = maxH * w / h; }
  const pxW = Math.round(wCm / 2.54 * 96), pxH = Math.round(hCm / 2.54 * 96);
  const none = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
  const nob = { top: none, bottom: none, left: none, right: none };
  return [
    new Table({
      width: { size: CM(16.5), type: WidthType.DXA }, layout: TableLayoutType.FIXED,
      columnWidths: [CM(16.5 - ROT_CAP_W), CM(ROT_CAP_W)],
      borders: { ...nob, insideHorizontal: none, insideVertical: none },
      rows: [new TableRow({ cantSplit: true, height: { value: CM(ROT_ROW_H), rule: HeightRule.EXACT },
        children: [
          new TableCell({ width: { size: CM(16.5 - ROT_CAP_W), type: WidthType.DXA }, borders: nob,
            verticalAlign: VerticalAlign.CENTER, margins: { left: 0, right: CM(ROT_GAP), top: 0, bottom: 0 },
            children: [new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 240, lineRule: LineRuleType.AUTO },
              children: [new ImageRun({ data: buf, type: "png", transformation: { width: pxW, height: pxH } })] })] }),
          new TableCell({ width: { size: CM(ROT_CAP_W), type: WidthType.DXA }, borders: nob,
            verticalAlign: VerticalAlign.CENTER, textDirection: TextDirection.BOTTOM_TO_TOP_LEFT_TO_RIGHT,
            margins: { left: 0, right: 0, top: 0, bottom: 0 },
            children: [new Paragraph({ alignment: AlignmentType.CENTER, spacing: { ...LINE },
              children: runs(b.caption) })] }),
        ] })],
    }),
    // разделитель: две таблицы подряд Word склеил бы в одну
    new Paragraph({ spacing: { before: 0, after: 0, line: 20, lineRule: LineRuleType.EXACT },
      children: [new TextRun({ text: "", size: 2 })] }),
  ];
}

function cellPara(text, align) {
  return new Paragraph({
    alignment: align || AlignmentType.LEFT, spacing: { line: 240, lineRule: LineRuleType.AUTO },
    children: runs(text, {}, 12),
  });
}

// Таблица (СТП 01-2024, п. 2.6): головка – обычным шрифтом по центру; ячейка «^» объединяется
// с ячейкой выше (пустые графы не допускаются).
function table(block) {
  const total = CM(16.5);
  const sum = block.cols.reduce((a, b) => a + b, 0);
  const widths = block.cols.map((c) => Math.round(c / sum * total));
  widths[widths.length - 1] += total - widths.reduce((a, b) => a + b, 0);
  const border = { style: BorderStyle.SINGLE, size: 6, color: "000000" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const aligns = block.align || [];
  const rows = block.rows;
  const mk = (cells, r, isHeader) => new TableRow({
    tableHeader: isHeader, cantSplit: true,
    children: cells.map((t, i) => {
      const cont = !isHeader && t === "^";
      const restart = !isHeader && !cont && r + 1 < rows.length && rows[r + 1][i] === "^";
      const align = isHeader ? AlignmentType.CENTER
        : (aligns[i] === "c" ? AlignmentType.CENTER : aligns[i] === "r" ? AlignmentType.RIGHT : undefined);
      return new TableCell({
        width: { size: widths[i], type: WidthType.DXA }, borders,
        verticalAlign: isHeader ? VerticalAlign.CENTER : VerticalAlign.TOP,
        margins: { left: 80, right: 80, top: 20, bottom: 20 },
        verticalMerge: cont ? VerticalMergeType.CONTINUE : (restart ? VerticalMergeType.RESTART : undefined),
        children: cont ? [cellPara("", align)] : String(t).split("\n").map((line) => cellPara(line, align)),
      });
    }),
  });
  return new Table({
    width: { size: total, type: WidthType.DXA }, columnWidths: widths, layout: TableLayoutType.FIXED,
    rows: [mk(block.header, -1, true), ...rows.map((row, r) => mk(row, r, false))],
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

function listing(block, before) {
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
      out.push(new Paragraph({ alignment: AlignmentType.CENTER, pageBreakBefore: true, spacing: { ...LINE, after: 0 },
        children: [new TextRun({ text: `Продолжение приложения ${block.letter}`, font: FONT, size: PT(14) })] }));
    }
    for (const [li, l] of page.entries()) {
      out.push(new Paragraph({ spacing: { line: 240, lineRule: LineRuleType.EXACT,
        before: (k === 0 && li === 0) ? (before || 0) : (k > 0 && li === 0 ? 240 : 0) }, keepLines: true,
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
      children: [new TextRun({ text: `(${b.kind || "обязательное"})`, font: FONT, size: PT(14), bold: true })] }),
    ...b.title.split("\n").map((t, i, arr) => new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true,
      spacing: { ...LINE, after: 0 },
      children: [new TextRun({ text: t, font: FONT, size: PT(14), bold: true })] })),
  ];
}

// ---------------- тело ----------------
// Пробельная строка между элементами (СТП 01-2024, прил. Л, Н, М, п. 2.6.3): после заголовков,
// до и после рисунков, таблиц и формул, перед подразделом и пунктом. Задаётся только отступом
// «перед» следующего элемента, чтобы промежутки не складывались.
function gapBefore(prev, cur) {
  if (!prev) return false;
  if (cur.t === "h1" || cur.t === "appendix" || cur.t === "toc") return prev && ["h1"].includes(prev.t);
  if (cur.t === "tbl" && (cur.cont || cur.newPage)) return false;
  if (["h1", "h2", "appendix", "fig", "tbl", "formula"].includes(prev.t)) return true;
  return ["h2", "pt", "fig", "tbl", "formula"].includes(cur.t);
}

const WHERE_IND = CM(1.0);
const SPACER = () => new Paragraph({ spacing: { before: 0, after: 0, line: 20, lineRule: LineRuleType.EXACT },
  children: [new TextRun({ text: "", size: 2 })] });

function body(blocks, geom, noFirstBreak) {
  const out = [];
  let first = !!noFirstBreak;
  for (let bi = 0; bi < blocks.length; bi++) {
    const b = blocks[bi], next = blocks[bi + 1], prev = blocks[bi - 1];
    const isFirst = first; first = false;
    const before = gapBefore(prev, b) ? BL : 0;
    switch (b.t) {
      case "h1": out.push(heading(1, b.num, b.text, !b.num, isFirst, 0)); break;
      case "h2": out.push(heading(2, b.num, b.text, false, false, before)); break;
      case "p": out.push(P(b.text, { before, keepNext: b.keepNext })); break;
      case "pc": out.push(P(b.text, { before, align: AlignmentType.CENTER, indent: { firstLine: 0 } })); break;
      case "pl": out.push(P(b.text, { before, align: AlignmentType.LEFT, indent: { firstLine: 0 } })); break;
      case "li": out.push(P(`– ${b.text}`, { before })); break;
      case "ol": out.push(P(`${b.n} ${b.text}`, { before })); break;           // сложное перечисление (п. 2.3.7)
      case "ola": out.push(P(`${b.letter}) ${b.text}`, { before })); break;    // перечисление со ссылками (п. 2.3.8)
      case "pt": out.push(P(`**${b.num}** ${b.text}`, { before })); break;     // пункт (прил. Л)
      case "src": out.push(P(b.text, { before })); break;                      // библиографическая запись (п. 2.8.6)
      case "fig":
        if (b.imgRot) {
          out.push(...rotatedFigure(b));
        } else {
          out.push(image(b, geom.maxW, geom.maxH, before));
          out.push(P(b.caption, { align: AlignmentType.CENTER, indent: { firstLine: 0 }, before: BL, keepLines: true }));
        }
        break;
      case "tbl":
        out.push(P(b.caption, { align: AlignmentType.LEFT, before,
          indent: b.hang && !b.cont ? { left: b.hang, hanging: b.hang } : { firstLine: 0 },
          keepNext: true, keepLines: true, pageBreakBefore: !!b.cont || !!b.newPage }));
        out.push(table(b));
        // две таблицы подряд (таблица и повёрнутый рисунок) Word объединил бы в одну
        if (next && next.t === "fig" && next.imgRot) out.push(SPACER());
        break;
      case "formula":
        out.push(new Paragraph({ alignment: AlignmentType.LEFT, spacing: { ...LINE, before, after: 0 },
          keepNext: !!b.keepNext, keepLines: true,
          tabStops: [{ type: TabStopType.CENTER, position: CM(8.25) }, { type: TabStopType.RIGHT, position: CM(16.5) }],
          children: [new TextRun({ text: "\t", font: FONT, size: PT(14) }), ...runs(b.text),
            new TextRun({ text: `\t${b.num || ""}`, font: FONT, size: PT(14) })] }));
        break;
      case "where": {
        // «где» – без абзацного отступа, символы выровнены в столбец (п. 2.4.7, прил. Л)
        const firstLine = b.text.startsWith("где ");
        out.push(P(firstLine ? "где\t" + b.text.slice(4) : b.text, { before,
          indent: firstLine ? { left: WHERE_IND, hanging: WHERE_IND } : { left: WHERE_IND },
          tabStops: [{ type: TabStopType.LEFT, position: WHERE_IND }] }));
        break;
      }
      case "toc": out.push(new Paragraph({ spacing: { ...LINE, before },
        children: [new TextRun({ text: "%%TOC%%", font: FONT, size: PT(14) })] })); break;
      case "pb": out.push(new Paragraph({ children: [new PageBreak()] })); break;
      case "appendix": out.push(...appendixHeading(b, isFirst)); break;
      case "listing": out.push(...listing(b, before)); break;
      default: throw new Error("unknown block " + b.t);
    }
  }
  return out;
}

// ---------------- разделы документа ----------------
const portrait = { page: { size: { width: CM(21), height: CM(29.7) },
  margin: { left: CM(3), right: CM(1.5), top: CM(2), bottom: CM(2), footer: CM(1) } } };
const fullPage = { page: { size: { width: CM(21), height: CM(29.7) },
  margin: { left: 0, right: 0, top: 0, bottom: 0, header: 0, footer: 0 } } };
const PORTRAIT_GEOM = { maxW: 16.5, maxH: 22.5 };

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
