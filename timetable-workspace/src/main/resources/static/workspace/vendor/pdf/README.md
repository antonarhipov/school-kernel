# PDF dependencies

Loaded on demand, served locally; no CDN is used at runtime.

- pdf-lib 1.17.1: `dist/pdf-lib.esm.min.js`, MIT, https://github.com/Hopding/pdf-lib. Copied from the installed package; license retained.
- @pdf-lib/fontkit 1.1.1: `dist/fontkit.umd.min.js`, MIT, https://github.com/Hopding/fontkit. Obtained from the npm archive. The UMD global target is changed from `this` to `globalThis` and a default ESM export is appended. This build includes its dependencies. The published package declares MIT in its package metadata and README but contains no separate license file; notice retained below.
- Liberation Sans regular and bold: from pdfjs-dist's standard fonts; SIL Open Font License 1.1, retained in LICENSE_LIBERATION.

Fontkit copyright Andrew Dillon and Devon Govett and contributors.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
