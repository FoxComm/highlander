'use strict';

const path = require('path');
const runSequence = require('run-sequence');
const mocha = require('gulp-spawn-mocha');

module.exports = function(gulp, opts) {
  const specs = path.join(opts.srcDir, '/**/*.spec.jsx');
  const acceptance = path.join(opts.testDir, '/acceptance/**/*.jsx');

  gulp.task('mocha.main', function() {
    const setup = path.join(opts.testDir, '/acceptance/_setup.js');

    return gulp.src([setup, specs, acceptance], { read: false }).pipe(mocha());
  });

  gulp.task('mocha.unit', function() {
    const unitTests = path.join(opts.testDir, '/unit/**/*.js');

    return gulp.src([unitTests], { read: false }).pipe(mocha());
  });

  gulp.task('mocha', function(cb) {
    runSequence('mocha.unit', 'mocha.main', cb);
  });
};
