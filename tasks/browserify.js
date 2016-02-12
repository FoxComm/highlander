'use strict';

const fs = require('fs');
const path = require('path');
const browserify = require('browserify');
const watchify = require('watchify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const runSequence = require('run-sequence');
const affectsServer = require('./server').affectsServer;
const modulesify = require('css-modulesify');

module.exports = function(gulp, opts, $) {
  const production = (process.env.NODE_ENV === 'production');

  let bundler = null;

  function getBundler() {
    if (bundler) return bundler;

    bundler = browserify({
      entries: ['src/client.jsx'],
      transform: ['babelify'],
      standalone: 'App',
      extensions: ['.jsx'],
      debug: !production,
      cache: {},
      packageCache: {}
    });
    bundler.plugin(require('css-modulesify'), {
      output: path.resolve('public/app.css'),
      after: ['postcss-cssnext']
    });
    if (!production) {
      bundler.plugin('livereactload');
    }

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
    const stream = getBundler()
      .bundle()
      .on('error', function(err) {
        stream.emit('error', err);
      })
      .pipe(source(`app.js`))
      .pipe(buffer())
      .pipe($.if(production, $.uglify()))
      .pipe(gulp.dest('public'));

    return stream;
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundler().on('update', function() {
      runSequence('browserify');
    });
  })
};
