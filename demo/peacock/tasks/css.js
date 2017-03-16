const browserSync = require('browser-sync');
const cssnano = require('gulp-cssnano');
const _if = require('gulp-if');

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
      .pipe(_if(process.env.NODE_ENV === 'production', cssnano()))
      .pipe(gulp.dest('public'))
      .pipe(browserSync.stream({ match: '**/*.css' }));
  });


  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
