'use strict';

const path = require('path');
const runSequence = require('run-sequence');

const mochaOpts = {
  reporter: 'dot',
  ui: 'bdd',
  timeout: 30000,
  require: [
    'co-mocha',
    './test/spec-helper'
  ]
};

module.exports = function(gulp, opts, $) {
  let specs = path.join(opts.testDir, '/specs/**/*.js');
  let acceptance = path.join(opts.testDir, '/acceptance/**/*.jsx');

  gulp.task('mocha.main', function() {

    const acceptanceOpts = Object.assign({}, mochaOpts);
    acceptanceOpts.require = mochaOpts.require.concat(['./test/acceptance/setup']);

    return gulp.src([specs, acceptance], {read: false})
      .pipe($.mocha(acceptanceOpts));
  });

  gulp.task('mocha.unit', function() {
    return gulp.src(path.join(opts.testDir, '/unit/**/*.js'), {read: false})
      .pipe($.mocha(mochaOpts));
  });

  gulp.task('mocha', function(cb) {
    runSequence('mocha.unit', 'mocha.main', cb);
  });
};
