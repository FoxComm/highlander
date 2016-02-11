'use strict';

const cssSrc = 'src/**/*.css';

module.exports = function(gulp, opts, $) {

  const autoprefixer = require('autoprefixer');

  gulp.task('css', function () {
    return gulp.src(cssSrc)
      .pipe($.if(opts.devMode, $.plumber(function (err) {
        console.error(err);
        this.emit('end');
      })))
      .pipe($.sourcemaps.init())
      .pipe($.postcss([
        autoprefixer({browsers: ['last 2 versions']}),
        require('precss')
      ]))
      .pipe($.sourcemaps.write('.'))
      .pipe(gulp.dest('build/'));
  });

  gulp.task('css.watch', function () {
    gulp.watch(cssSrc, ['css']);
  });

};
