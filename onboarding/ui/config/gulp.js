const fs = require('fs');

if (fs.existsSync('./.gulprc')) {
  // eslint-disable-next-line import/no-unresolved
  Object.assign(exports, require('../.gulprc'));
}
