'use strict';

const fs = require('fs');
const path = require('path');
const browserify = require('browserify');
const watchify = require('watchify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const runSequence = require('run-sequence');
const affectsServer = require('./server').affectsServer;
const envify = require('envify/custom');

const plugins = require('../src/postcss').plugins;

module.exports = function (gulp, $, opts) {
  const production = (process.env.NODE_ENV === 'production');

  let bundler = null;

  function getBundler() {
    if (bundler) return bundler;

    bundler = browserify(Object.assign({
      entries: ['src/client.jsx'],
      transform: [
        'babelify',
      ],
      standalone: 'App',
      extensions: ['.jsx'],
      debug: !production,
    }, watchify.args));

    bundler.plugin(require('css-modulesify'), Object.assign({
      output: path.resolve('build/bundle.css'),
      use: plugins,
      jsonOutput: 'build/css-modules.json',
    }));

    if (opts.devMode) {
      let watchifyOpts = {
        poll: parseInt(process.env.WATCHIFY_POLL_INTERVAL || 250, 10),
      };

      if (fs.existsSync('.watchifyrc')) {
        watchifyOpts = JSON.parse(fs.readFileSync('.watchifyrc'));
      }
      bundler = watchify(bundler, watchifyOpts);
    }

    return bundler;
  }

  gulp.task('browserify.purge_cache', function () {
    const cache = watchify.args.cache;

    Object.keys(cache).map(key => {
      delete cache[key];
    });
  });

  gulp.task('browserify', function () {
    const stream = getBundler()
      .bundle()
      .on('error', function (err) {
        stream.emit('error', err);
      })
      .pipe(source(`app.js`))
      .pipe(buffer())
      .pipe($.if(production, $.uglify({
        compress: {
          global_defs: {
            DEBUG: false,
          },
        },
      })))
      .pipe(gulp.dest('public'));

    return stream;
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function () {
    getBundler().on('update', function () {
      runSequence('browserify');
    });
  });
};
