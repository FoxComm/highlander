
const LeafDoc = require('leafdoc');
const fs = require('fs');
const handlebars = require('handlebars');
const escapeHtml = require('escape-html');

const OUT_PATH = process.env.DOCS_PATH || 'dist/reference.html';

// add support for composite types

const makeLink = handlebars.helpers.type;
const typeRe = /[\w\._\s]+/g;

handlebars.registerHelper('type', function(str) {
  return str
    .replace(/[\<\>]/g, function(str) {
      return escapeHtml(str);
    })
    .replace(typeRe, function(typeStr) {
      return makeLink(typeStr);
    });
});

function buildDocs() {
  console.log('Building Api documentation');

  const doc = new LeafDoc({
    templateDir: 'bin/leafdoc-templates',
    showInheritancesWhenEmpty: true,
    leadingCharacter: '@'
  });

  doc.addFile('src/query-options.leafdoc', false);
  doc.addFile('src/index.js', true);
  doc.addDir('src/api');
  doc.addDir('src/utils');

  const out = doc.outputStr();
  fs.writeFileSync(OUT_PATH, out);
}

buildDocs();
