'use strict';

const _ = require('lodash');
const fs = require('fs');
const del = require('del');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const Config = require('./config');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./lib')}`;

const opts = new Config().gulp;

let exitStatus = 0;

for (let task of fs.readdirSync(opts.taskDir)) {
  let file = path.join(opts.taskDir, task);
  require(file)(gulp, opts, $);
}

gulp.task('clean', () => del(['build/**/*', 'lib/**/*']));

gulp.task('build', function(cb) {
  runSequence('clean', 'imagemin', 'less','sprites','precompile', 'browserify', 'css', cb);
});

gulp.task('dev', function(cb) {
  opts.devMode = true;

  let tasks = _.compact([
    'build',
    process.env.ASHES_NO_TEST_FOR_DEV ? null : 'test',
    'server',
    'watch',
    process.env.ASHES_NOTIFY_ABOUT_TASKS ? 'notifier' : null
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
