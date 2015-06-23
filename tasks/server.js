'use strict';

const
  spawn = require('child_process').spawn;

module.exports = function(gulp, opts, $) {
  let node = null;

  gulp.task('server', function() {
    if (node) node.kill();
    node = spawn('iojs', ['--harmony', 'boot.js'], {stdio: 'inherit'});
  });

  process.on('exit', function() {
    if (node) node.kill();
  });
}
