
const fs = require('fs');

// by default enable for all platforms, except mac
exports.enableNotifier = true; // process.platform != 'darwin';

// install pre-push hooks
exports.autoInstallHooks = true;

if (fs.existsSync('./.gulprc')) {
  Object.assign(exports, require('../.gulprc'));
}

if (process.argv.indexOf('--browser-sync') >= 0) {
  exports.enableBrowserSync = true;
}
