
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
    this.parentNamespace = 'FoxApi';
  }

  addSection(name) {
    this.sections.push({
      head: [],
      body: [],
      name: name,
      deps: []
    });
  }

  addDependency(type, parentsMap) {
    const types = type.split(/[<>]/);
    types.map(type => {
      if (type.length) {
        this.lastSection.deps.push(type);
        const name = this.lastSection.name;

        // by default dependants inherits parent
        if ((name in parentsMap) && !parentsMap[type]) {
          parentsMap[type] = parentsMap[name];
        }
      }
    });
  }

  setParentNamespace(parentNamespace) {
    this.parentNamespace = parentNamespace;
  }

  addStructure(structureName) {
    this.addSection(structureName);
  }

  makeHeadDefinition(name) {
    return `\n@miniclass ${name} (${this.parentNamespace})\n@aka ${name.toLowerCase()}`
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
    return sections.map(s => {
      const head = [
        this.makeHeadDefinition(s.name),
        ...s.head,
      ];
      return [head.join('\n'), s.body.join('\n')].join('\n');
    }).join('\n');
  }

  toString() {
    return this.stringify(this.sections);
  }

  filterAndDump(names) {
    const sections = this.sections.filter(s => s.name in names);
    if (sections.length) {
      return this.stringify(sections);
    }
  }

  indexDeps(acc = {}) {
    return this.sections.reduce((acc, s) => {
      acc[s.name] = s.deps;
      return acc;
    }, acc);
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

function apiaryToLeafdoc(str, namesMap, parentsMap) {
  const attrRe = /\+\s+(\w+):?\s*(\d+|`[^\`]+`|[^\s]+)?\s+\(([^)]+)\)\s*\-\s*(.*)/;
  const directiveRe = /[\-+]\s*(\w+)\s+(.*)/;
  const doc = new Leafdoc();

  let structureName = null;
  let linePadding = 0;
  let prevLinePadding = 0;
  let structureContext = null;

  let structMembers = [];

  // @todo: add support for renaming complex type, i.e. Array<Type1> -> Array<Type2>
  const rename = name => namesMap[name] || name;
  let parentNamespace = 'FoxApi';

  str.toString().split(/\n\r?/).forEach(line => {
    if (line.startsWith('##')) {
      structureName = rename(line.replace(/[\#\s]/g, ''));
      if (structureName in parentsMap) {
        parentNamespace = parentsMap[structureName];
      }
      doc.addStructure(structureName);
    }
    linePadding = 0;
    if (/^\s+/.test(line)) {
      linePadding = /^\s+/.exec(line)[0].length;
      line = line.replace(/^\s+/, '');
    }
    if (linePadding === 0 && /^[+\-]/.test(line) && structureName) {
      let split = attrRe.exec(line);
      if (split) {
        const name = split[1];
        const example = split[2];
        const attrs = split[3].split(/[\,\s]+/);
        let description = split[4];
        if (!description.endsWith('.')) {
          description = description + '.'; // Add a point to let style to be similar
        }

        const type = rename(convertType(attrs[attrs.length - 1]));
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
        doc.addDependency(type, parentsMap);
        return;
      }

      split = directiveRe.exec(line);
      if (split) {
        const name = split[1];
        if (name.toLowerCase() === 'include') {
          const type = rename(split[2]);
          doc.writeHead(`@inherits ${type}`);
          doc.addDependency(type, parentsMap);
        }
      }
    }
    if (linePadding < prevLinePadding) {
      if (structureContext === 'members' && structMembers.length > 0) {
        doc.writeBody(
          'Available values:',
          '<ul class="collapsible collapsed">',
          structMembers.map(name => `<li>${name}</li>`).join('\n'),
          '</ul>'
        );
      }
      structureContext = null;
      structMembers = [];
    }
    if (linePadding && structureContext == 'members') {
      const memberName = /\+\s+(\w+|`[^\`]+`)/.exec(line)[1];
      structMembers.push(memberName);
    }
    if (prevLinePadding === 0 && linePadding > 0) {
      structureContext = /\+\s+(\w+)/.exec(line)[1].toLowerCase();
    }
    prevLinePadding = linePadding;
  });

  doc.setParentNamespace(parentNamespace);
  return doc;
}

function rename(filename, newExt) {
  return `${path.basename(filename, path.extname(filename))}${newExt}`;
}

// format - [ParentNamespace]: '[namespace]:name'
const importData = {
  'Addresses': [
    'CreateAddressPayload',
    'UpdateAddressPayload',
    'Address',
    'Addresses:AddressesResponse',
  ],
  'CreditCards': [
    'CreditCard',
    'CreditCards:CreditCardsResponse',
    'CreditCardCreatePayload',
    'BillingAddressCreatePayload',
    'CreditCardUpdatePayload',
    'IssueCreditCardTokenPayload',
    'CreditCardTokenResponse',
  ],
  'StoreCredits': [
    'StoreCredit',
    'StoreCreditAdjustments',
    'StoreCreditTotals',
    'StoreCreditCreateSinglePayload',
  ],
  'Cart': [
    'UpdateLineItemsPayload',
    'GiftCardPaymentPayload',
  ],
  'FoxApi': [
    'ShippingMethod',
    'ValidationResult',
  ],
  'Customers': [
    'CustomerCreatePayload',
    'CustomerUpdatePayload',
    'CustomerResponse',
  ],
  'CustomerGroups': [
    'GroupPayload',
    'GroupResponse',
  ],
  'Skus': [
    'SkuPayload',
    'SkuResponse',
  ],
  'Products': [
    'ProductPayload',
    'ProductResponse',
  ],
  'GiftCards': [
    'GiftCard',
    'GiftCardCreatePayload',
    'GiftCardUpdateStatePayload',
  ],
  'Promotions': [
    'PromotionPayload',
    'PromotionIlluminated',
  ],
  'Coupons': [
    'CouponPayload',
    'Coupon',
    'CouponCode',
    'GenerateCodesPayload',
  ],
  'Albums': [
    'AlbumPayload',
    'AlbumResponse',
    'Image',
  ],
  'Notes': [
    'NotePayload',
    'AdminNote',
  ],
  'Watchers': [
    'Watcher',
    'AddWatchersPayload',
  ],
  'Inventories': [
    'InventoryResponse',
    'ShipmentResponse',
    'ModifyInventoryItemQuantityPayload',
  ],
  'Orders': [
    'FullOrder',
    'OrderStatePayload',
  ],
  'SharedSearches': [
    'SharedSearch',
    'SharedSearchPayload',
    'SharedSearchAssociationPayload',
  ],
  'StoreAdmins': [
    'StoreAdmin',
    'StoreAdminCreatePayload',
    'StoreAdminUpdatePayload',
  ],
};

function convertDocs() {
  console.log('Import apiary objects descriptions');
  mkdirp.sync(outPath);

  const parentsMap = Object.create(null);
  let structuresToImport = [];
  _.each(importData, (names, parentName) => {
    _.each(names, name => parentsMap[name] = parentName);
    structuresToImport = [...structuresToImport, ...names];
  });

  const structureNames = structuresToImport.map(name => name.split(':').slice(-1)[0])
  const newNames = structuresToImport.reduce((acc, name) => {
    const [realName, newName] = name.split(':');
    if (newName) {
      acc[realName] = newName;
    }
    return acc;
  }, {});

  const depsTree = {};
  const docs = collectDocs().map(([filename, contents]) => {
    const leafdoc = apiaryToLeafdoc(contents, newNames, parentsMap);
    leafdoc.indexDeps(depsTree);
    leafdoc.filename = rename(filename, '.leafdoc');
    return leafdoc;
  });

  const deps = {};
  const collectDeps = (depNames) => {
    for (const name of depNames)
      if (!deps[name]) {
        deps[name] = true;
        collectDeps(depsTree[name] || []);
      }
  }

  collectDeps(structureNames);

  for (const doc of docs) {
    const value = doc.filterAndDump(deps);
    if (value) {
      fs.writeFileSync(path.join(outPath, doc.filename), value);
    }
  }
}

if (docsPath && outPath) {
  convertDocs();
} else {
  console.log('Usage example:');
  console.log('APIARY_DOCS_PATH=../docs/apiary-objects node bin/import-docs.js');
  process.exit(1);
}
