'use strict';


const child_process = require('child_process');
const runSequence = require('run-sequence');

let affectsServerTasks = {};

function affectsServer(task) {
  affectsServerTasks[task] = 1;
}

module.exports = function(gulp, opts, $) {
  let node = null;

  let affectTasksRunning = 0;

  function checkForPause(e) {
    if (e.task in affectsServerTasks) {
      affectTasksRunning++;
      process.nextTick(function() {
        runSequence('server.stop');
      });
    }
  }

  function checkForResume(e) {
    if (e.task in affectsServerTasks) {
      affectTasksRunning--;

      if (affectTasksRunning <= 0) {
        affectTasksRunning = 0;
        process.nextTick(function() {
          runSequence('server.start');
        });
      }
    }
  }

  gulp.task('server.stop', function(cb) {
    if (node) {
      node.once('close', () => cb());
      node.kill();
      node = null;
    } else {
      cb();
    }
  });

  gulp.task('server.start', function() {
    if (node) {
      console.warn('Server already started');
    } else {
      node = child_process.fork('boot.js');
    }
  });

  gulp.task('server', function(cb) {
    runSequence('server.stop', 'server.start', cb);
  });

  // like server but gracefully lookup for affectTasksRunning
  gulp.task('server.restart', function(cb) {
    if (affectTasksRunning) {
      return cb();
    }
    runSequence('server', cb);
  });

  gulp.task('server.watch', function() {
    gulp.on('task_start', checkForPause);
    gulp.on('task_err', checkForResume);
    gulp.on('task_stop', checkForResume);

    gulp.watch([opts.configSrc, opts.serverSrc], ['server.restart']);
  });

  process.on('exit', function() {
    if (node) node.kill();
  });
};

module.exports.affectsServer = affectsServer;
