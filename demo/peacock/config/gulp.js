
const fs = require('fs');

exports.enableNotifier = true;

if (fs.existsSync('./.gulprc')) {
  Object.assign(exports, require('../.gulprc'));
}

if (process.argv.indexOf('--browser-sync') >= 0) {
  exports.enableBrowserSync = true;
}
