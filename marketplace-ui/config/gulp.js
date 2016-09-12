
const fs = require('fs');

// install pre-push hooks
exports.autoInstallHooks = true;

if (fs.existsSync('./.gulprc')) {
  Object.assign(exports, require('../.gulprc'));
}
