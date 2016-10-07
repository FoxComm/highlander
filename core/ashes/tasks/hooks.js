'use strict';

const fs = require('fs');
const runSequence = require('run-sequence');

function copy(src, dest, mode) {
  mode = mode || 0o755;

  return fs.createReadStream(src)
    .pipe(fs.createWriteStream(dest, {mode: mode}));
}

const HOOK = './hooks/validate.sh';
const PRE_PUSH = '.git/hooks/pre-push';

module.exports = function(gulp, opts, $) {
  gulp.task('hooks', function(cb) {
    copy(HOOK, PRE_PUSH)
      .on('finish', function() {
        cb();
      })
      .on('error', cb);
  });

  gulp.task('hooks.run', function(cb) {
    runSequence('lint', 'mocha', cb);
  });
};
