'use strict';

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

gulp.task('build', ['hooks', 'less', 'browserify', 'imagemin']);

gulp.task('test', function(cb) {
  runSequence('lint', 'mocha', cb);
});

gulp.task('dev', function(cb) {
  opts.devMode = true;
  runSequence('build', 'server', 'watch', cb);
});
gulp.task('default', ['build']);


gulp.on('err', function() {
  exitStatus = 1;
  $.util.beep();
});

process.on('exit', function() {
  process.exit(exitStatus);
});
