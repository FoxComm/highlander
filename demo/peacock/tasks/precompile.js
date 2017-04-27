'use strict';

const { spawn } = require('child_process');
const watch = require('glob-watcher');
const path = require('path');

function runScript(name, cb = () => {}) {
  let child = spawn('yarn',
    ['run', name],
    {
      shell: true,
      detached: true,
      stdio: 'inherit',
    }
  ).on('close', (code) => {
    child = null;
    if (code != 0) {
      cb(new Error(`"yarn run ${name}" process exited with code ${code}`));
    } else {
      cb();
    }
  }).on('error', (err) => {
    child = null;
    cb(err);
  });

  process.on('exit', () => {
    if (child) process.kill(-child.pid);
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

  const projectPath = path.resolve(__dirname, '../src');

  const logSrcToLib = (filepath) => {
    const fullPath = path.resolve(filepath);
    const relative = path.relative(projectPath, fullPath);

    console.info(`src/${relative} -> lib/${relative}`);
  };

  gulp.task('precompile.source', function () {
    return gulp.src('src/**/*.{jsx,js}')
      .pipe(changed('lib', {extension: '.js'}))
      .pipe(through.obj((file, enc, cb) => {
        logSrcToLib(file.path);
        cb(null, file);
      }))
      .pipe(babel())
      .pipe(gulp.dest('lib'));
  });

  gulp.task('precompile', ['precompile.static', 'precompile.source']);

  gulp.task('precompile.watch', function () {
    const handleChanged = (filepath) => {
      logSrcToLib(filepath);
      gulp
        .src(filepath, { base: 'src' })
        .pipe(gulp.dest('./lib'));
    };

    watch(statics)
      .on('change', handleChanged)
      .on('add', handleChanged);


    runScript('watch-precompile');
  });
};
