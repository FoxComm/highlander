'use strict';

const
  fs = require('fs'),
  path = require('path');

for (let file of fs.readdirSync(__dirname)) {
  if (file === 'index.js' || !/\.js$/.test(file)) continue;
  let name = path.basename(file, '.js');
  exports[name] = require(`${__dirname}/${file}`);
}
