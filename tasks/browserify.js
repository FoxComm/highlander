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

function setApiURL() {
  process.env.API_URL = process.env.API_URL || '/api';
}

function setDemoAuthToken() {
  /*  The demo site is protected by basic auth. All requests from javascript
   *  require basic auth headers. This will create the basic auth base64 encoded
   *  header and set it on the client side via the process.env.DEMO_AUTH_TOKEN
   *  variable. This is replaced in-line by envify with the correct value.
   */
  const demoAuthToken = process.env.DEMO_USER && process.env.DEMO_PASS ?
    new Buffer(`${process.env.DEMO_USER}:${process.env.DEMO_PASS}`).toString('base64')
    : undefined;

  process.env.DEMO_AUTH_TOKEN = demoAuthToken;
}

function setContext() {
  const language = process.env.FIREBIRD_LANGUAGE || 'en';
  process.env.FIREBIRD_CONTEXT = process.env.FIREBIRD_CONTEXT || (language == 'en' ? 'default' : language);

  console.log(`Language is: ${process.env.FIREBIRD_LANGUAGE}`);
  console.log(`Context is: ${process.env.FIREBIRD_CONTEXT}`);
}

module.exports = function(gulp, $, opts) {
  const production = (process.env.NODE_ENV === 'production');

  let bundler = null;

  // configure default env variables before browserify
  require('../server/env_defaults');

  // please use this options for every transform since we have nested node_modules in project
  const pleaseDontIgnoreNestedNodeModules = {
    global: true,
    ignore: new RegExp(path.join(process.cwd(), 'node_modules')),
  };

  function getBundler() {
    if (bundler) return bundler;

    bundler = browserify(Object.assign({
      entries: ['src/client.jsx'],
      transform: [
        ['babelify', pleaseDontIgnoreNestedNodeModules],
      ],
      standalone: 'App',
      extensions: ['.jsx'],
      debug: !production,
    }, watchify.args)).transform(envify({
      _: 'purge',
      NODE_ENV: process.env.NODE_ENV || 'development',
      DEMO_AUTH_TOKEN: process.env.DEMO_AUTH_TOKEN,
      FIREBIRD_LANGUAGE: process.env.FIREBIRD_LANGUAGE,
      FIREBIRD_CONTEXT: process.env.FIREBIRD_CONTEXT,
      API_URL: process.env.API_URL,
    }), pleaseDontIgnoreNestedNodeModules);

    bundler.plugin(require('css-modulesify'), Object.assign({
      output: path.resolve('build/bundle.css'),
      use: plugins,
      jsonOutput: 'build/css-modules.json',
    }, pleaseDontIgnoreNestedNodeModules));

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

  setApiURL();
  setDemoAuthToken();
  setContext();

  gulp.task('browserify.purge_cache', function() {
    const cache = watchify.args.cache;

    Object.keys(cache).map(key => {
      delete cache[key];
    });
  });

  gulp.task('browserify', function() {
    const stream = getBundler()
      .bundle()
      .on('error', function(err) {
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

  gulp.task('browserify.watch', function() {
    getBundler().on('update', function() {
      runSequence('browserify');
    });
  });
};
