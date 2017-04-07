const browserSync = require('browser-sync');
const cssnano = require('gulp-cssnano');
const _if = require('gulp-if');

module.exports = function(gulp, $) {
  const src = [
    'public/css/fonts.css',
    'node_modules/evil-icons/assets/evil-icons.css',
    'public/css/reset.css',
    'public/css/inputs.css',
    'node_modules/react-image-gallery/styles/css/image-gallery.css',
    'node_modules/@foxcomm/wings/lib/bundle.css',
    'public/common.css',
    'public/css/overrides.css',
    'build/bundle.css',
  ];

  gulp.task('css', ['css.common'], () => {
    return gulp.src(src)
      .pipe($.concat('app.css'))
      .pipe(_if(process.env.NODE_ENV === 'production', cssnano()))
      .pipe(gulp.dest('public'))
      .pipe(browserSync.stream({ match: '**/*.css' }));
  });


  gulp.task('css.watch', function() {
    gulp.watch(src, ['css']);
  });

  const commonSrc = [
    'src/css/common/*.css',
  ];

  gulp.task('css.common', () => {
    const postcss = require('gulp-postcss');
    const { plugins } = require('../src/postcss.config');

    return gulp.src(commonSrc)
      .pipe(postcss(plugins))
      .pipe($.concat('common.css'))
      .pipe(_if(process.env.NODE_ENV === 'production', cssnano()))
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.common.watch', function() {
    gulp.watch(src, ['css.common']);
  });
};
