'use strict';

module.exports = function(gulp, opts, $) {
  const files = [
    opts.jsSrc,
    opts.serverSrc,
    opts.configSrc,
    opts.testSrc
  ];
  gulp.task('lint', function() {
    return gulp.src(files)
      .pipe($.eslint())
      .pipe($.eslint.format());
  });
}
