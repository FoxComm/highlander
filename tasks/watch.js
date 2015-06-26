'use strict';

module.exports = function(gulp, opts, $) {
  gulp.task('watch', function() {
    gulp.watch(opts.jsSrc, ['browserify', 'server']);
    gulp.watch(opts.lessSrc, ['less']);
    gulp.watch(opts.imageSrc, ['imagemin']);
    gulp.watch([opts.jsSrc, opts.serverSrc], ['lint', 'mocha']);
    gulp.watch(opts.serverSrc, ['server']);
  });
}
