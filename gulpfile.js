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

gulp.task('build', ['less', 'browserify', 'imagemin']);

gulp.task('dev', ['build', 'test', 'server', 'watch']);

gulp.task('default', ['build']);


gulp.on('err', function() {
  exitStatus = 1;
  $.util.beep();
});

process.on('exit', function() {
  process.exit(exitStatus);
});
