'use strict';


module.exports = function(gulp, opts) {
  gulp.task('precompile', function() {
    gulp.src(['src/**/*.css', 'src/**/*.json'])
      .pipe(gulp.dest('lib'));
  });
}
