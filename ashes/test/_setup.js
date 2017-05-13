import 'babel-polyfill';
import 'co-mocha';

import path from 'path';
import rewire from 'rewire';
import register from 'ignore-styles';

// works when running from gulp but does not when running mocha from cli/webstorm
// NODE_PATH should be set as env variable before running mocha binary, NODE_PATH=src mocha ...
process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./src')}`;

process.env.API_URL = 'http://api.foxcommerce'; // mock
process.env.STRIPE_PUBLISHABLE_KEY = 'key_mock'; // mock

register(['.css']);

const unexpected = require('unexpected');
global.unexpected = unexpected
  .use(require('./_unexpected_actions'));

global.TimeShift = require('./_timeshift');

global.expect = (function(expect) {
  return function(target) {
    if (arguments.length > 1) {
      return global.unexpected.apply(this, arguments);
    } else {
      return expect(target);
    }
  };
})(require('chai').expect);


global.later = function(func) {
  return function() {
    var args = arguments;
    process.nextTick(function() {
      func.apply(this, args);
    });
  };
};

global.localStorage = require('localStorage');

const modulesCache = {};

global.importSource = function(sourcePath, actionsToExport) {
  if (!actionsToExport) {
    throw new Error('If you want just request some source without rewiring use requireSource instead');
  }
  const finalPath = path.resolve(`./src/${sourcePath}`);
  if (!(finalPath in modulesCache)) {
    const importedModule = rewire(finalPath);

    actionsToExport.forEach(action => {
      importedModule[action] = importedModule.__get__(action);
    });

    modulesCache[finalPath] = importedModule;
  }

  return modulesCache[finalPath];
};

global.requireSource = function(sourcePath) {
  const finalPath = path.resolve(`./src/${sourcePath}`);
  return require(finalPath);
};
