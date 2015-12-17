'use strict';

const
  fs    = require('fs'),
  path  = require('path');

class Config {
  constructor(environment) {
    const env = {
      environment: environment,
      phoenix_url: (process.env.PHOENIX_URL || 'http://localhost:9090')
    };

    for (let file of fs.readdirSync(__dirname)) {
      if (file === 'index.js' || !/\.js$/.test(file)) continue;
      let name = path.basename(file, '.js');
      this[name] = require(`${__dirname}/${file}`)(env);
    }
  }
}

module.exports = Config;
