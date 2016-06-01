
const _ = require('lodash');
const path = require('path');
const fs = require('fs');
const mkdirp = require('mkdirp');

const docsPath = process.env.APIARY_DOCS_PATH;
const outPath = path.resolve(__dirname, '../docs/objects');

function collectDocs() {
  const files = fs.readdirSync(docsPath);

  return files
    .map(filename => [filename, fs.readFileSync(path.join(docsPath, filename))]);
}

class Leafdoc {
  constructor() {
    this.sections = [];
  }

  addSection(name) {
    this.sections.push({
      head: [],
      body: [],
      name: name,
      deps: []
    });
  }

  addStructure(structureName) {
    this.addSection(structureName);
    this.writeHead(
      `\n@miniclass ${structureName} (FoxApi)`,
      `@aka ${structureName.toLowerCase()}`
    );
  }

  get lastSection() {
    if (!this.sections.length) {
      this.addSection();
    }

    return this.sections[this.sections.length - 1];
  }

  writeHead(...args) {
    this.lastSection.head.push(...args);
  }

  writeBody(...args) {
    this.lastSection.body.push(...args);
  }

  stringify(sections) {
    return sections.map(s => [s.head.join('\n'), s.body.join('\n')].join('\n')).join('\n');
  }

  toString() {
    return this.stringify(sections);
  }

  filterAndDump(names) {
    const sections = this.sections.filter(s => s.name in names);
    if (sections.length) {
      return this.stringify(sections);
    }
  }

  getDepsFor(name, deps = {}) {
    return this.sections.reduce((acc, s) => {
      if (s.name === name) {
        indexArray(s.deps, acc);
      }
      return acc;
    }, deps);
  }
}


function convertType(type) {
  const complexType = /(\w+)\[(\w+)\]/;
  const split = complexType.exec(type);

  if (split) {
    const superType = split[1];
    const subType = split[2];

    if (superType == 'enum') {
      // drop super type at all
      return _.upperFirst(subType);
    } else {
      return `${_.upperFirst(superType)}<${_.upperFirst(subType)}>`;
    }
  }
  return _.upperFirst(type);
}

function apiaryToLeafdoc(str) {
  const attrRe = /\+\s+(\w+):?\s*(\d+|`[^\`]+`)?\s+\(([^)]+)\)\s*\-\s*(.*)/;
  const directiveRe = /[\-+]\s*(\w+)\s+(.*)/;
  const doc = new Leafdoc();

  let structureName = null;

  str.toString().split(/\n\r?/).forEach(line => {
    if (line.startsWith('###')) {
      structureName = line.replace(/[\#\s]/g, '');
      doc.addStructure(structureName);
    }
    if (/^[+\-]/.test(line) && structureName) {
      let split = attrRe.exec(line);
      if (split) {
        const name = split[1];
        const example = split[2];
        const attrs = split[3].split(/[\,\s]+/);
        let description = split[4];
        if (!description.endsWith('.')) {
          description = description + '.'; // Add a point to let style to be similar
        }

        const type = convertType(attrs[attrs.length-1]);
        const defaultPart = example ? ` = ${example}` : '';
        let required = true;
        if (attrs.indexOf('optional') != -1) {
          required = false;
        }

        const nameSuffix = required ? '' : '?';

        doc.writeBody(
          `\n@field ${name}${nameSuffix}: ${type}${defaultPart}`,
          `${description}`
        );
        doc.lastSection.deps.push(type);
        return;
      }

      split = directiveRe.exec(line);
      if (split) {
        const name = split[1];
        if (name.toLowerCase() === 'include') {
          const type = split[2];
          doc.writeHead(`@inherits ${type}`);
          doc.lastSection.deps.push(type);
        }
      }
    }
  });

  return doc;
}

function indexArray(arr, hash = {}) {
  arr.forEach(value => hash[value] = true);
  return hash;
}

function rename(filename, newExt) {
  return `${path.basename(filename, path.extname(filename))}${newExt}`;
}

const structuresToImport = [
  'CreateAddressPayload',
  'UpdateAddressPayload',
  'Address'
];

function convertDocs() {
  mkdirp.sync(outPath);
  const allDeps = indexArray(structuresToImport);
  collectDocs()
    .map(([filename, contents]) => {
      const leafdoc = apiaryToLeafdoc(contents);
      structuresToImport.forEach(name => {
        leafdoc.getDepsFor(name, allDeps);
      });
      leafdoc.filename = rename(filename, '.leafdoc');

      return leafdoc;
    })
    .map(leafdoc => {

      const value = leafdoc.filterAndDump(allDeps);
      if (value) {
        fs.writeFileSync(path.join(outPath, leafdoc.filename), value);
      }
    });
  console.log(allDeps);
}

if (docsPath && outPath) {
  convertDocs();
} else {
  console.log('Usage example:');
  console.log('APIARY_DOCS_PATH=../docs/apiary-objects node bin/import-docs.js');
  process.exit(1);
}

