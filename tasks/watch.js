'use strict';

module.exports = function(gulp, opts, $) {
  gulp.task('watch', function() {
    opts.usePlumber = true;
    gulp.watch(opts.jsSrc, ['browserify']);
    gulp.watch(opts.lessSrc, ['less']);
    gulp.watch(opts.imageSrc, ['imagemin']);
    gulp.watch([opts.configSrc, opts.jsSrc, opts.serverSrc, opts.testSrc], ['test', 'server']);
  });
}
