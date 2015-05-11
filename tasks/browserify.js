'use strict';

const
  path        = require('path'),
  merge       = require('merge-stream'),
  browserify  = require('browserify'),
  source      = require('vinyl-source-stream'),
  buffer      = require('vinyl-buffer');

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  gulp.task('browserify', function() {
    let themes = opts.getThemes(opts.themeDir);
    let tasks = themes.map(function(theme) {
      let entries = path.join(opts.themeDir, theme, 'app.js');
      let bundler = browserify({
        entries: [entries],
        standalone: 'App',
        transform: ['babelify', 'reactify'],
        extensions: ['.jsx'],
        debug: !production
      });

      let bundle = function() {
        return bundler
          .bundle()
          .pipe(source(`${theme}.js`))
          .pipe(buffer())
          //.pipe($.sourcemaps.init())
          .pipe($.if(production, $.uglify()))
          //.pipe($.sourcemaps.write('./maps'))
          .pipe(gulp.dest(path.join(opts.publicDir, 'themes', theme)));
      }

      return bundle();
    });

    return merge(tasks);
  });
}
