'use strict';

const
  path      = require('path'),
  merge     = require('merge-stream');

module.exports = function(gulp, opts, $) {
  gulp.task('images', function() {
    return gulp.src(opts.imageSrc)
      .pipe(gulp.dest(opts.assetsDir));
  });

  gulp.task('images.watch', function() {
    gulp.watch(opts.imageSrc, ['images']);
  });
};
