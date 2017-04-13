
const childProcess = require('child_process');
const path = require('path');

const storefrontPath = path.resolve(path.join(__dirname, '..'));

class Storefront {
  runGulp(tasks) {
    const gulpBin = path.join(storefrontPath, 'node_modules/.bin/gulp');
    return new Promise((resolve, reject) => {
      let child = childProcess.fork(gulpBin, tasks, {
        cwd: storefrontPath,
        env: Object.assign({}, process.env, {
          TARGET_CWD: process.cwd(),
        }),
      });

      child.once('exit', (code) => {
        child = null;
        if (code == 0) {
          resolve();
        } else {
          reject(code);
        }
      });

      const killChild = () => {
        if (child) {
          process.kill(-child.pid);
          child = null;
        }
      };

      process.on('SIGINT', killChild);
      process.on('exit', killChild);
    });
  }

  build() {
    return this.runGulp(['build']);
  }

  dev() {
    return this.runGulp(['dev']);
  }

  run() {
    require('./boot.js');
  }

  start() {
    return this.build().then(() => {
      this.run();
    }, err => {
      console.error('Oops, process exited with code', err);
    });
  }
}

module.exports = Storefront;
