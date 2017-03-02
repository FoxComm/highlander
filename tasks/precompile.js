'use strict';

const { spawn } = require('child_process');

function runScript(name, cb = () => {}) {
  const child = spawn('yarn',
    ['run', name],
    {
      shell: true,
      detached: true,
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

  process.on('exit', () => {
    process.kill(-child.pid);
  });

  return child;
}

const statics = ['src/**/*.css', 'src/**/*.json'];

module.exports = function (gulp) {
  const babel = require('gulp-babel');
  const changed = require('gulp-changed');
  const through = require('through2');

  gulp.task('precompile.static', function () {
    return gulp.src(statics)
      .pipe(gulp.dest('lib'));
  });

  const logBabelified = file => {
    console.info(`src/${file.relative} -> lib/${file.relative}`);
  };

  gulp.task('precompile.source', function () {
    return gulp.src('src/**/*.{jsx,js}')
      .pipe(changed('lib', {extension: '.js'}))
      .pipe(through.obj((file, enc, cb) => {
        logBabelified(file);
        cb(null, file);
      }))
      .pipe(babel())
      .pipe(gulp.dest('lib'));
  });

  gulp.task('precompile', ['precompile.static', 'precompile.source']);

  gulp.task('precompile.watch', function () {
    gulp.watch(statics).on('change', file => {
      logBabelified(file);
      gulp
        .src(file.path, { base: 'src' })
        .pipe(gulp.dest('./lib'));
    });

    runScript(`watch-precompile`);
  });
};
