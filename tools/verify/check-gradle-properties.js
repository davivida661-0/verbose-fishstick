#!/usr/bin/env node
/**
 * Confere o gradle.properties:
 *   - nao ha comentario colado no fim de uma chave (o Gradle leria o
 *     comentario como parte do valor e a dependencia nao resolveria)
 *   - toda chave referenciada como rootProject.* nos .gradle existe no arquivo
 *
 * Uso: node tools/verify/check-gradle-properties.js
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', '..');
const props = fs.readFileSync(path.join(ROOT, 'gradle.properties'), 'utf8');

let errors = 0;
const keys = new Map();

props.split('\n').forEach((line, i) => {
  if (!line.trim() || line.trim().startsWith('#') || line.trim().startsWith('!')) return;
  const eq = line.indexOf('=');
  if (eq < 0) return;
  const key = line.slice(0, eq).trim();
  const value = line.slice(eq + 1).trim();
  if (value.includes('#')) {
    console.log('ERRO linha ' + (i + 1) + ': "' + key + '" tem comentario colado no valor -> "' + value + '"');
    errors++;
  }
  keys.set(key, value);
});

// chaves usadas nos build scripts:
//   - "rootProject.<chave>" em qualquer .gradle
//   - "${<chave>}" apenas no build raiz (onde as versoes sao lidas direto)
// metodos da API do Gradle que aparecem como rootProject.<algo> mas NAO sao
// chaves do gradle.properties
const GRADLE_API = new Set(['name', 'file', 'dir', 'projectDir', 'rootDir', 'buildDir',
  'version', 'group', 'tasks', 'project', 'allprojects', 'subprojects']);

const used = new Set();
for (const file of ['build.gradle', 'client/build.gradle', 'launcher/build.gradle', 'api/build.gradle']) {
  const text = fs.readFileSync(path.join(ROOT, file), 'utf8');
  const re = /rootProject\.(\w+)/g;
  let m;
  while ((m = re.exec(text)) !== null) {
    if (!GRADLE_API.has(m[1])) used.add(m[1]);
  }
}
const rootBuild = fs.readFileSync(path.join(ROOT, 'build.gradle'), 'utf8');
for (const m of rootBuild.matchAll(/\$\{(\w+)\}/g)) used.add(m[1]);

for (const key of used) {
  if (!keys.has(key)) {
    console.log('ERRO: o build referencia a propriedade "' + key + '", que nao existe no gradle.properties');
    errors++;
  }
}

console.log(errors === 0
  ? 'OK: gradle.properties valido (' + used.size + ' chaves referenciadas)'
  : 'FALHA: ' + errors + ' problema(s)');
process.exit(errors === 0 ? 0 : 1);
