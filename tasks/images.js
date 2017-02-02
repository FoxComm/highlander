'use strict';

module.exports = function(gulp) {
  gulp.task('images', () => {
    gulp
      .src(['src/images/**/*'])
      .pipe(gulp.dest('public/images'));

    gulp
      .src(['src/images/favicon.png'])
      .pipe(gulp.dest('public'));
  });
};
