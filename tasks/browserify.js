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

function getWatchifyOpts() {
  let watchifyOpts = {
    poll: parseInt(process.env.WATCHIFY_POLL_INTERVAL || 250)
  };

  if (fs.existsSync('.watchifyrc')) {
    watchifyOpts = JSON.parse(fs.readFileSync('.watchifyrc'));
  }
  return watchifyOpts;
}

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  let bundlers = null;

  function getBundlers() {
    if (bundlers) return bundlers;

    const appEntries = [
      {name: 'app', file: 'app.js', out: 'admin.js'},
      {name: 'login', file: 'login.js', out: 'login.js'},
    ];

    // map them to our stream function
    bundlers = appEntries.map(function(entry) {
      const entries  = path.join(opts.srcDir, entry.file);

      let bundler = browserify({
        entries: [entries],
        standalone: 'App',
        transform: ['babelify'],
        extensions: ['.jsx'],
        debug: true,
        cache: {},
        packageCache: {}
      }).transform(envify({
        DEMO_AUTH_TOKEN: process.env.DEMO_AUTH_TOKEN
      }));

      if (opts.devMode) {
        const watchifyOpts = getWatchifyOpts();
        bundler = watchify(bundler, watchifyOpts);
      }

      let bundle = function() {
        return bundler.bundle()
          .pipe(source(entry.out))
          .on('error', function(err) {
            stream.emit('error', err);
          })
          .pipe(buffer())
          .pipe($.if(production, $.sourcemaps.init({loadMaps: true})))
          .pipe($.if(production, $.uglify()))
          .pipe($.if(production, $.sourcemaps.write('_', {addComment: false})))
          .pipe(gulp.dest(opts.publicDir));
      };

      const taskName = `browserify.${entry.name}`;
      gulp.task(taskName, function() {
        return bundle();
      });

      affectsServer(taskName);

      return {
        name: entry.name,
        bundler: bundler,
        bundle: bundle,
      };

    });

    return bundlers;
  }

  setDemoAuthToken();

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
        runSequence(`browserify.${entry.name}`);
      });
    });
  });
};
