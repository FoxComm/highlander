
const fs = require('fs');

// by default enable for all platforms, except mac
exports.enableNotifier = process.platform != 'darwin';

// install pre-push hooks
exports.autoInstallHooks = true;

if (fs.existsSync('./.gulprc')) {
  Object.assign(exports, require('../.gulprc'));
}
