'use strict';

const _ = require('lodash');

module.exports = function(gulp, opts, $) {
  let watchTasks = _.reduce(gulp.tasks, function(acc, task, name) {
    if (name.indexOf('watch') != -1) {
      if (name.indexOf('test') != -1 && process.env.USE_HOOKS == true) {
        return acc;
      }
      acc.push(name);
    }
    return acc;
  }, []);

  gulp.task('watch', watchTasks);
};
