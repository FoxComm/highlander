'use strict'

const fs = require('fs');
const send = require('koa-send');
const path = require('path');

function readable(filePath) {
  return new Promise(function(resolve, reject) {
    fs.access(filePath, fs.constants.R_OK, (err) => resolve(!err));
  });
}

module.exports = function serve(opts) {
  let options = opts;

  if (typeof opts === 'string') {
    options = {
      path: opts,
      publicPath: '/'
    };
  }

  return async (ctx, next) => {
    const url = ctx.path;

    if (url.startsWith(options.publicPath)) {
      const slug = path.normalize(url.replace(options.publicPath, '/'));
      const filePath = path.resolve(`${options.path}${slug}`);
      const exists = await readable( filePath );

      if (exists) {
        const sendOptions = {
          root: path.resolve(options.publicPath || process.cwd()),
          index: options.index || 'index.html'
        };
        const sent = await send(ctx, filePath, sendOptions);

        if (sent) {
          return;
        }
      }
    }

    // Requested url not starts with publicPath → pass to next middleware
    // File not exists or not readable → pass to next middleware
    return next();
  }
}
