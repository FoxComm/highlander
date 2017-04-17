const path = require('path');
const _ = require('lodash');

const storefrontRoot = path.normalize(path.resolve(
  path.join(__dirname, '..')
));

const libDir = path.join(storefrontRoot, 'lib');
let additionalPaths = [libDir];
const targetPath = process.env.TARGET_CWD || process.cwd();
if (targetPath != storefrontRoot) {
  additionalPaths = [path.join(targetPath, 'lib'), ...additionalPaths];
}
process.env.NODE_PATH = _.compact([process.env.NODE_PATH, ...additionalPaths]).join(':');
