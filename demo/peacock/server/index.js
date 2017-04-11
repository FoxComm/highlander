
const childProcess = require('child_process');
const path = require('path');

const storefrontPath = path.resolve(path.join(__dirname, '..'));

class Storefront {
  build() {
    const gulpBin = path.join(storefrontPath, 'node_modules/.bin/gulp');
    return new Promise((resolve, reject) => {
      const child = childProcess.fork(gulpBin, ['build'], {
        cwd: storefrontPath,
        env: Object.assign({}, process.env, {
          TARGET_CWD: process.cwd(),
        }),
      });

      child.once('exit', (code) => {
        if (code == 0) {
          resolve();
        } else {
          reject(code);
        }
      });
    });
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
