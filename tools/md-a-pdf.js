const fs=require('fs');const {marked}=require('marked');
const [,,src,out]=process.argv;
const md=fs.readFileSync(src,'utf8');
const css=`@page{size:A4 landscape;margin:14mm 12mm}
body{font-family:"Segoe UI",Arial,sans-serif;font-size:9.5pt;line-height:1.4;color:#1a1a1a}
h1{font-size:20pt;margin:0 0 4pt;color:#0b3d5c}h2{font-size:14pt;color:#0b3d5c;border-bottom:1.5px solid #0b3d5c;padding-bottom:2pt;margin-top:16pt;page-break-after:avoid}
h3{font-size:11.5pt;color:#155e75;margin-top:12pt;page-break-after:avoid}h4{font-size:10.5pt}
table{border-collapse:collapse;width:100%;margin:6pt 0;font-size:7.6pt;page-break-inside:auto}
th,td{border:1px solid #b9c4cc;padding:3pt 4pt;vertical-align:top;text-align:left}th{background:#e6eef3}
tr{page-break-inside:avoid}blockquote{border-left:3px solid #d97706;background:#fff7ed;margin:6pt 0;padding:4pt 8pt}
code{font-size:8.5pt;background:#f1f5f9;padding:0 2px}a{color:#0b5c8a;word-break:break-all}hr{border:0;border-top:1px solid #ccc}
.draft{position:fixed;top:0;right:0;font-size:7pt;color:#b45309}`;
const html=`<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Informe de Discovery — Turnos Odontología</title><style>${css}</style></head><body><div class="draft">BORRADOR — verificación de fuentes de ejemplo</div>${marked.parse(md)}</body></html>`;
fs.writeFileSync(out,html);
