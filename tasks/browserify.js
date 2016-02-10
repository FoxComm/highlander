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
const envify = require('envify/custom');

function setDemoAuthToken() {
  /*  The demo site is protected by basic auth. All requests from javascript
   *  require basic auth headers. This will create the basic auth base64 encoded
   *  header and set it on the client side via the process.env.DEMO_AUTH_TOKEN
   *  variable. This is replaced in-line by envify with the correct value.
   */
  var demoAuthToken = process.env.DEMO_USER && process.env.DEMO_PASS ? 
    new Buffer(process.env.DEMO_USER+":"+process.env.DEMO_PASS).toString('base64')
    : undefined;

  process.env.DEMO_AUTH_TOKEN = demoAuthToken;
}

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  let bundler = null;

  function getBundler() {
    if (bundler) return bundler;

    let entries = path.join(opts.srcDir, 'app.js');
    bundler = browserify({
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

  setDemoAuthToken();

  gulp.task('browserify', function() {
    const stream = getBundler()
      .transform(envify({
            DEMO_AUTH_TOKEN: process.env.DEMO_AUTH_TOKEN
      }))
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
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundler().on('update', function() {
      runSequence('browserify');
    });
  })
};
