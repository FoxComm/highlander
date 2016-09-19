const fs = require('fs');

// install pre-push hooks
exports.autoInstallHooks = true;

if (fs.existsSync('./.gulprc')) {
  // eslint-disable-next-line import/no-unresolved
  Object.assign(exports, require('../.gulprc'));
}
