'use strict';

module.exports = function(gulp) {
  gulp.task('favicon', () => gulp.src('favicon.ico').pipe(gulp.dest('public')));
};
