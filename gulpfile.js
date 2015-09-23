'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const Config = require('./config');

const opts = new Config().gulp;

let exitStatus = 0;

for (let task of fs.readdirSync(opts.taskDir)) {
  let file = path.join(opts.taskDir, task);
  require(file)(gulp, opts, $);
}

gulp.task('build', ['less', 'browserify', 'imagemin']);

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


gulp.on('err', function() {
  exitStatus = 1;
  $.util.beep();
});

process.on('exit', function() {
  process.exit(exitStatus);
});
