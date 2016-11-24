const browserSync = require('browser-sync');

module.exports = function(gulp, $) {
  const src = [
    'src/css/fonts.css',
    'node_modules/evil-icons/assets/evil-icons.css',
    'src/css/reset.css',
    'src/css/inputs.css',
    'node_modules/wings/lib/bundle.css',
    'build/bundle.css',
    'node_modules/slick-carousel/slick/slick.css',
    'node_modules/slick-carousel/slick/slick-theme.css',
  ];

  gulp.task('css', function() {
    return gulp.src(src)
      .pipe($.concat('app.css'))
      .pipe(gulp.dest('public'))
      .pipe(browserSync.stream({ match: '**/*.css' }));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
