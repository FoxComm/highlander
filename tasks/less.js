'use strict';

const path = require('path');
const merge = require('merge-stream');
const PluginCleanCSS = require('less-plugin-clean-css');
const PluginAutoPrefix = require('less-plugin-autoprefix');
const PluginNpmImport = require('less-plugin-npm-import');
const PluginFunctions = require('less-plugin-functions');
const groupMediaQueries = require('less-plugin-group-css-media-queries');

const npmImport = new PluginNpmImport();
const cleanCSS = new PluginCleanCSS({advanced: true});
const autoPrefix = new PluginAutoPrefix({browsers: ["last 2 versions"]});
const functions = new PluginFunctions();

module.exports = function(gulp, opts, $) {
  gulp.task('less', function() {
    let src = [
      path.join(opts.srcDir, 'less/[^_]*.less'),
      path.join(opts.srcDir, 'components/**/*.less')
    ];
    return gulp.src(src)
      .pipe($.if(opts.devMode, $.plumber(function (err) {
        console.error(err);
        this.emit('end');
      })))
      .pipe($.concat(`admin.less`))
      .pipe($.less({
        plugins: [functions, npmImport, groupMediaQueries, autoPrefix, cleanCSS]
      }))
      .pipe(gulp.dest(opts.publicDir));
  });

  gulp.task('less.watch', function() {
    gulp.watch(opts.lessSrc, ['less']);
  });
};
