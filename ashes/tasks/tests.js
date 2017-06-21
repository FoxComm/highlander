'use strict';

const runSequence = require('run-sequence');

module.exports = function(gulp, opts, $) {
  gulp.task('test', function(cb) {
    runSequence('mocha', cb);
  });

  if (!process.env.ASHES_NO_WATCH_FOR_TEST) {
    gulp.task('test.watch', function() {
      gulp.watch([opts.configSrc, opts.jsSrc, opts.serverSrc, opts.testSrc], ['test']);
    });
  }
};
