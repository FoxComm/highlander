'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const opts = require('./config/gulp');
const revAll = require('gulp-rev-all');
const revdel = require('gulp-rev-delete-original');
const del = require('del');

let exitStatus = 0;

for (const task of fs.readdirSync('./tasks')) {
  const file = path.join('./tasks', task);
  const taskModule = require(path.resolve(file));
  if (typeof taskModule == 'function') {
    taskModule(gulp, $, opts);
  }
}

gulp.task('build', function(cb) {
  const buildTasks = ['templates', 'browserify', 'css', 'images', 'fonts'];
  let tasks = buildTasks;

  if (process.env.NODE_ENV === 'production') {
    tasks = [
      'build:clean',
      ...buildTasks,
      'css-min',
      'rev',
    ];
  }

  runSequence.apply(this, tasks.concat(cb));
});

gulp.task('build:clean', function() {
  return del(['build', 'public/{*.js,*.css,fonts,images}']);
});

gulp.task('rev', function () {
  gulp
    .src('public/**/*')
    .pipe(revAll.revision({
      dontRenameFile: [/^\/favicon.png$/g],
      includeFilesInManifest: ['.css', '.js', '.jpg', '.png'],
    }))
    .pipe(revdel())
    .pipe(gulp.dest('public'))
    .pipe(revAll.manifestFile())
    .pipe(gulp.dest('build'));
});

gulp.task('dev', function(cb) {
  opts.devMode = true;

  const tasks = _.compact([
    'build',
    'server',
    'watch',
    'interactivity',
    opts.enableNotifier ? 'notifier' : null,
  ]);

  runSequence.apply(this, tasks.concat(cb));
});

gulp.task('default', ['build']);

function handleErrors(err) {
  if (err) {
    console.error(err && err.stack);
  }
  exitStatus = 1;
  $.util.beep();
}

process.on('unhandledRejection', handleErrors);
process.on('uncaughtException', handleErrors);

gulp.on('err', handleErrors);

process.on('exit', () => {
  process.exit(exitStatus);
});
