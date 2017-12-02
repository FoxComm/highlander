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
    'node_modules/slick-carousel/slick/slick.css',
    'node_modules/slick-carousel/slick/slick-theme.css',
    'node_modules/@foxcommerce/wings/lib/bundle.css',
    'public/common.css',
    'build/bundle.css',
  ];

  gulp.task('css', ['css.common'], () => {
    return gulp.src(src)
      .pipe($.concat('app.css'))
      .pipe(_if(process.env.NODE_ENV === 'production', cssnano()))
      .pipe(gulp.dest('public'))
      .pipe(browserSync.stream({ match: '**/*.css' }));
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
      .pipe(gulp.dest('public'));
  });

  gulp.task('css.watch', function() {
    gulp.watch([...src, ...commonSrc], ['css']);
  });
};
