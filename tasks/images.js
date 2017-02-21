'use strict';

const imagemin = require('gulp-imagemin');
const _if = require('gulp-if');

module.exports = function(gulp) {
  gulp.task('images', () => {
    return gulp
      .src(['src/images/**/*', '!src/images/{svg,svg/**/*}'])
      .pipe(_if(process.env.NODE_ENV === 'production', imagemin()))
      .pipe(gulp.dest('public/images'));
  });
};
