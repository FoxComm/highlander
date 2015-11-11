const fs = require('fs');

for (let file of fs.readdirSync(__dirname)) {
  if (file === 'index.js' || !/\.js$/.test(file)) continue;
  let err = require(`${__dirname}/${file}`);
  exports[err.name] = err;
}
