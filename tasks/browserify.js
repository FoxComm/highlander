'use strict';

const fs = require('fs');
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

  let bundler = null;

  function getBundler() {
    if (bundler) return bundler;

    let entries = path.join(opts.srcDir, 'app.js');
    bundler = browserify({
      paths: [opts.srcDir],
      entries: [entries],
      standalone: 'App',
      transform: ['babelify'],
      extensions: ['.jsx'],
      debug: !production,
      cache: {},
      packageCache: {}
    });

    if (opts.devMode) {
      let watchifyOpts = {
        poll: parseInt(process.env.WATCHIFY_POLL_INTERVAL || 250)
      };

      if (fs.existsSync('.watchifyrc')) {
        watchifyOpts = JSON.parse(fs.readFileSync('.watchifyrc'));
      }
      bundler = watchify(bundler, watchifyOpts);
    }

    return bundler;
  }

  gulp.task('browserify', function() {
    return getBundler()
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
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundler().on('update', function() {
      runSequence('browserify');
    });
  })
};
