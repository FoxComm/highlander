'use strict';

module.exports = function(gulp) {
  gulp.task('fonts', () => gulp.src(['src/fonts/**/*']).pipe(gulp.dest('public/fonts')));
};
