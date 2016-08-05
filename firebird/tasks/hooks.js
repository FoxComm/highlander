'use strict';

const fs = require('fs');
const PRE_PUSH = '../.git/hooks/pre-push';

const hookScript = `#!/bin/sh
npm run lint && npm run flow
`;

function installHooks() {
}

module.exports = function(gulp, $, opts) {
  if (opts.autoInstallHooks) {
    installHooks();
  }
  gulp.task('hooks', installHooks);
};
