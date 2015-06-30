'use strict';

const
  gulp    = require('gulp'),
  $       = require('gulp-load-plugins')(),
  fs      = require('fs'),
  path    = require('path'),
  Config  = require('./config');

const opts = new Config().gulp;

for (let task of fs.readdirSync(opts.taskDir)) {
  let file = path.join(opts.taskDir, task);
  require(file)(gulp, opts, $);
}

gulp.task('build', ['less', 'browserify', 'imagemin']);
gulp.task('test', ['lint', 'mocha']);
gulp.task('dev', ['build', 'test', 'server', 'watch']);
gulp.task('default', ['build']);
