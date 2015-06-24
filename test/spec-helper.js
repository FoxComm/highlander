'use strict';

global.expect = require('chai').expect;

const
  path  = require('path'),
  Api   = require(path.resolve('server/lib/api'));

before(function(done) {
  this.api = new Api('http://localhost:3001/api/v1');
  this.app = require(path.resolve('server'));
  this.app.init().then(done).catch(function(err) {
    console.error(err.stack);
    done(err);
  });
});
