'use strict';


const path = require('path');
const merge = require('merge-stream');
const browserify = require('browserify');
const watchify = require('watchify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const runSequence = require('run-sequence');
const affectsServer = require('./server').affectsServer;

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  let bundlers = null;

  function getBundlers() {
    if (bundlers) return bundlers;

    let themes = opts.getThemes(opts.themeDir);

    bundlers = themes.map(function(theme) {
      let entries = path.join(opts.themeDir, theme, 'app.js');
      let bundler = browserify({
        entries: [entries],
        standalone: 'App',
        transform: ['babelify'],
        extensions: ['.jsx'],
        debug: !production,
        cache: {},
        packageCache: {}
      });

      if (opts.devMode) {
        bundler = watchify(bundler);
      }

      let bundle = function() {
        let stream = bundler
          .bundle()
          .on('error', function(err) {
            stream.emit('error', err);
          })
          .pipe(source(`${theme}.js`))
          .pipe(buffer())
          //.pipe($.sourcemaps.init())
          .pipe($.if(production, $.uglify()))
          //.pipe($.sourcemaps.write('./maps'))
          .pipe(gulp.dest(path.join(opts.publicDir, 'themes', theme)));

        return stream;
      };

      const taskName = 'browserify.' + theme;
      gulp.task(taskName, function() {
        return bundle();
      });

      affectsServer(taskName);

      return {
        theme: theme,
        bundler: bundler,
        bundle: bundle
      }
    });

    return bundlers;
  }



  gulp.task('browserify', function() {
    let stream = merge(getBundlers().map(function(entry) {
        return entry.bundle()
          .on('error', function(err) {
            stream.emit('error', err);
          });
    }));

    return stream;
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundlers().map(function(entry) {
      entry.bundler.on('update', function() {
        runSequence('browserify.' + entry.theme);
      });
    });
  })
};
