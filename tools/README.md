# tools/

## md-a-pdf.js — regenerar el PDF del informe de Discovery

Convierte `docs/discovery/informe-discovery.md` a HTML con estilo de informe y lo imprime a PDF con Microsoft Edge (o Chrome) en modo headless. Volvé a correrlo cada vez que editen el `.md`.

```bash
npm i --no-save marked@12.0.2
node tools/md-a-pdf.js docs/discovery/informe-discovery.md informe.html
"/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="docs\discovery\informe-discovery.pdf" "file:///$PWD/informe.html"
rm informe.html
```

Cuando la verificación de fuentes deje de ser un ejemplo, borren de `md-a-pdf.js` la línea `<div class="draft">BORRADOR …</div>`.
