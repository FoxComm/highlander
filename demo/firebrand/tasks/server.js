'use strict';

/* eslint camelcase: 0 */

const _ = require('lodash');
const child_process = require('child_process');
const runSequence = require('run-sequence');

const affectsServerTasks = {};

function affectsServer(task) {
  affectsServerTasks[task] = 1;
}

module.exports = function(gulp) {
  let node = null;

  function killServer(cb) {
    if (node) {
      node.once('close', () => cb());
      node.kill();
      node = null;
    } else {
      cb();
    }
  }

  let affectTasksRunning = 0;

  function checkForPause(e) {
    if (e.task in affectsServerTasks) {
      affectTasksRunning++;
      killServer(_.noop);
    }
  }

  function checkForResume(e) {
    if (e.task in affectsServerTasks) {
      affectTasksRunning--;

      if (affectTasksRunning <= 0) {
        affectTasksRunning = 0;
        process.nextTick(function() {
          runSequence('server.invalidate');
        });
      }
    }
  }

  gulp.task('server.stop', killServer);

  gulp.task('server.invalidate', function(cb) {
    if (node) {
      runSequence('server.stop', 'server.start', cb);
    } else {
      runSequence('server.start', cb);
    }
  });

  gulp.task('server.start', function() {
    if (node) {
      console.warn('Server already started');
    } else {
      node = child_process.fork('server/boot.js');
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

    gulp.watch(['server/**.*.js', 'src/server.jsx'], ['server.restart']);
  });

  function silentlyKill() {
    if (node) node.kill();
  }

  process.on('exit', silentlyKill);
  process.on('uncaughtException', silentlyKill);
};

module.exports.affectsServer = affectsServer;
