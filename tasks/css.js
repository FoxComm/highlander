const browserSync = require('browser-sync');
const cleanCSS = require('gulp-clean-css');

module.exports = function(gulp, $) {
  const src = [
    'src/css/fonts.css',
    'node_modules/evil-icons/assets/evil-icons.css',
    'src/css/reset.css',
    'src/css/inputs.css',
    'node_modules/@foxcomm/wings/lib/bundle.css',
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

  gulp.task('css-min', function () {
    return gulp.src('public/app.css')
      .pipe(cleanCSS({ restructuring: false }))
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
