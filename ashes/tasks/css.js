
module.exports = function(gulp, opts) {
  const src = [
    'build/less_bundle.css',
    'build/css_bundle.css',
  ];

  gulp.task('css', function() {
    const concat = require('gulp-concat');
    const cssnano = require('gulp-cssnano');
    const _if = require('gulp-if');

    return gulp.src(src)
      .pipe(concat('admin.css'))
      .pipe(_if(process.env.NODE_ENV === 'production', cssnano()))
      .pipe(gulp.dest(opts.assetsDir));
  });

  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });
};
