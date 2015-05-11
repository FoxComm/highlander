'use strict';

module.exports = function(gulp, opts, $) {
  gulp.task('lint', function() {
    return gulp.src([opts.jsSrc, opts.serverSrc])
      .pipe($.eslint())
      .pipe($.eslint.format());
  });
}
