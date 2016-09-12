
module.exports = function(gulp, $) {
  const src = [
    'src/css/fonts.css',
    'src/css/reset.css',
    'src/css/inputs.css',
    'build/bundle.css',
  ];

  gulp.task('css', function() {
    return gulp.src(src)
      .pipe($.concat('app.css'))
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
