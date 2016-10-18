
const fs = require('fs');

exports.enableNotifier = process.argv.indexOf('--notifications') >= 0;

// install pre-push hooks
exports.autoInstallHooks = true;

if (fs.existsSync('./.gulprc')) {
  Object.assign(exports, require('../.gulprc'));
}

if (process.argv.indexOf('--browser-sync') >= 0) {
  exports.enableBrowserSync = true;
}
