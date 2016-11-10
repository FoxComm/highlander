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

const excludeList = [
  './components/style-guide/style-guide',
  './components/style-guide/style-guide-grid',
  './components/style-guide/style-guide-buttons',
  './components/style-guide/style-guide-containers',
  './components/activity-trail/all',
  './components/activity-notifications/all',
];

module.exports = function(gulp, opts, $) {
  let production = (process.env.NODE_ENV === 'production');

  const plugins = require('../src/postcss').plugins;
  let bundler = null;

  function getBundler() {
    if (bundler) return bundler;

    let entries = path.join(opts.srcDir, 'client.js');
    bundler = browserify({
      entries: [entries],
      standalone: 'App',
      transform: [
        'babelify',
      ],
      extensions: ['.jsx'],
      debug: true,
      cache: {},
      packageCache: {}
    }).transform(envify({
      NODE_ENV: process.env.NODE_ENV,
      DEMO_AUTH_TOKEN: process.env.DEMO_AUTH_TOKEN,
      API_URL: process.env.API_URL,
      ON_SERVER: process.env.ON_SERVER,
    }));

    if (production) {
      excludeList.map(file => bundler.exclude(file));
    }

    bundler.plugin(require('css-modulesify'), {
      output: path.resolve('build/css_bundle.css'),
      use: plugins,
      jsonOutput: 'build/css-modules.json',
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
      .bundle()
      .on('error', function(err) {
        stream.emit('error', err);
      })
      .pipe(source(`admin.js`))
      .pipe(buffer())
      .pipe($.if(production, $.sourcemaps.init({loadMaps: true})))
      .pipe($.if(production, $.uglify()))
      .pipe($.if(production, $.sourcemaps.write('_', {addComment: false})))
      .pipe(gulp.dest(opts.assetsDir));

    return stream;
  });
  affectsServer('browserify');

  gulp.task('browserify.watch', function() {
    getBundler().on('update', function() {
      runSequence('browserify');
    });
  });
};
