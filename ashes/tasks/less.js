'use strict';

const path = require('path');
const merge = require('merge-stream');
const PluginAutoPrefix = require('less-plugin-autoprefix');
const PluginNpmImport = require('less-plugin-npm-import');
const groupMediaQueries = require('less-plugin-group-css-media-queries');

const npmImport = new PluginNpmImport();
const autoPrefix = new PluginAutoPrefix({browsers: ["last 2 versions"]});

module.exports = function(gulp, opts, $) {
  gulp.task('less', function() {
    return gulp.src(path.join(opts.srcDir, 'less/base.less'))
      .pipe($.if(opts.devMode, $.plumber(function (err) {
        console.error(err);
        this.emit('end');
      })))
      .pipe($.concat(`less_bundle.less`))
      .pipe($.less({
        plugins: [npmImport, groupMediaQueries, autoPrefix]
      }))
      .pipe(gulp.dest('build'));
  });

  gulp.task('less.watch', function() {
    gulp.watch(opts.lessSrc, ['less']);
  });
};
