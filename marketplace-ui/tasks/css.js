const gulpif = require('gulp-if');
const cleanCSS = require('gulp-clean-css');

module.exports = function(gulp, $) {
  const production = (process.env.NODE_ENV === 'production');

  const src = [
    'src/css/fonts.css',
    'src/css/reset.css',
    'src/css/inputs.css',
    'build/bundle.css',
  ];

  gulp.task('css', function() {
    return gulp.src(src)
      .pipe($.concat('app.css'))
      .pipe(gulpif(production, cleanCSS()))
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
