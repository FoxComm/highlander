'use strict';

const path = require('path');
const { spawn } = require('child_process');

function runScript(name, cb = () => {}) {
  return spawn('yarn',
    ['run', name],
    {
      shell: true,
      stdio: 'inherit',
    }
  ).on('close', code => {
    if (code != 0) {
      cb(new Error(`"yarn run ${name}" process exited with code ${code}`));
    } else {
      cb();
    }
  }).on('error', err => {
    cb(err);
  });
}

const statics = ['src/**/*.css', 'src/**/*.json'];

module.exports = function (gulp) {
  gulp.task('precompile.static', function () {
    return gulp.src(statics)
      .pipe(gulp.dest('lib'));
  });

  gulp.task('precompile.source', function (cb) {
    runScript('precompile', cb);
  });

  gulp.task('precompile', ['precompile.static', 'precompile.source']);

  gulp.task('precompile.watch', function () {
    gulp.watch(statics).on('change', file => {
      const from = path.relative(process.cwd(), file.path);
      const to = from.replace('src/', 'lib/');
      console.info(`${from} -> ${to}`);
      gulp
        .src(file.path, { base: 'src' })
        .pipe(gulp.dest('./lib'));
    });

    runScript(`watch-precompile`);
  });
};
