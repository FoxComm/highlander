const gulp = require('gulp');
const runSequence = require('run-sequence');
const chalk = require('chalk');

function listen(cb) {
  process.stdin.setEncoding('utf8');
  process.stdin.setRawMode(true);

  console.info(`Press ${chalk.white.bold('j')} to clean caches and re-run ${chalk.green('browserify')} task.`);
  console.info(`Press ${chalk.white.bold('c')} to re-run ${chalk.green('css')} task.`);
  console.info(`Press ${chalk.white.bold('s')} to re-run ${chalk.green('server')}.`);

  process.stdin.on('data', function(chunk) {
    switch (chunk) {
      case 'i':
        runSequence('images');
        break;
      case 'j':
        runSequence('browserify.purge_cache', 'browserify');
        break;
      case 'c':
        runSequence('css');
        break;
      case 's':
        runSequence('server.invalidate');
        break;
      case '\r':
        process.stdout.write('\n');
        break;
      case '\u0003':
        process.stdout.write('^C\n');
        process.exit();
        break;
      case 'q':
        process.exit();
        break;
      default:
        process.stdout.write(chunk);

    }
  });

  cb();
}

gulp.task('interactivity', listen);
