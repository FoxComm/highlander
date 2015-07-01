'use strict';

const
  fs    = require('fs'),
  path  = require('path');

class Config {
  constructor(env) {
    env = env || (process.env.NODE_ENV || 'development');
    for (let file of fs.readdirSync(__dirname)) {
      if (file === 'index.js' || !/\.js$/.test(file)) continue;
      let name = path.basename(file, '.js');
      this[name] = require(`${__dirname}/${file}`)(env);
    }
  }
}

module.exports = Config;
