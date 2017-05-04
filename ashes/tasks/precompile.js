'use strict';

const path = require('path');
const { runScript } = require('./helpers');

process.on('SIGINT', () => {
  console.log('SIGINT. Exiting...');
  process.exit();
});

process.on('uncaughtException', () => {
  console.log('uncaughtException. Exiting...');
  process.exit();
});


const statics = ['src/**/*.css', 'src/**/*.json'];

module.exports = function (gulp, opts) {
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
    return gulp.src(['src/**/*.{jsx,js}', '!src/**/*.stories.js'])
      .pipe(changed('lib', { extension: '.js' }))
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
