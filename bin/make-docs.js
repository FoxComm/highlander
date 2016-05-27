
const LeafDoc = require('leafdoc');
const fs = require('fs');

const OUT_PATH = process.env.DOCS_PATH || 'dist/reference.html';

function buildDocs() {
  console.log('Building Api documentation');

  const doc = new LeafDoc({
    templateDir: 'bin/leafdoc-templates',
    showInheritancesWhenEmpty: true,
    leadingCharacter: '@'
  });

  doc.addFile('src/index.js', true);
  doc.addDir('src/api');
  doc.addDir('src/utils');

  const out = doc.outputStr();
  fs.writeFileSync(OUT_PATH, out);
}

buildDocs();
