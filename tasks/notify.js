'use strict';

const _ = require('lodash');
const argv = require('yargs').argv;
const stripColors = require('strip-ansi');
const notifier = require('node-notifier');

function notify(opts, type) {
  if (process.platform === 'linux') {
    if (type == 'error') {
      opts.icon = 'emblem-important';
    } else {
      opts.icon = 'emblem-default';
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

var completedTasks = [];
var notifyTimer = null;
var notifyAboutCompletedTasks = _.debounce(function() {
  let message = `Task${completedTasks.length > 1 ? 's' : ''} ${completedTasks.join(', ')} successfully completed.`;
  notify({
    title: 'Gulp tasks completed',
    message
  });
  completedTasks = [];
}, 125);


module.exports = function(gulp, opts, $) {
  if (argv.q || argv.notify === false) return false;


  gulp.task('notifier', function() {
    gulp.on('task_stop', function(e) {
      completedTasks.push(e.task);
    });

    gulp.on('stop', function() {
      notifyAboutCompletedTasks();
    });
  });

  gulp.on('err', function(e) {
    e = e.err || e;

    notify({
      title: 'Gulp emit error',
      message: stripColors(e && e.toString() || '')
    })
  });

};
