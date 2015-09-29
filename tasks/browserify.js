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

  function getBundler() {
    let entries = path.join(opts.srcDir, 'app.js');
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
        .pipe(source(`admin.js`))
        .pipe(buffer())
        //.pipe($.sourcemaps.init())
        .pipe($.if(production, $.uglify()))
        //.pipe($.sourcemaps.write('./maps'))
        .pipe(gulp.dest(opts.publicDir));

      return stream;
    };

    return {
      bundler: bundler,
      bundle: bundle
    }
  }

  gulp.task('browserify', function() {
    let stream = getBundler().bundle()
      .on('error', function(err) {
        stream.emit('error', err);
      });
    return stream;
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundler().bundler.on('update', function() {
      runSequence('browserify');
    });
  })
};
