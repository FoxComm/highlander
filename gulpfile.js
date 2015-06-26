'use strict';

const
  gulp      = require('gulp'),
  $         = require('gulp-load-plugins')(),
  fs        = require('fs'),
  path      = require('path');

const opts = {
  taskDir: path.resolve(__dirname, 'tasks'),
  themeDir: path.resolve('src', 'themes'),
  publicDir: path.resolve('public'),
  testDir: path.resolve(__dirname, 'test'),
  serverSrc: path.resolve(__dirname, 'server', '**/*.{js,json}'),
  jsSrc: path.resolve('src', 'themes', '**/*.{js,jsx}'),
  lessSrc: path.resolve('src', 'themes', '**/*.less'),
  imageSrc: path.resolve('src', 'themes', '**/*.{png,jpg,gif}'),
  getThemes: function(dir) {
    return fs.readdirSync(dir)
      .filter(function(file) {
        return fs.statSync(path.join(dir, file)).isDirectory();
    });
  }
};

for (let task of fs.readdirSync(opts.taskDir)) {
  let file = path.join(opts.taskDir, task);
  require(file)(gulp, opts, $);
}

gulp.task('build', ['less', 'browserify', 'imagemin']);
gulp.task('dev', ['lint', 'build', 'server', 'mocha', 'watch']);
gulp.task('default', ['build']);
