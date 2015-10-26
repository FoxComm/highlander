'use strict';


const path = require('path');

const mochaOpts = {
  reporter: 'dot',
  ui: 'bdd',
  timeout: 30000,
  require: [
    'co-mocha',
    './test/spec-helper',
    './test/acceptance/setup'
  ]
};

module.exports = function(gulp, opts, $) {
  let specs = path.join(opts.testDir, '/specs/**/*.js');
  let acceptance = path.join(opts.testDir, '/acceptance/**/*.jsx');

  gulp.task('mocha.main', function() {
    return gulp.src([specs, acceptance], {read: false})
      .pipe($.mocha(mochaOpts));
  });

  gulp.task('mocha.unit', function() {
    return gulp.src(path.join(opts.testDir, '/unit/**/*.js'), {read: false})
      .pipe($.mocha(mochaOpts));
  });

  gulp.task('mocha', ['mocha.unit', 'mocha.main']);
};
