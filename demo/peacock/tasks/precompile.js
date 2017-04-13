'use strict';

const { spawn } = require('child_process');
const watch = require('glob-watcher');
const path = require('path');
const fs = require('mz/fs');
const rimraf = require('rimraf');

function runScript(name, opts = {}, cb = () => {}) {
  let child = spawn('yarn',
    ['run', name],
    Object.assign({
      shell: true,
      detached: true,
      stdio: 'inherit',
    }, opts)
  ).on('close', (code) => {
    child = null;
    if (code != 0) {
      cb(new Error(`"yarn run ${name}" process exited with code ${code}`));
    } else {
      cb();
    }
  }).on('error', (err) => {
    child = null;
    cb(err);
  });

  process.on('exit', () => {
    if (child) process.kill(-child.pid);
  });

  return child;
}

const statics = ['src/**/*.css', 'src/**/*.json'];

module.exports = function (gulp) {
  const babel = require('gulp-babel');
  const changed = require('gulp-changed');
  const through = require('through2');
  const read = require('gulp-read');

  gulp.task('precompile.static', function () {
    return gulp.src(statics)
      .pipe(gulp.dest('lib'));
  });

  const projectPath = path.resolve(__dirname, '..');
  const srcPath = path.join(projectPath, 'src');

  const getRelativePath = (filepath, basepath = projectPath) => {
    const fullPath = path.resolve(filepath);
    return path.relative(basepath, fullPath);
  };

  const logSrcToLib = (filepath, base = srcPath, overridden = false) => {
    const relative = getRelativePath(filepath, base);
    let message = `src/${relative} -> lib/${relative}`;
    if (overridden) {
      message += ' (overridden)';
    }

    console.info(message);
  };

  const targetCwd = process.env.TARGET_CWD;

  const replaceByAlt = () => {
    return through.obj((file, enc, cb) => {
      let willBeAlt = Promise.resolve(false);
      let altPath;
      if (targetCwd) {
        const relativePath = getRelativePath(file.path);
        altPath = path.join(targetCwd, relativePath);
        willBeAlt = fs.lstat(altPath);
      }

      willBeAlt.then(stat => {
        file.path = altPath;
        file.base = path.join(targetCwd, 'src');
        file.cwd = targetCwd;
        file.stat = stat;
        file.isAlt = true;
        cb(null, file);
      }, () => {
        cb(null, file);
      });
    });
  };

  const libPath = file => {
    if (file.isAlt) {
      return path.join(targetCwd, 'lib');
    } else {
      return 'lib';
    }
  };

  gulp.task('precompile.clean_target', function() {
    if (targetCwd) {
      rimraf.sync(path.join(targetCwd, 'lib'));
    }
  });

  const srcToLibLogger = () => {
    return through.obj((file, enc, cb) => {
      logSrcToLib(file.path, file.base || srcPath, file.isAlt);
      cb(null, file);
    });
  };

  gulp.task('precompile.source', ['precompile.clean_target'], function () {
    return gulp.src('src/**/*.{jsx,js}', {read: false})
      .pipe(replaceByAlt())
      .pipe(changed(libPath, {extension: '.js'}))
      .pipe(srcToLibLogger())
      .pipe(read())
      .pipe(babel({
        extends: path.resolve('.babelrc'),
      }))
      .pipe(gulp.dest(libPath));
  });

  gulp.task('precompile', ['precompile.static', 'precompile.source']);

  const watchStaticts = cwd => {
    const handleChanged = (filepath) => {
      logSrcToLib(filepath, path.join(cwd, 'src'));
      gulp
        .src(filepath, { base: path.join(cwd, 'src'), cwd })
        .pipe(through.obj((file, enc, cb) => {
          file.isAlt = targetCwd == cwd;
          cb(null, file);
        }))
        .pipe(srcToLibLogger())
        .pipe(gulp.dest('./lib', { cwd }));
    };

    watch(statics, {cwd})
      .on('change', handleChanged)
      .on('add', handleChanged);
  };

  const watchJs = cwd => {
    const babelify = filepath => {
      gulp
        .src(filepath, { base: path.join(cwd, 'src'), cwd })
        .pipe(through.obj((file, enc, cb) => {
          file.isAlt = targetCwd == cwd;
          cb(null, file);
        }))
        .pipe(srcToLibLogger())
        .pipe(babel({
          extends: path.resolve('.babelrc'),
        }))
        .pipe(gulp.dest('lib', { cwd }));
    };

    watch(`${cwd}/src/**/*.{jsx,js}`, {cwd})
      .on('change', babelify)
      .on('add', babelify);
  };

  gulp.task('precompile.watch', function () {
    watchStaticts(process.cwd());
    if (targetCwd) {
      watchStaticts(targetCwd);
    }

    runScript('watch-precompile');
    if (targetCwd) {
      watchJs(targetCwd);
    }
  });
};
