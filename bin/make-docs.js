
const LeafDoc = require('leafdoc');
const fs = require('fs');
const escapeHtml = require('escape-html');

const OUT_PATH = process.env.DOCS_PATH || 'index.html';

function setupHandlebars(handlebars) {
  const makeLink = handlebars.helpers.type;
  const typeRe = /[\w\._\s]+/g;

  handlebars.registerHelper('type', function(str, options) {
    if (!str) {
      throw new Error(`Calling type helper on null type, key: ${options.data.key}`)
    }
    return str
      .replace(/[\<\>]/g, function(str) {
        return escapeHtml(str);
      })
      .replace(typeRe, function(typeStr) {
        return makeLink(typeStr);
      });
  });
}

function buildDocs() {
  console.log('Building Api documentation');

  const doc = new LeafDoc({
    templateDir: 'bin/leafdoc-templates',
    showInheritancesWhenEmpty: true,
    leadingCharacter: '@'
  });

  setupHandlebars(doc.getTemplateEngine());
  doc.registerDocumentable('field', 'Fields', true);
  doc.registerDocumentable('key', 'Keys', true);

  doc.addDir('docs/objects');
  doc.addFile('src/query-options.leafdoc', false);
  doc.addFile('src/index.js', true);
  doc.addDir('src/api');
  doc.addDir('src/utils');

  const out = doc.outputStr();
  fs.writeFileSync(OUT_PATH, out);
}

buildDocs();
