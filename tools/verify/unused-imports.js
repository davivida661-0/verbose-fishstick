#!/usr/bin/env node
/**
 * Lista imports nao usados (o simple name da classe nao aparece em nenhum
 * outro lugar do arquivo). Ferramenta de limpeza, nao faz parte do build.
 *
 * Uso: node tools/verify/unused-imports.js client/src/main/java launcher/src/main/java api/src/main/java
 */
const fs = require('fs');
const path = require('path');

const roots = process.argv.slice(2);
let total = 0;

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else if (entry.name.endsWith('.java')) check(full);
  }
}

function check(file) {
  const text = fs.readFileSync(file, 'utf8');
  const lines = text.split('\n');
  const body = lines.filter((l) => !l.trim().startsWith('import ')).join('\n');
  const unused = [];

  for (const line of lines) {
    const m = line.match(/^import\s+(?:static\s+)?([\w.]+);/);
    if (!m) continue;
    const simple = m[1].split('.').pop();
    const re = new RegExp('\\b' + simple + '\\b');
    if (!re.test(body)) unused.push(simple);
  }

  if (unused.length) {
    total += unused.length;
    console.log(file + ': ' + unused.join(', '));
  }
}

for (const root of roots) walk(root);
console.log(total === 0 ? 'nenhum import nao usado' : total + ' import(s) nao usado(s)');
