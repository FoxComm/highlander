/* eslint camelcase: 0 */

const child_process = require('child_process');

module.exports = function (gulp) {
  gulp.task('sitemap', ['precompile'], function (cb) {
    child_process.fork('create-sitemap').once('close', () => cb());
  });
};
