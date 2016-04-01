
module.exports = function(gulp) {
  const src = [
    'build/less_bundle.css',
    'build/css_bundle.css'
  ];

  gulp.task('css', function() {
    const concat = require('gulp-concat');

    return gulp.src(src)
      .pipe(concat('admin.css'))
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
