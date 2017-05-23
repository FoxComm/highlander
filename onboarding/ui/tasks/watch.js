'use strict';

const _ = require('lodash');

module.exports = function(gulp) {
  const watchTasks = _.reduce(gulp.tasks, function(acc, task, name) {
    if (name.indexOf('watch') != -1) {
      acc.push(name);
    }
    return acc;
  }, []);

  gulp.task('watch', watchTasks);
};
