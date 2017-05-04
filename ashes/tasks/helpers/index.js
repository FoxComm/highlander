const { spawn } = require('child_process');

// run yarn script from package.json
function runScript(name, cb = () => {}) {
  let child = spawn('yarn',
    ['run', name],
    {
      shell: true,
      detached: true, // Do we need it to be detached? TBD
      stdio: 'inherit',
    }
  ).on('close', code => {
    child = null;
    if (code != 0) {
      cb(new Error(`"yarn run ${name}" process exited with code ${code}`));
    } else {
      cb();
    }
  }).on('error', err => {
    child = null;
    cb(err);
  });

  process.on('exit', () => {
    if (child) process.kill(-child.pid);
  });

  return child;
}

module.exports.runScript = runScript;
