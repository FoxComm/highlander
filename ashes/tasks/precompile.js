'use strict';

const { spawn } = require('child_process');

function runScript(name) {
  return spawn('yarn',
    ['run', name],
    {
      shell: true,
      stdio: 'inherit',
    }
  );
}


module.exports = function(gulp, opts) {
  gulp.task('precompile.static', function() {
    return gulp.src(['src/**/*.css', 'src/**/*.json'])
      .pipe(gulp.dest('lib'));
  });

  gulp.task('precompile.source', function(cb) {
    runScript('precompile').on('close', () => cb());
  });

  gulp.task('precompile', ['precompile.static', 'precompile.source']);

  gulp.task('precompile-source.watch', function() {
    runScript(`watch-precompile`);
  });

  gulp.task('precompile-static.watch', function() {
    gulp.watch(['src/**/*.css', 'src/**/*.json'], ['precompile.static']);
  });
};
