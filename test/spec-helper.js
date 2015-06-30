'use strict';

global.expect = require('chai').expect;

const
  path  = require('path'),
  Api   = require(path.resolve('server/lib/api'));

before(function(done) {
  let _this = this;
  this.app = require(path.resolve('server'));
  this.app.init('test')
    .then(function() {
      let port = _this.app.config.server.port;
      _this.api = new Api(`http://localhost:${port}/api/v1`);
      done();
    })
    .catch(function(err) {
      console.error(err.stack);
      done(err);
    });
});

after(function(done) {
  this.app.server.close(done);
});
