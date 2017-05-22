'use strict';

const _ = require('lodash');
const path = require('path');
const stripColors = require('strip-ansi');
const notifier = require('node-notifier');

/* eslint no-param-reassign:0 */

function notify(opts, type) {
  if (process.platform === 'linux') {
    if (type == 'error') {
      opts.icon = path.join(__dirname, 'cancel-48.png');
    } else {
      opts.icon = path.join(__dirname, 'ok-48.png');
    }
    opts.time = opts.time || process.env.DEFAULT_NOTIFICATION_TIMEOUT || 2000;
  } else if (process.platform === 'darwin') {
    if (type === 'error') {
      opts.type = 'fail';
    } else {
      opts.type = 'info';
    }
  }

  notifier.notify(opts);
}

let completedTasks = [];
const notifyAboutCompletedTasks = _.debounce(function() {
  const message = `Task${completedTasks.length > 1 ? 's' : ''} ${completedTasks.join(', ')} successfully completed.`;
  notify({
    title: 'Gulp tasks completed',
    message,
  });
  completedTasks = [];
}, 125);


module.exports = function(gulp) {
  gulp.task('notifier', function() {
    gulp.on('task_stop', function(e) {
      completedTasks.push(e.task);
    });

    gulp.on('stop', function() {
      notifyAboutCompletedTasks();
    });

    gulp.on('err', function(e) {
      e = e.err || e;

      notify({
        title: 'Gulp emit error',
        message: stripColors(e && e.toString() || ''),
      });
    });
  });
};
