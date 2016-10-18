'use strict';

const _ = require('lodash');

module.exports = function (gulp, $, opts) {
  let watchTasks = _.reduce(gulp.tasks, function(acc, task, name) {
    if (name.indexOf('watch') != -1) {
      acc.push(name);
    }
    return acc;
  }, []);

  if (opts.enableBrowserSync) {
    watchTasks = watchTasks.concat('browserSync');
  }

  gulp.task('watch', watchTasks);
};
