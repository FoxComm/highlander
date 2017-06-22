'use strict';

const fs = require('fs');
const path = require('path');

class Config {
  constructor(environment) {
    const env = {
      environment: environment,
      public_key: process.env.PHOENIX_PUBLIC_KEY,
    };

    for (let file of fs.readdirSync(__dirname)) {
      if (file === 'index.js' || !/\.js$/.test(file)) continue;
      let name = path.basename(file, '.js');
      this[name] = require(`${__dirname}/${file}`)(env);
    }
  }
}

module.exports = Config;
