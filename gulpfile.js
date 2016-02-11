'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const opts = require('./config/gulp');

let exitStatus = 0;

for (let task of fs.readdirSync('./tasks')) {
  const file = path.join('./tasks', task);
  require(path.resolve(file))(gulp, opts, $);
}

gulp.task('build', ['browserify']);

gulp.task('dev', function(cb) {
  opts.devMode = true;

  let tasks = _.compact([
    'build',
    'server',
    'watch',
    opts.enableNotifier ? 'notifier' : null
  ]);

  runSequence.apply(this, tasks.concat(cb));
});

gulp.task('default', ['build']);

function handleErrors(err) {
  if (err) {
    console.error(err);
  }
  exitStatus = 1;
  $.util.beep();
}

process.on('unhandledRejection', handleErrors);
process.on('uncaughtException', handleErrors);

gulp.on('err', handleErrors);

process.on('exit', function() {
  process.exit(exitStatus);
});
