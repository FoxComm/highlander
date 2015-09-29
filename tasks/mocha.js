'use strict';

require('babel/register');
const path = require('path');

const mochaOpts = {
  reporter: 'dot',
  ui: 'bdd',
  timeout: 30000,
  require: ['co-mocha']
};

module.exports = function(gulp, opts, $) {
  let helper = path.join(opts.testDir, '/spec-helper.js');
  let specs = path.join(opts.testDir, '/specs/**/*.js');
  let acceptance = path.join(opts.testDir, '/acceptance/**/*.jsx');

  gulp.task('mocha.main', function() {
    return gulp.src([helper, specs, acceptance], {read: false})
      .pipe($.mocha(mochaOpts));
  });

  gulp.task('mocha.unit', function() {
    return gulp.src(path.join(opts.testDir, '/unit/**/*.js'), {read: false})
      .pipe($.mocha(mochaOpts));
  });

  gulp.task('mocha', ['mocha.unit', 'mocha.main']);
};
