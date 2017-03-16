'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const opts = require('./config/gulp');

let exitStatus = 0;

for (const task of fs.readdirSync('./tasks')) {
  const file = path.join('./tasks', task);
  const taskModule = require(path.resolve(file));
  if (typeof taskModule == 'function') {
    taskModule(gulp, $, opts);
  }
}

gulp.task('build', function(cb) {
  runSequence('templates', 'browserify', 'css', 'images', cb);
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
