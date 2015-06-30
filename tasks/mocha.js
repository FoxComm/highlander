'use strict';

const
  path = require('path');

const mochaOpts = {
  reporter: 'dot',
  ui: 'bdd',
  timeout: 5000,
  require: ['co-mocha']
};

module.exports = function(gulp, opts, $) {
  let
    isTest  = (process.env.NODE_ENV === 'test'),
    helper  = path.join(opts.testDir, '/spec-helper.js'),
    specs   = path.join(opts.testDir, '/specs/**/*.js');

  gulp.task('mocha', function() {
    return gulp.src([helper, specs], {read: false})
      .pipe($.mocha(mochaOpts));
  });
}
