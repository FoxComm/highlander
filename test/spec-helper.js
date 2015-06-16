'use strict';

global.expect = require('chai').expect;

const
  path = require('path');

before(function(done) {
  this.Api = require('./api');
  this.app = require(path.resolve('server'));
  this.app.init().then(done).catch(function(err) {
    console.error(err.stack);
    done(err);
  });
});
