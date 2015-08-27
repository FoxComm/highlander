'use strict';

const
  path      = require('path'),
  merge     = require('merge-stream'),
  pngquant  = require('imagemin-pngquant');

module.exports = function(gulp, opts, $) {
  gulp.task('imagemin', function() {
    let themes = opts.getThemes(opts.themeDir);
    let tasks = themes.map(function(theme) {
      return gulp.src(opts.imageSrc)
        .pipe($.imagemin({
          progressive: true,
          svgoPlugins: [{removeViewBox: false}],
          use: [pngquant()]
        }))
        .pipe(gulp.dest(path.join(opts.publicDir, 'themes', theme)));
    });

    return merge(tasks);
  });

  gulp.task('imagemin.watch', function() {
    gulp.watch(opts.imageSrc, ['imagemin']);
  });
};
