
const fs = require('fs');
const path = require('path');

// by default enable for all platforms, except mac
exports.enableNotifier = process.platform != 'darwin';

if (fs.existsSync('./.gulpconfig')) {
  Object.assign(exports, require('../.gulpconfig'));
}
