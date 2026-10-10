/* =====================================================
   SPREADSHEET READER (CSV and Excel .xlsx)
   =====================================================
   No outside library needed:
   - CSV is parsed directly.
   - .xlsx files are ZIP files with XML inside. They are
     unzipped with the browser's built-in DecompressionStream
     and the first worksheet is read.
   readSpreadsheet(file) → Promise of rows: [["first_name", ...], [...], ...]
===================================================== */

async function readSpreadsheet(file) {
    const name = (file.name || "").toLowerCase();

    if (name.endsWith(".csv") || file.type === "text/csv") {
        return parseCsv(await file.text());
    }
    if (name.endsWith(".xlsx")) {
        return readXlsx(await file.arrayBuffer());
    }
    if (name.endsWith(".xls")) {
        throw new Error("Old Excel files (.xls) are not supported. In Excel choose File → Save As → Excel Workbook (.xlsx) or CSV.");
    }
    throw new Error("Please upload a .csv or .xlsx file.");
}

/* ---------- CSV ---------- */

function parseCsv(text) {
    text = text.replace(/^﻿/, "");          // remove Excel's BOM

    // Excel in some regions saves CSV with ";" instead of ","
    const firstLine = text.split(/\r?\n/, 1)[0] || "";
    const delimiter =
        (firstLine.split(";").length > firstLine.split(",").length) ? ";" : ",";

    const rows = [];
    let row = [];
    let field = "";
    let inQuotes = false;

    for (let i = 0; i < text.length; i++) {
        const c = text[i];

        if (inQuotes) {
            if (c === '"') {
                if (text[i + 1] === '"') { field += '"'; i++; }
                else inQuotes = false;
            } else {
                field += c;
            }
        } else if (c === '"') {
            inQuotes = true;
        } else if (c === delimiter) {
            row.push(field); field = "";
        } else if (c === "\n" || c === "\r") {
            if (c === "\r" && text[i + 1] === "\n") i++;
            row.push(field); rows.push(row);
            row = []; field = "";
        } else {
            field += c;
        }
    }
    if (field !== "" || row.length) { row.push(field); rows.push(row); }
    return rows;
}

/* ---------- XLSX ---------- */

async function readXlsx(buffer) {
    if (typeof DecompressionStream === "undefined") {
        throw new Error("This browser cannot read Excel files. Please save the file as CSV and upload that instead.");
    }

    const zip = readZipDirectory(buffer);
    const text = async function(path) {
        const entry = zip[path];
        return entry ? new TextDecoder("utf-8").decode(await unzipEntry(buffer, entry)) : null;
    };
    const xml = function(str) { return new DOMParser().parseFromString(str, "application/xml"); };

    // Shared strings (most text cells point into this list)
    const shared = [];
    const sstText = await text("xl/sharedStrings.xml");
    if (sstText) {
        xml(sstText).querySelectorAll("si").forEach(function(si) {
            let s = "";
            si.querySelectorAll("t").forEach(function(t) {
                // skip phonetic text
                if (!t.closest("rPh")) s += t.textContent;
            });
            shared.push(s);
        });
    }

    // Find the first worksheet
    let sheetPath = "xl/worksheets/sheet1.xml";
    const wbText = await text("xl/workbook.xml");
    const relsText = await text("xl/_rels/workbook.xml.rels");
    if (wbText && relsText) {
        const firstSheet = xml(wbText).querySelector("sheets > sheet, sheet");
        const relId = firstSheet && (firstSheet.getAttribute("r:id") ||
            firstSheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"));
        xml(relsText).querySelectorAll("Relationship").forEach(function(rel) {
            if (rel.getAttribute("Id") === relId) {
                const target = rel.getAttribute("Target").replace(/^\//, "");
                sheetPath = target.startsWith("xl/") ? target : "xl/" + target;
            }
        });
    }

    const sheetText = await text(sheetPath);
    if (!sheetText) throw new Error("The Excel file has no worksheet.");

    const rows = [];
    xml(sheetText).querySelectorAll("sheetData > row, row").forEach(function(rowEl) {
        const r = parseInt(rowEl.getAttribute("r"), 10) || rows.length + 1;
        const values = [];
        rowEl.querySelectorAll("c").forEach(function(c, index) {
            const ref = c.getAttribute("r");
            const col = ref ? columnIndex(ref) : index;
            const type = c.getAttribute("t");
            const v = c.querySelector("v");
            let value = "";
            if (type === "s") value = shared[parseInt(v ? v.textContent : "0", 10)] || "";
            else if (type === "inlineStr") value = Array.from(c.querySelectorAll("is t")).map(function(t) { return t.textContent; }).join("");
            else if (type === "b") value = v && v.textContent === "1" ? "TRUE" : "FALSE";
            else value = v ? v.textContent : "";
            // 19.0000000001 → 19 (Excel floating point)
            if (type !== "s" && type !== "inlineStr" && /^-?\d+\.\d{6,}$/.test(value)) {
                value = String(Math.round(parseFloat(value) * 1e6) / 1e6);
            }
            values[col] = value;
        });
        for (let i = 0; i < values.length; i++) if (values[i] === undefined) values[i] = "";
        rows[r - 1] = values;
    });
    for (let i = 0; i < rows.length; i++) if (!rows[i]) rows[i] = [];
    return rows;
}

function columnIndex(cellRef) {
    const letters = cellRef.replace(/[0-9]/g, "").toUpperCase();
    let n = 0;
    for (let i = 0; i < letters.length; i++) n = n * 26 + (letters.charCodeAt(i) - 64);
    return n - 1;
}

/* Reads the ZIP "central directory": file name → where its data is. */
function readZipDirectory(buffer) {
    const view = new DataView(buffer);
    let eocd = -1;
    for (let i = buffer.byteLength - 22; i >= Math.max(0, buffer.byteLength - 66000); i--) {
        if (view.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
    }
    if (eocd < 0) throw new Error("This file is not a valid Excel (.xlsx) file.");

    const count = view.getUint16(eocd + 10, true);
    let offset = view.getUint32(eocd + 16, true);
    const entries = {};
    const decoder = new TextDecoder("utf-8");

    for (let i = 0; i < count; i++) {
        if (view.getUint32(offset, true) !== 0x02014b50) break;
        const method = view.getUint16(offset + 10, true);
        const compressedSize = view.getUint32(offset + 20, true);
        const nameLength = view.getUint16(offset + 28, true);
        const extraLength = view.getUint16(offset + 30, true);
        const commentLength = view.getUint16(offset + 32, true);
        const localOffset = view.getUint32(offset + 42, true);
        const name = decoder.decode(new Uint8Array(buffer, offset + 46, nameLength));
        entries[name] = { method: method, compressedSize: compressedSize, localOffset: localOffset };
        offset += 46 + nameLength + extraLength + commentLength;
    }
    return entries;
}

async function unzipEntry(buffer, entry) {
    const view = new DataView(buffer);
    const nameLength = view.getUint16(entry.localOffset + 26, true);
    const extraLength = view.getUint16(entry.localOffset + 28, true);
    const start = entry.localOffset + 30 + nameLength + extraLength;
    const data = new Uint8Array(buffer, start, entry.compressedSize);

    if (entry.method === 0) return data;                      // stored
    if (entry.method !== 8) throw new Error("Unsupported compression in Excel file.");

    const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    return new Uint8Array(await new Response(stream).arrayBuffer());
}
