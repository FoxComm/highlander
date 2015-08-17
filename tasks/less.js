'use strict';

const
  path              = require('path'),
  merge             = require('merge-stream'),
  plumber           = require('gulp-plumber'),
  PluginCleanCSS    = require("less-plugin-clean-css"),
  PluginAutoPrefix  = require('less-plugin-autoprefix'),
  PluginNpmImport   = require('less-plugin-npm-import'),
  groupMediaQueries = require('less-plugin-group-css-media-queries');

const
  npmImport   = new PluginNpmImport(),
  cleanCSS    = new PluginCleanCSS({advanced: true}),
  autoPrefix  = new PluginAutoPrefix({browsers: ["last 2 versions"]});

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  gulp.task('less', function() {
    let themes = opts.getThemes(opts.themeDir);
    let tasks = themes.map(function(theme) {
      let src = path.join(opts.themeDir, theme, '**/*.less');
      return gulp.src(src)
        .pipe($.if(!production, plumber()))
        .pipe($.concat(`${theme}.less`))
        .pipe($.less({
          plugins: [npmImport, groupMediaQueries, autoPrefix, cleanCSS]
        }))
        .pipe(gulp.dest(path.join(opts.publicDir, 'themes', theme)));
    });

    return merge(tasks);
  });
}
