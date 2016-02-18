
module.exports = function(gulp, $) {
  const src = [
    'node_modules/evil-icons/assets/evil-icons.css',
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
