'use strict';

module.exports = function (gulp, opts, $) {
  gulp.task('test', ['lint', 'mocha']);

  gulp.task('test.watch', function() {
    gulp.watch([opts.configSrc, opts.jsSrc, opts.serverSrc, opts.testSrc], ['test']);
  });
};
