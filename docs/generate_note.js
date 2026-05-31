const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
  LevelFormat
} = require('docx');
const fs = require('fs');

const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const cellBorders = { top: border, bottom: border, left: border, right: border };

const tableHeaderShading = { fill: "2E5FA3", type: ShadingType.CLEAR };
const tableRowShading    = { fill: "EEF3FB", type: ShadingType.CLEAR };

function h(text, level) {
  return new Paragraph({ heading: level, children: [new TextRun(text)] });
}

function p(runs, opts = {}) {
  const children = typeof runs === 'string'
    ? [new TextRun(runs)]
    : runs;
  return new Paragraph({ children, spacing: { after: 160 }, ...opts });
}

function bold(text) { return new TextRun({ text, bold: true }); }
function normal(text) { return new TextRun(text); }
function mono(text) { return new TextRun({ text, font: "Courier New", size: 20 }); }

function cell(content, opts = {}) {
  const { shading, width = 3120, bold: isBold = false, color } = opts;
  const runOpts = { text: content, bold: isBold };
  if (color) runOpts.color = color;
  return new TableCell({
    borders: cellBorders,
    width: { size: width, type: WidthType.DXA },
    shading,
    margins: { top: 100, bottom: 100, left: 150, right: 150 },
    verticalAlign: "center",
    children: [new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [new TextRun(runOpts)]
    })]
  });
}

// ── Table 1: precisionBits values ──────────────────────────────────────────
const precisionTable = new Table({
  width: { size: 9026, type: WidthType.DXA },
  columnWidths: [2256, 2256, 2257, 2257],
  rows: [
    new TableRow({
      tableHeader: true,
      children: [
        cell("Warto\u015b\u0107 precisionBits", { shading: tableHeaderShading, width: 2256, bold: true, color: "FFFFFF" }),
        cell("Dok\u0142adno\u015b\u0107 pozycji", { shading: tableHeaderShading, width: 2256, bold: true, color: "FFFFFF" }),
        cell("Maksymalne odchylenie", { shading: tableHeaderShading, width: 2257, bold: true, color: "FFFFFF" }),
        cell("Przyk\u0142adowe zastosowanie", { shading: tableHeaderShading, width: 2257, bold: true, color: "FFFFFF" }),
      ]
    }),
    new TableRow({ children: [
      cell("32 (pe\u0142na)", { width: 2256 }),
      cell("~1 cm", { width: 2256 }),
      cell("Brak", { width: 2257 }),
      cell("Domy\u015blne, pe\u0142na precyzja", { width: 2257 }),
    ]}),
    new TableRow({ children: [
      cell("19", { shading: tableRowShading, width: 2256 }),
      cell("~182 m", { shading: tableRowShading, width: 2256 }),
      cell("~91 m", { shading: tableRowShading, width: 2257 }),
      cell("Ni\u017cszy poziom prywatno\u015bci", { shading: tableRowShading, width: 2257 }),
    ]}),
    new TableRow({ children: [
      cell("16", { width: 2256 }),
      cell("~730 m", { width: 2256 }),
      cell("~365 m", { width: 2257 }),
      cell("\u015arednio obni\u017cona precyzja", { width: 2257 }),
    ]}),
    new TableRow({ children: [
      cell("13 (wyst\u0105pi\u0142o)", { shading: { fill: "FDECEA", type: ShadingType.CLEAR }, width: 2256, bold: true }),
      cell("~5,8 km", { shading: { fill: "FDECEA", type: ShadingType.CLEAR }, width: 2256, bold: true }),
      cell("~2,9 km", { shading: { fill: "FDECEA", type: ShadingType.CLEAR }, width: 2257, bold: true }),
      cell("Wysoka anonimizacja pozycji", { shading: { fill: "FDECEA", type: ShadingType.CLEAR }, width: 2257, bold: true }),
    ]}),
    new TableRow({ children: [
      cell("10", { width: 2256 }),
      cell("~46 km", { width: 2256 }),
      cell("~23 km", { width: 2257 }),
      cell("Bardzo ogólna lokalizacja", { width: 2257 }),
    ]}),
  ]
});

// ── Table 2: formula explanation ───────────────────────────────────────────
const formulaTable = new Table({
  width: { size: 9026, type: WidthType.DXA },
  columnWidths: [2500, 6526],
  rows: [
    new TableRow({ tableHeader: true, children: [
      cell("Wielko\u015b\u0107", { shading: tableHeaderShading, width: 2500, bold: true, color: "FFFFFF" }),
      cell("Opis", { shading: tableHeaderShading, width: 6526, bold: true, color: "FFFFFF" }),
    ]}),
    new TableRow({ children: [
      cell("precisionBits = 13", { width: 2500 }),
      cell("Liczba zachowanych bit\u00f3w wsp\u00f3\u0142rz\u0119dnych", { width: 6526 }),
    ]}),
    new TableRow({ children: [
      cell("32 \u2212 13 = 19", { shading: tableRowShading, width: 2500 }),
      cell("Liczba bit\u00f3w obci\u0119tych (zerowanych)", { shading: tableRowShading, width: 6526 }),
    ]}),
    new TableRow({ children: [
      cell("2\u00B9\u2079 \u00D7 10\u207B\u2077", { width: 2500 }),
      cell("Krok siatki kwantyzacji \u2248 0,0524\u00B0", { width: 6526 }),
    ]}),
    new TableRow({ children: [
      cell("0,0524\u00B0 \u00D7 111 km", { shading: tableRowShading, width: 2500 }),
      cell("Krok liniowy na r\u00f3wnole\u017cniku \u2248 5,8 km", { shading: tableRowShading, width: 6526 }),
    ]}),
  ]
});

const doc = new Document({
  styles: {
    default: {
      document: { run: { font: "Calibri", size: 24 } }
    },
    paragraphStyles: [
      {
        id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal",
        run: { size: 36, bold: true, font: "Calibri", color: "1F3864" },
        paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 }
      },
      {
        id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal",
        run: { size: 28, bold: true, font: "Calibri", color: "2E5FA3" },
        paragraph: { spacing: { before: 280, after: 140 }, outlineLevel: 1 }
      },
      {
        id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal",
        run: { size: 24, bold: true, font: "Calibri", color: "2E5FA3" },
        paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 2 }
      },
    ]
  },
  numbering: {
    config: [
      {
        reference: "bullets",
        levels: [{
          level: 0, format: LevelFormat.BULLET, text: "\u2022",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } }
        }]
      },
      {
        reference: "steps",
        levels: [{
          level: 0, format: LevelFormat.DECIMAL, text: "%1.",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } }
        }]
      }
    ]
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1800 }
      }
    },
    children: [

      // ── TYTUŁ ──────────────────────────────────────────────────────────
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 480, after: 120 },
        children: [new TextRun({
          text: "Analiza b\u0142\u0119du pozycjonowania w\u0119z\u0142\u00f3w Meshtastic",
          bold: true, size: 44, font: "Calibri", color: "1F3864"
        })]
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 80 },
        children: [new TextRun({
          text: "Systematyczne odchylenie pozycji GPS \u2014 przyczyna i spos\u00f3b usuni\u0119cia",
          size: 24, font: "Calibri", color: "595959", italics: true
        })]
      }),
      new Paragraph({
        border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: "2E5FA3", space: 1 } },
        spacing: { after: 400 },
        children: []
      }),

      // ── 1. OPIS PROBLEMU ───────────────────────────────────────────────
      h("1. Opis problemu", HeadingLevel.HEADING_1),

      p([
        normal("Podczas testowania aplikacji MeshTracker zaobserwowano, \u017ce znacznik w\u0119z\u0142a Meshtastic wy\u015bwietlany na mapie Google Maps konsekwentnie pojawia si\u0119 w "),
        bold("sta\u0142ej odleg\u0142o\u015bci od rzeczywistej pozycji urz\u0105dzenia"),
        normal(". Odchylenie nie zmienia\u0142o si\u0119 wraz z ruchem urz\u0105dzenia i zawsze wyst\u0119powa\u0142o w tym samym kierunku geograficznym."),
      ]),

      p([
        normal("Takie zachowanie \u2014 sta\u0142y, powtarzalny offset w okre\u015blonym kierunku \u2014 jest charakterystyczne dla b\u0142\u0119du "),
        bold("systematycznego"),
        normal(", nie za\u015b losowego szumu GPS. Wyklucza to wp\u0142yw warunk\u00f3w atmosferycznych, zak\u0142\u00f3ce\u0144 sygna\u0142u czy dok\u0142adno\u015bci odbiornika GPS.")
      ]),

      // ── 2. DIAGNOZA ────────────────────────────────────────────────────
      h("2. Diagnoza \u2014 mechanizm precisionBits", HeadingLevel.HEADING_1),

      h("2.1. Funkcja prywatno\u015bci pozycji w Meshtastic", HeadingLevel.HEADING_2),

      p([
        normal("Oprogramowanie Meshtastic zawiera wbudowany mechanizm ochrony prywatno\u015bci u\u017cytkownika \u2014 parametr "),
        bold("precisionBits"),
        normal(". Jego zadaniem jest celowe obni\u017cenie precyzji wsp\u00f3\u0142rz\u0119dnych GPS przed ich transmisj\u0105 w sieci mesh, tak aby inni uczestnicy sieci nie mogli pozna\u0107 dok\u0142adnego po\u0142o\u017cenia urz\u0105dzenia."),
      ]),

      p([
        normal("Wsp\u00f3\u0142rz\u0119dne geograficzne s\u0105 w protoko\u0142ach Meshtastic przechowywane jako 32-bitowe liczby ca\u0142kowite w formacie E7 (warto\u015b\u0107 w stopniach pomno\u017cona przez 10"),
        new TextRun({ text: "7", superScript: true }),
        normal("). Parametr precisionBits okre\u015bla, ile z tych 32 bit\u00f3w jest zachowywanych \u2014 pozosta\u0142e bity s\u0105 zerowane. Powoduje to "),
        bold("kwantyzacj\u0119 wsp\u00f3\u0142rz\u0119dnych"),
        normal(" do najbli\u017cszego punktu regularnej siatki."),
      ]),

      h("2.2. Obliczenie wielko\u015bci odchylenia", HeadingLevel.HEADING_2),

      p("Analiza logcat aplikacji ujawni\u0142a warto\u015b\u0107:"),

      new Paragraph({
        spacing: { before: 100, after: 200 },
        indent: { left: 720 },
        children: [new TextRun({
          text: "Field 'precisionBits': 13 (type: int)",
          font: "Courier New", size: 20, bold: true, color: "C0392B"
        })]
      }),

      p("Wielko\u015b\u0107 kroku kwantyzacji obliczana jest wed\u0142ug wzoru:"),

      new Paragraph({
        spacing: { before: 100, after: 100 },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({
          text: "krok = 2^(32 \u2212 precisionBits) \u00D7 10\u207B\u2077 stopnia",
          font: "Courier New", size: 22, bold: true, color: "1F3864"
        })]
      }),

      new Paragraph({
        spacing: { before: 60, after: 200 },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({
          text: "krok = 2\u00B9\u2079 \u00D7 10\u207B\u2077 \u2248 0,0524\u00B0 \u2248 5,8 km",
          font: "Courier New", size: 22, color: "C0392B"
        })]
      }),

      p("Poni\u017csza tabela wyja\u015bnia poszczeg\u00f3lne sk\u0142adowe obliczenia:"),
      new Paragraph({ spacing: { after: 200 }, children: [] }),
      formulaTable,
      new Paragraph({ spacing: { after: 320 }, children: [] }),

      p([
        normal("W\u0119ze\u0142 z "),
        bold("precisionBits = 13"),
        normal(" zg\u0142asza\u0142 zatem sw\u0105 pozycj\u0119 z odchyleniem si\u0119gaj\u0105cym nawet "),
        bold("oko\u0142o 5,8 km"),
        normal(". Zerowanie dolnych bit\u00f3w odpowiada operacji "),
        bold("floor()"),
        normal(" na wsp\u00f3\u0142rz\u0119dnych, co zawsze przesuwa pozycj\u0119 w kierunku po\u0142udniowo-zachodnim od rzeczywistego miejsca. St\u0105d w\u0142a\u015bnie odchylenie by\u0142o zawsze "),
        bold("sta\u0142e i jednokierunkowe"),
        normal("."),
      ]),

      h("2.3. Por\u00f3wnanie warto\u015bci precisionBits", HeadingLevel.HEADING_2),

      new Paragraph({ spacing: { after: 200 }, children: [] }),
      precisionTable,
      new Paragraph({ spacing: { after: 100 }, children: [] }),
      p([
        normal("Wiersz wy\u015bwietlony na czerwono odpowiada warto\u015bci wykrytej w testowanym urz\u0105dzeniu.")
      ]),

      // ── 3. DLACZEGO OFFSET JEST STAŁY? ────────────────────────────────
      h("3. Dlaczego offset jest sta\u0142y i jednokierunkowy?", HeadingLevel.HEADING_1),

      p("Zachowanie to wynika bezpo\u015brednio z matematyki kwantyzacji:"),

      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 100 },
        children: [
          bold("Operacja floor(): "),
          normal("Zerowanie dolnych bit\u00f3w liczby dodatniej jest r\u00f3wnoznaczne z zaokr\u0105glaniem w d\u00f3\u0142. Dla wsp\u00f3\u0142rz\u0119dnych dodatnich (p\u00f3\u0142nocna szeroko\u015b\u0107, wschodnia d\u0142ugo\u015b\u0107) powoduje to przesuni\u0119cie "),
          bold("zawsze w kierunku po\u0142udniowo-zachodnim"),
          normal("."),
        ]
      }),
      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 100 },
        children: [
          bold("Sta\u0142a siatka: "),
          normal("Punkty siatki s\u0105 niezale\u017cne od po\u0142o\u017cenia urz\u0105dzenia. Dop\u00f3ki urz\u0105dzenie nie przekroczy granicy kom\u00f3rki siatki (~5,8\u00d73,6 km), jego zg\u0142aszana pozycja pozostaje identyczna, niezale\u017cnie od rzeczywistego ruchu w obr\u0119bie tej kom\u00f3rki."),
        ]
      }),
      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 200 },
        children: [
          bold("Brak szumu: "),
          normal("W odro\u017cnieniu od b\u0142\u0119d\u00f3w GPS, kwantyzacja jest deterministyczna \u2014 dla danej rzeczywistej pozycji wynik jest zawsze identyczny."),
        ]
      }),

      // ── 4. WYKLUCZONE PRZYCZYNY ───────────────────────────────────────
      h("4. Wykluczone przyczyny", HeadingLevel.HEADING_1),

      p("W trakcie diagnozy rozwa\u017cano r\u00f3wnie\u017c inne mo\u017cliwe \u017ar\u00f3d\u0142a b\u0142\u0119du:"),

      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 80 },
        children: [
          bold("R\u00f3\u017cne uk\u0142ady wsp\u00f3\u0142rz\u0119dnych (WGS84 vs. inne datum): "),
          normal("Wykluczone \u2014 zar\u00f3wno GPS, jak i Google Maps u\u017cywaj\u0105 standardu WGS84."),
        ]
      }),
      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 80 },
        children: [
          bold("B\u0142\u0105d konwersji formatu E7: "),
          normal("Wykluczone \u2014 logi potwierdzi\u0142y poprawn\u0105 konwersj\u0119 liczb ca\u0142kowitych E7 na stopnie dziesi\u0119tne."),
        ]
      }),
      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 80 },
        children: [
          bold("Utrata precyzji typu Float: "),
          normal("Wykluczone \u2014 logi potwierdzi\u0142y, \u017ce API Meshtastic zwraca\u0142o wsp\u00f3\u0142rz\u0119dne jako typ int (E7), nie float."),
        ]
      }),
      new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 200 },
        children: [
          bold("B\u0142\u0105d sygna\u0142u GPS: "),
          normal("Wykluczone \u2014 b\u0142\u0119dy GPS s\u0105 losowe i zmienne, nie daj\u0105 sta\u0142ego, jednokierunkowego offsetu."),
        ]
      }),

      // ── 5. ROZWIĄZANIE ────────────────────────────────────────────────
      h("5. Spos\u00f3b rozwi\u0105zania", HeadingLevel.HEADING_1),

      h("5.1. Zmiana ustawienia w aplikacji Meshtastic", HeadingLevel.HEADING_2),

      p("Problem mo\u017cna usun\u0105\u0107 zmieniaj\u0105c konfiguracj\u0119 w\u0119z\u0142a:"),

      new Paragraph({
        numbering: { reference: "steps", level: 0 },
        spacing: { after: 100 },
        children: [normal("Otworzy\u0107 aplikacj\u0119 Meshtastic na urz\u0105dzeniu mobilnym.")]
      }),
      new Paragraph({
        numbering: { reference: "steps", level: 0 },
        spacing: { after: 100 },
        children: [normal("Przej\u015b\u0107 do: "),
          new TextRun({ text: "Radio Configuration \u2192 Device \u2192 Position \u2192 Position Precision", bold: true })]
      }),
      new Paragraph({
        numbering: { reference: "steps", level: 0 },
        spacing: { after: 100 },
        children: [normal("Zmieni\u0107 warto\u015b\u0107 z "),
          new TextRun({ text: "Low (13 bit\u00f3w)", bold: true, color: "C0392B" }),
          normal(" na "),
          new TextRun({ text: "High (32 bity)", bold: true, color: "27AE60" }),
          normal(".")]
      }),
      new Paragraph({
        numbering: { reference: "steps", level: 0 },
        spacing: { after: 200 },
        children: [normal("Zapisa\u0107 konfiguracj\u0119 i poczeka\u0107 na aktualizacj\u0119 pozycji w\u0119z\u0142a (do 30 sekund).")]
      }),

      h("5.2. Weryfikacja poprawno\u015bci", HeadingLevel.HEADING_2),

      p([
        normal("Po zastosowaniu poprawki warto\u015b\u0107 pola "),
        mono("precisionBits"),
        normal(" w logach aplikacji powinna wynosi\u0107 "),
        bold("32"),
        normal(", a znacznik w\u0119z\u0142a na mapie powinien pojawia\u0107 si\u0119 z dok\u0142adno\u015bci\u0105 poni\u017cej 1 metra, odpowiadaj\u0105c\u0105 mo\u017cliwo\u015bciom uk\u0142adu GPS."),
      ]),

      // ── 6. UWAGA O PRYWATNOŚCI ────────────────────────────────────────
      h("6. Uwaga dotycz\u0105ca prywatno\u015bci", HeadingLevel.HEADING_1),

      p([
        normal("Nale\u017cy podkre\u015bli\u0107, \u017ce parametr precisionBits pe\u0142ni uzasadnion\u0105 funkcj\u0119 \u2014 chroni prywatno\u015b\u0107 u\u017cytkownik\u00f3w sieci mesh, uniemo\u017cliwiaj\u0105c dok\u0142adne \u015bledzenie ich po\u0142o\u017cenia przez osoby trzecie. "),
        bold("Ustawienie pe\u0142nej precyzji (32 bity) jest zalecane wy\u0142\u0105cznie w zastosowaniach"),
        normal(", w kt\u00f3rych dok\u0142adno\u015b\u0107 pozycji ma pierwszorz\u0119dne znaczenie (np. \u015bledzenie floty pojazd\u00f3w, aplikacje SAR), i gdzie u\u017cytkownik \u015bwiadomie zgadza si\u0119 na udost\u0119pnianie swojego dok\u0142adnego po\u0142o\u017cenia."),
      ]),

      // ── LINIA KOŃCOWA ──────────────────────────────────────────────────
      new Paragraph({
        border: { top: { style: BorderStyle.SINGLE, size: 4, color: "2E5FA3", space: 1 } },
        spacing: { before: 480, after: 160 },
        alignment: AlignmentType.RIGHT,
        children: [new TextRun({
          text: "MeshTracker v1 \u2014 Dokumentacja techniczna",
          size: 18, color: "7F7F7F", italics: true
        })]
      }),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync("blad_precisionBits.docx", buffer);
  console.log("OK: blad_precisionBits.docx");
});
