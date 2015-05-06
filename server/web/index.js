'use strict';

const
  Router      = require('koa-router'),
  fs          = require('fs'),
  path        = require('path'),
  _           = require('underscore'),
  htmlescape  = require('htmlescape');

module.exports = function(app) {
  let
    config    = app.config,
    router    = new Router(),
    template  = path.join(__dirname, '../views/layout.tmpl');
};
