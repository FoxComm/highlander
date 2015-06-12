'use strict';

const
  fs    = require('fs'),
  path  = require('path'),
  t     = require('thunkify-wrap'),
  fleck = require('fleck');

module.exports = function *() {
  let
    db      = {},
    models  = yield t(fs.readdir)(`${__dirname}/models`);
  db.models = {};

  for (let file of models) {
    let
      name      = path.basename(file, '.js'),
      modelName = fleck.camelize(name, true);

    let model = require(`${__dirname}/models/${file}`);
    db.models[modelName] = model;
  }

  return db;
};
