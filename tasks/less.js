'use strict';

const path = require('path');
const merge = require('merge-stream');
const PluginCleanCSS = require("less-plugin-clean-css");
const PluginAutoPrefix = require('less-plugin-autoprefix');
const PluginNpmImport = require('less-plugin-npm-import');
const groupMediaQueries = require('less-plugin-group-css-media-queries');

const
  npmImport   = new PluginNpmImport(),
  cleanCSS    = new PluginCleanCSS({advanced: true}),
  autoPrefix  = new PluginAutoPrefix({browsers: ["last 2 versions"]});

module.exports = function(gulp, opts, $) {
  gulp.task('less', function() {
    let themes = opts.getThemes(opts.themeDir);
    let tasks = themes.map(function(theme) {
      let src = path.join(opts.themeDir, theme, '**/*.less');
      return gulp.src(src)
        .pipe($.if(opts.devMode, $.plumber(function (err) {
          console.log(err);
          this.emit('end');
        })))
        .pipe($.concat(`${theme}.less`))
        .pipe($.less({
          plugins: [npmImport, groupMediaQueries, autoPrefix, cleanCSS]
        }))
        .pipe(gulp.dest(path.join(opts.publicDir, 'themes', theme)));
    });

    return merge(tasks);
  });

  gulp.task('less.watch', function() {
    gulp.watch(opts.lessSrc, ['less']);
  });
};
