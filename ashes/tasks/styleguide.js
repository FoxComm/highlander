'use strict';

const { runScript } = require('./helpers');

module.exports = function (gulp) {
  gulp.task('styleguide-build', function (cb) {
    runScript(`styleguide:build`, cb);
  });
};
