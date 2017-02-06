'use strict';

module.exports = function(gulp) {
  gulp.task('images', () => {
    return gulp
      .src(['src/images/**/*'])
      .pipe(gulp.dest('public/images'));
  });
};
