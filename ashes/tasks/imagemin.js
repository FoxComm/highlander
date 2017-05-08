'use strict';

const
  path      = require('path'),
  merge     = require('merge-stream'),
  pngquant  = require('imagemin-pngquant');

module.exports = function(gulp, opts, $) {
  gulp.task('imagemin', function() {
    return gulp.src(opts.imageSrc)
      .pipe($.imagemin({
        progressive: true,
        svgoPlugins: [{removeViewBox: false}],
        use: [pngquant()]
      }))
      .pipe(gulp.dest(opts.assetsDir));
  });

  gulp.task('imagemin.watch', function() {
    gulp.watch(opts.imageSrc, ['imagemin']);
  });
};
