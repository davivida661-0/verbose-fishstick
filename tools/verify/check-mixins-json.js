#!/usr/bin/env node
/**
 * Valida o client/src/main/resources/solar.mixins.json:
 *   - JSON valido
 *   - toda classe listada existe mesmo (classe aninhada em SolarMixin.java)
 *   - todo @Mixin do fonte esta listado
 *
 * Uso: node tools/verify/check-mixins-json.js
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', '..');
const JSON_PATH = path.join(ROOT, 'client/src/main/resources/solar.mixins.json');
const SOURCE = path.join(ROOT, 'client/src/main/java/com/solarclient/mixin/SolarMixin.java');

const config = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
const source = fs.readFileSync(SOURCE, 'utf8');
// no mixins.json os nomes sao RELATIVOS ao campo "package"
const listed = [...(config.client || []), ...(config.mixins || []), ...(config.server || [])];

let errors = 0;

// 1) package declarado bate com o package do fonte
if (!source.includes('package ' + config.package + ';')) {
  console.log('ERRO: o package do fonte nao bate com o do JSON (' + config.package + ')');
  errors++;
}

// 2) classes aninhadas realmente declaradas
//    (nomes RELATIVOS ao package, igualzinhos aos que ficam no JSON)
const declared = new Set();
const re = /public\s+(?:abstract\s+)?static\s+class\s+(\w+)/g;
let match;
while ((match = re.exec(source)) !== null) {
  declared.add('SolarMixin$' + match[1]);
}

for (const name of listed) {
  if (!declared.has(name)) {
    console.log('ERRO: ' + name + ' esta no JSON mas nao existe no fonte');
    errors++;
  }
}

// 3) todo @Mixin do fonte esta listado
for (const name of declared) {
  if (!listed.includes(name)) {
    console.log('ERRO: ' + name + ' existe no fonte mas nao esta no JSON');
    errors++;
  }
}

// 4) o refmap e gerado pelo annotation processor do Mixin em compileJava
//    (ele nao precisa estar no build.gradle, mas o processor precisa estar no
//    classpath de compilacao - ver client/build.gradle)
const gradle = fs.readFileSync(path.join(ROOT, 'client/build.gradle'), 'utf8');
if (config.refmap && !gradle.includes(':processor')) {
  console.log('ERRO: o refmap "' + config.refmap + '" exige o annotation processor do Mixin, '
    + 'que nao esta em client/build.gradle');
  errors++;
}

console.log(errors === 0
  ? 'OK: solar.mixins.json valido (' + listed.length + ' mixins, ' + declared.size + ' declarados)'
  : 'FALHA: ' + errors + ' problema(s)');
process.exit(errors === 0 ? 0 : 1);
